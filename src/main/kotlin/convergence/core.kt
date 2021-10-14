@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import humanize.Humanize
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import org.ocpsoft.prettytime.units.JustNow
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginWrapper
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class CommandDoesNotExist(cmd: String): Exception(cmd)

// TODO: Replace this with an actual logging system
fun log(it: Any) = println(it)
fun logErr(it: Any) = System.err.println(it)
fun logErr(it: Throwable) = it.printStackTrace()

/**
 * Deserializes a JSON string into the type specified in [T].
 */
inline fun <reified T: Any> Moshi.fromJson(jsonStr: String): T =
        this.adapter(T::class.java)?.fromJson(jsonStr)
                ?: throw JsonDataException("Is there an adapter missing for ${T::class.simpleName}?")

fun <T> Moshi.fromJson(jsonStr: String, cls: Class<T>): T = this.adapter(cls)?.fromJson(jsonStr)
        ?: throw JsonDataException("Is there an adapter missing for ${cls.simpleName}?")

/**
 * Serializes a variable of type [T] into a JSON string. Will serialize null values if they are present.
 */
inline fun <reified T: Any> Moshi.toJson(obj: T, prettyPrint: Boolean = false): String =
        this.adapter(T::class.java)?.serializeNulls()?.indent(if (prettyPrint) "    " else "")?.toJson(obj)
                ?: throw JsonDataException("Is there an adapter missing for ${T::class.simpleName}?")

fun <T> Moshi.toJson(obj: T, cls: Class<T>, prettyPrint: Boolean = false): String =
        this.adapter(cls)?.serializeNulls()?.indent(if (prettyPrint) "    " else "")?.toJson(obj)
                ?: throw JsonDataException("Is there an adapter missing for ${cls.simpleName}?")

/**
 * Adds a list of protocol to the protocol registry.
 * @return a list of booleans, true if a protocol with that name does not already exist in the registry, false otherwise.
 */
fun registerProtocols(protocols: List<Protocol>, baseInterface: BaseInterface) = protocols.map {
    registerProtocol(it, baseInterface)
}

/**
 * Adds a protocol to the protocol registry.
 * @return true if a protocol with that name does not already exist in the registry, false otherwise.
 */
fun registerProtocol(protocol: Protocol, baseInterface: BaseInterface): Boolean {
    if (protocol in protocols)
        return false
    protocols.add(protocol)
    baseInterfaceMap[protocol] = baseInterface
    return true
}

/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(command: Command): Boolean {
    val chat = command.chat
    val commands = commands
    if (chat !in commands || commands[chat] !is MutableMap)
        commands[chat] = mutableMapOf(command.name to command)

    if (command.name in commands[chat]!!)
        return false

    if (sortedHelpText.isEmpty())
        sortedHelpText.add(command)
    else
        sortedHelpText.add(-sortedHelpText.binarySearch(command) - 1, command)
    commands[chat]!![command.name] = command
    return true
}

/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */
fun registerAlias(alias: Alias): Boolean {
    val chat = alias.chat
    val aliases = aliases
    if (chat !in aliases || aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf(alias.name to alias)

    if (alias.name in aliases[chat]!!)
        return false

    if (sortedHelpText.isNotEmpty())
        for ((i, commandLike) in sortedHelpText.withIndex())
            if (alias.name < commandLike.name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, alias)
                break
            } else
                sortedHelpText[0] = alias

    aliases[chat]!![alias.name] = alias
    return true
}

class DefaultMap<K, V>(var defaultValue: V): HashMap<K, V>() {
    override fun get(key: K): V = super.getOrDefault(key, defaultValue)
}

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
    forwardToLinkedChats(message, sender, true)
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
    return if (baseInterface is HasNicknames)
        baseInterface.getUserNickname(chat, sender) ?: baseInterface.getName(chat, sender)
    else
        baseInterface.getName(chat, sender)
}

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
    val possibleMatches = BooleanArray(aliasVars.size)
    possibleMatches.fill(false)

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
                if (possibleMatches[i] && currentChar != string[charIndex])
                    possibleMatches[i] = false
                if (possibleMatches[i]) {
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
    forwardToLinkedChats(message, sender)
    getCommandData(message, sender)?.let { runCommand(sender, it) }
}

fun getStackTraceText(e: Exception): String = ByteArrayOutputStream().let {
    e.printStackTrace(PrintStream(it))
    it.toString("UTF8")
}

fun runCommand(sender: User, command: CommandData) = try {
    sendMessage(sender, replaceAliasVars(command(sender), sender))
} catch (e: Exception) {
    sendMessage(sender, "Error while running command! Stack trace:\n${getStackTraceText(e)}")
}

fun forwardToLinkedChats(message: String?, sender: User, isCommand: Boolean = false) {
    if (message == null)
        return
    val chat = sender.chat
    val protocol = chat.protocol
    var boldOpen = ""
    var boldClose = ""
    val baseInterface = baseInterfaceMap[protocol]
    // Try to get the delimiters for bold, if possible.
    if (baseInterface is CanFormatMessages && Format.bold in baseInterface.supportedFormats) {
        val delimiters = baseInterface.getDelimiters(Format.bold)
        boldOpen = delimiters?.first ?: boldOpen
        boldClose = delimiters?.second ?: boldClose
    }

    // Send the messages out to the linked chats, if there are any. Don't error if there aren't any.
    val bot = baseInterfaceMap[sender.chat.protocol]!!.getBot(sender.chat)
    if (isCommand || sender != bot)
        if (chat in linkedChats)
            for (linkedChat in linkedChats[chat]!!)
                sendMessage(linkedChat, "$boldOpen${getUserName(if (isCommand) bot else sender)}:$boldClose $message")
}

fun offsetDateTimeToDate(time: OffsetDateTime): Date = Date.from(time.toInstant())
fun formatTime(time: OffsetDateTime): String = Humanize.naturalTime(offsetDateTimeToDate(time))

data class ScheduledCommand(
        val time: OffsetDateTime,
        val sender: User,
        val commandData: CommandData,
        val id: Int,
): JsonConvertible {
    override fun toJson() = mapOf(
            "type" to "ScheduledCommand",
            "sender" to sender.toJson(),
            "commandData" to commandData.toJson(),
            "id" to id
    ).json()
}

/**
 * Checks if commands scheduled for later are ready to be run yet. Can give information on the commands in its queue.
 */
object SchedulerThread: Thread() {
    private const val allowedTimeDifferenceSeconds = 30
    private const val updatesPerSecond = 1

    private val scheduledCommands = TreeMap<OffsetDateTime, MutableList<ScheduledCommand>>()
    private val commandsList = TreeMap<Int, ScheduledCommand>()
    private var currentId: Int = 0
    private val serializedCommands = hashMapOf<Int, String>()

    init {
        serializedCommands.values.forEach {
            val cmd = moshi.fromJson<ScheduledCommand>(it)
            commandsList[cmd.id] = cmd
            scheduledCommands.getOrPut(cmd.time) { mutableListOf(cmd) }
        }
    }

    override fun run() {
        while (this.isAlive) {
            val now = OffsetDateTime.now()
            for ((cmdTime, cmdList) in scheduledCommands) {
                if (cmdTime.isBefore(now)) {
                    if (cmdTime.until(now, ChronoUnit.SECONDS) in 0..allowedTimeDifferenceSeconds)
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

    /**
     * Schedules [command] sent by [sender] to run at [time].
     * @return The response the user will get from the command.
     */
    fun schedule(sender: User, command: CommandData, time: OffsetDateTime): String {
        if (time !in scheduledCommands)
            scheduledCommands[time] = mutableListOf()
        val cmd = ScheduledCommand(time, sender, command, currentId)
        @Suppress("ControlFlowWithEmptyBody")
        while (commandsList.containsKey(++currentId));
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            logErr("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        serializedCommands[cmd.id] = cmd.toJson()
        return "Scheduled ${getUserName(sender)} to run \"$command\" to run at $time."
    }

    fun schedule(scheduled: ScheduledCommand) = schedule(scheduled.sender, scheduled.commandData, scheduled.time)

    /**
     * Gets all of the commands scheduled by [sender].
     */
    fun getCommands(sender: User?): List<ScheduledCommand> =
            if (sender == null)
                getCommands()
            else
                commandsList.values.filter { it.sender == sender }

    /**
     * Gets all of the commands scheduled.
     */
    fun getCommands(): List<ScheduledCommand> = commandsList.values.toList()

    /**
     * Writes out all of the commands in a human-readable format.
     */
    fun getCommandStrings(sender: User, getAllCommands: Boolean = false) =
            getCommands(if (getAllCommands) null else sender)
                    .sortedBy { it.time }
                    .map {
                        "[${it.id}] ${formatTime(it.time)}: ${getUserName(it.sender)} - \"${commandDelimiters[sender.chat]}${it.commandData.command.name} ${
                            it.commandData.args.joinToString(" ")
                        }\""
                    }

    /**
     * Removes a command from the queue.
     */
    fun unschedule(sender: User, index: Int) = commandsList.remove(index)?.let {
        serializedCommands.remove(it.id)
        scheduledCommands[it.time]?.remove(it)
    } != null
}

/**
 * Schedules [command] to run at [time] from [sender].
 */
fun schedule(sender: User, command: CommandData, time: OffsetDateTime) = SchedulerThread.schedule(sender, command, time)

val pluginWrappers: MutableList<PluginWrapper> get() = pluginManager.plugins

class PluginManagerWithConfiguration(list: List<Path>): DefaultPluginManager(list) {
    val configuration = Configuration.conf
}

class core {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Parse command line arguments.
            val argParser = ArgumentParsers.newFor("Convergence Bot").build()
                    .defaultHelp(true)
                    .description("Sets the paths used by the bot.")

            val paths = argParser.addArgumentGroup("Paths")
            paths.addArgument("-pp", "--plugin-path")
                    .dest("pluginPath")
                    .nargs("+")
                    .type(List::class.java)
                    .default = listOf(convergencePath.resolve("plugins").toString())
            commandLineArgs = try {
                argParser.parseArgs(args)
            } catch (e: ArgumentParserException) {
                logErr("Failed to parse command line arguments. Printing stack trace:")
                logErr(e)
                return
            }

            // It'd be silly to get this list, convert it to a set, then convert it back to a list for the plugin
            // manager, so we'll keep this locally so we can construct the plugin manager.
            commandLineArgs.get<List<String>>("pluginPath").map { Paths.get(it) }.let {
                pluginManager = PluginManagerWithConfiguration(it)
                pluginPaths = it.toSet()
            }

            // Remove "just now" as an option for time formatting. 5 minutes for "just now" is annoying.
            Humanize.prettyTimeFormat().prettyTime.removeUnit(JustNow::class.java)

            registerProtocol(UniversalProtocol, FakeBaseInterface)

            log("Registering default commands...")
            registerDefaultCommands()

            log("Starting scheduler thread...")
            SchedulerThread.start()

            pluginManager.loadPlugins()
            if (pluginWrappers.isEmpty())
                log("No plugins loaded.")
            else
                log("Loaded Plugins:\t${pluginWrappers.joinToString("\t") { it.pluginId }}")

            moshi = moshiBuilder.build()
            for (wrapper in pluginWrappers) {
                log("Initializing plugin: ${wrapper.pluginId}")

                Thread({
                    wrapper.plugin.start()
                }, "${wrapper.pluginId}-thread").start()
                val plugin = wrapper.plugin as Plugin
                for (protocol in plugin.baseInterface.protocols)
                    registerProtocol(protocol, plugin.baseInterface)
            }

            // Update the chat map
            val chatUpdateDelay = 6000L
            for (protocol in protocols) {
                Thread({
                    Thread.sleep(chatUpdateDelay)
                    try {
                        (baseInterfaceMap[protocol]
                                ?: throw Exception("Protocol ${protocol.name} is not in the base interface map!"))
                                .getChats().forEach {
                                    if (it !in reverseChatMap) {
                                        while (currentChatID in chatMap) currentChatID++
                                        chatMap[currentChatID] = it
                                        reverseChatMap[it] = currentChatID
                                    }
                                }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, "Chat Init for ${protocol.name}").start()
            }
        }
    }
}
