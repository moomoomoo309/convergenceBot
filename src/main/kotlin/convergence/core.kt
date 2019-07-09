@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import humanize.Humanize
import kotlinx.serialization.Serializable
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import org.ocpsoft.prettytime.units.JustNow
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.arrayListOf
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.emptyMap
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.sortedBy
import kotlin.collections.toList
import kotlin.ranges.contains

class CommandDoesNotExist(cmd: String) : Exception(cmd)

val log: (Any) -> Unit = { println(it) }
val logErr: (Any) -> Unit = { System.err.println(it) }

val protocols = mutableSetOf<Protocol>()
val baseInterfaceMap = mutableMapOf<Protocol, BaseInterface>()
val chatMap = mutableMapOf<Int, Chat>()
val reverseChatMap = mutableMapOf<Chat, Int>()
var currentChatID: Int = 0
/**
 * Adds a protocol to the protocol registry.
 * @return true if a protocol with that name does not already exist in the registry, false otherwise.
 */
fun registerProtocol(protocol: Protocol, baseInterface: BaseInterface): Boolean {
    if (protocol in protocols)
        return false
    protocols += protocol
    baseInterfaceMap[protocol] = baseInterface
    return true
}

val sortedHelpText = arrayListOf<CommandLike>()
val commands = mutableMapOf<Chat, MutableMap<String, Command>>()
/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(chat: Chat, command: Command): Boolean {
    if (chat !in commands || commands[chat] !is MutableMap)
        commands[chat] = mutableMapOf(command.name to command)

    if (command.name in commands[chat]!!)
        return false

    if (sortedHelpText.isNotEmpty())
        for (i in 0..sortedHelpText.size)
            if (command.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, command)
                break
            } else
                sortedHelpText.add(command)

    commands[chat]!![command.name] = command
    return true
}

val aliases = mutableMapOf<Chat, MutableMap<String, Alias>>()
/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */
fun registerAlias(chat: Chat, alias: Alias): Boolean {
    if (chat !in aliases || aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf(alias.name to alias)

    if (alias.name in aliases[chat]!!)
        return false

    if (sortedHelpText.isNotEmpty())
        for (i in 0..sortedHelpText.size)
            if (alias.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, alias)
                break
            } else
                sortedHelpText[0] = alias

    aliases[chat]!![alias.name] = alias
    return true
}

const val defaultCommandDelimiter = "!"

class DefaultMap<K, V>(val defaultValue: V) : HashMap<K, V>() {
    override fun get(key: K): V = super.get(key) ?: defaultValue
}

val commandDelimiters = DefaultMap<Chat, String>(defaultCommandDelimiter)

/**
 * Sets the Command delimiter used for the bot's commands. (is it !help, |help, @help, or something else?)
 */
fun setCommandDelimiter(chat: Chat, commandDelimiter: String): Boolean {
    if (commandDelimiter.any { it.isWhitespace() || it == '"' })
        return false
    commandDelimiters[chat] = commandDelimiter
    return true
}

/**
 * Sends [message] in the chat [sender] is in, forwarding the message to any linked chats.
 */
fun sendMessage(sender: User, message: String?) {
    if (sender != baseInterfaceMap[sender.chat.protocol]!!.getBot(sender.chat))
        sendMessage(sender.chat, message)
    forwardToLinkedChats(message, sender)
}

/**
 * Sends [message] in [chat], not forwarding it to linked chats.
 */
fun sendMessage(chat: Chat, message: String?) {
    if (message == null)
        return
    if (chat.protocol !in protocols) {
        logErr("Protocol \"${chat.protocol.name}\" not properly registered!")
        return
    }
    val baseInterface = baseInterfaceMap[chat.protocol]!!
    baseInterface.sendMessage(chat, message)
}

/**
 * Gets the nickname (if applicable) or name of a user.
 */
fun getUserName(sender: User): String {
    val chat = sender.chat
    val baseInterface = baseInterfaceMap[chat.protocol]!!
    return if (baseInterface is INickname)
        baseInterface.getUserNickname(chat, sender) ?: baseInterface.getName(chat, sender)
    else
        baseInterface.getName(chat, sender)
}

val aliasVars = mutableMapOf(
        "sender" to { b: BaseInterface, c: Chat, s: User -> b.getName(c, s) },
        "botname" to { b: BaseInterface, c: Chat, _: User -> b.getName(c, b.getBot(c)) },
        "chatname" to { b: BaseInterface, c: Chat, _: User -> b.getChatName(c) })

/**
 * Replaces instances of the keys in [aliasVars] preceded by a percent sign with the result of the functions therein,
 * such as %sender with the name of the user who sent the message.
 */
fun replaceAliasVars(message: String?, sender: User): String? {
    if (message == null)
        return null
    val chat = sender.chat
    // Used as a mutable string, since this function does a lot of string appending.
    val stringBuilder = StringBuilder((message.length * 1.5).toInt())

    var charIndex = -1
    // Bitsets are faster than boolean arrays.
    val bitSet = BitSet(aliasVars.size)
    bitSet.set(0, aliasVars.size)

    var anyTrue = false
    for (currentChar in message) {
        stringBuilder.append(currentChar)
        if (currentChar == '%') {
            charIndex = 0
            continue
        }
        if (charIndex >= 0) {
            var i = -1
            for ((string, aliasVar) in aliasVars) {
                i++
                if (bitSet[i] && currentChar != string[charIndex])
                    bitSet[i] = false
                if (bitSet[i]) {
                    anyTrue = true
                    if (charIndex == string.length - 1) {
                        val baseInterface = baseInterfaceMap[chat.protocol]!!
                        stringBuilder.setLength(stringBuilder.length - string.length - 1)
                        stringBuilder.append(aliasVar(baseInterface, chat, sender))
                        charIndex = -1
                        break
                    }
                }
            }
            if (!anyTrue)
                break
        }
        charIndex = if (anyTrue) charIndex + 1 else -1
    }
    return stringBuilder.toString()
}

fun getCommandData(message: String, sender: User): CommandData? = try {
    parseCommand(message, sender.chat)
} catch (e: CommandDoesNotExist) {
    sendMessage(sender, "No command exists with name \"${e.message}\".")
    null
} catch (e: InvalidEscapeSequence) {
    sendMessage(sender, "Invalid escape sequence \"${e.message}\" passed. Are your backslashes correct?")
    null
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(message: String, sender: User) {
    log("[${getUserName(sender)}]: $message")
    getCommandData(message, sender)?.let { runCommand(sender, it) }
}

fun runCommand(sender: User, command: CommandData) = sendMessage(sender, replaceAliasVars(command(sender), sender))

val linkedChats = HashMap<Chat, MutableSet<Chat>>()
fun forwardToLinkedChats(message: String?, sender: User) {
    if (message == null)
        return
    val chat = sender.chat
    val protocol = chat.protocol
    var boldOpen = ""
    var boldClose = ""
    val baseInterface = baseInterfaceMap[protocol]
    // Try to get the delimiters for bold, if possible.
    if (baseInterface is IFormatting && Format.bold in baseInterface.supportedFormats) {
        val delimiters = baseInterface.getDelimiters(Format.bold)
        boldOpen = delimiters?.first ?: boldOpen
        boldClose = delimiters?.second ?: boldClose
    }

    // Send the messages out to the linked chats, if there are any. Don't error if there aren't any.
    if (chat in linkedChats)
        for (linkedChat in linkedChats[chat]!!)
            sendMessage(linkedChat, "$boldOpen${getUserName(sender)}:$boldClose $message")
}

fun offsetDateTimeToDate(time: OffsetDateTime): Date = Date.from(time.toInstant())
fun formatTime(time: OffsetDateTime): String = Humanize.naturalTime(offsetDateTimeToDate(time))

const val allowedTimeDifference = 30
const val updatesPerSecond = 1

@Serializable
data class ScheduledCommand(@Serializable(OffsetDateTimeSerializer::class) val time: OffsetDateTime,
                            val sender: User, val commandData: CommandData, val id: Int)

object SchedulerThread : Thread() {
    private val scheduledCommands = TreeMap<OffsetDateTime, ArrayList<ScheduledCommand>>()
    private val commandsList = TreeMap<Int, ScheduledCommand>()
    private var currentId: Int = 0
    fun loadFromFile(filePath: String) {

    }

    override fun run() {
        while (this.isAlive) {
            val now = OffsetDateTime.now()
            for ((cmdTime, cmdList) in scheduledCommands) {
                if (cmdTime.isBefore(now)) {
                    if (cmdTime.until(now, ChronoUnit.SECONDS) in 0..allowedTimeDifference)
                        for (cmd in cmdList) {
                            runCommand(cmd.sender, cmd.commandData)
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
                    else
                        for (cmd in cmdList) {
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
                } else //TreeMaps are ordered; it's already sorted chronologically, so all following events are early.
                    break
            }
            sleep((1000.0 / updatesPerSecond).toLong())
        }
    }

    fun schedule(sender: User, command: CommandData, time: OffsetDateTime): String? {
        if (time !in scheduledCommands)
            scheduledCommands[time] = ArrayList(2)
        val cmd = ScheduledCommand(time, sender, command, currentId++)
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            logErr("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        return "Scheduled ${getUserName(sender)} to run \"$command\" to run at $time."
    }

    fun getCommands(sender: User?): List<ScheduledCommand> = if (sender == null) getCommands() else commandsList.values.filter { it.sender == sender }
    fun getCommands(): List<ScheduledCommand> = commandsList.values.toList()
    fun getCommandStrings(sender: User, getAllCommands: Boolean = false) = getCommands(if (getAllCommands) null else sender)
            .sortedBy { it.time }
            .map { "[${it.id}] ${formatTime(it.time)}: ${getUserName(it.sender)} - \"${commandDelimiters[sender.chat]}${it.commandData.command.name} ${it.commandData.args.joinToString(" ")}\"" }

    fun unschedule(sender: User, index: Int) = commandsList.remove(index)?.let { scheduledCommands[it.time]?.remove(it) }
            ?: false
}

/**
 * Schedules [command] to run at [time] from [sender].
 */
fun schedule(sender: User, command: CommandData, time: OffsetDateTime) = SchedulerThread.schedule(sender, command, time)

var commandLineArgs: Namespace = Namespace(emptyMap())

class core {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val argParser = ArgumentParsers.newFor("Convergence Bot").build()
                    .defaultHelp(true)
                    .description("Sets the paths used by the bot.")

            val paths = argParser.addArgumentGroup("Paths")
            paths.addArgument("-pp", "--plugin-path")
                    .type(String::class.java).default = Paths.get(System.getProperty("user.home"), ".convergence", "plugins").toString()
            try {
                commandLineArgs = argParser.parseArgs(args)
            } catch (e: ArgumentParserException) {
                logErr("Failed to parse command line arguments. Printing stack trace:")
                e.printStackTrace()
                return
            }
            // Remove "just now" as an option for time formatting. 5 minutes for "just now" is annoying.
            Humanize.prettyTimeFormat().prettyTime.removeUnit(JustNow::class.java)

            registerCallbacks()

            log("Registering default commands...")
            registerDefaultCommands()

            log("Starting scheduler thread...")

            SchedulerThread.start()

            val plugins = PluginLoader.loadPlugin(Paths.get(commandLineArgs.get<String>("plugin_path")))
            if (plugins.isEmpty())
                log("No plugins loaded.")
            else
                log("Loaded Plugins:\t${plugins.joinToString("\t") { it.name }}")

            for (plugin in plugins) {
                log("Initializing plugin: ${plugin.name}")
                Thread(plugin::init).start()
            }
        }
    }
}
