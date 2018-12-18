@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class CommandDoesNotExist(cmd: String): Exception(cmd)

val protocols = mutableSetOf<Protocol>()
val baseInterfaceMap = mutableMapOf<Protocol, BaseInterface>()
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
        for (i in 0..sortedHelpText.size) {
            if (command.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, command)
                break
            }
        }
    else
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
        for (i in 0..sortedHelpText.size) {
            if (alias.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, alias)
                break
            }
        }
    else
        sortedHelpText[0] = alias

    aliases[chat]!![alias.name] = alias
    return true
}

const val defaultCommandDelimiter = "!"
val commandDelimiters = mutableMapOf<Chat, String>()
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
 * Sends the provided message in the given chat.
 */
fun sendMessage(chat: Chat, message: String?) {
    if (message == null)
        return
    if (chat.protocol !in protocols) {
        System.err.println("Protocol \"${chat.protocol.name}\" not properly registered!")
        return
    }
    val baseInterface = baseInterfaceMap[chat.protocol]!!
    val bot = baseInterface.getBot(chat)
    baseInterface.sendMessage(chat, message, bot)
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
        "sender" to fun(b: BaseInterface, c: Chat, s: User): String { return b.getName(c, s) },
        "botname" to fun(b: BaseInterface, c: Chat, _: User): String { return b.getName(c, b.getBot(c)) },
        "chatname" to fun(b: BaseInterface, c: Chat, _: User): String { return b.getChatName(c) })

fun replaceAliasVars(message: String?, sender: User) = replaceAliasVars(sender.chat, message, sender)
/**
 * Replaces instances of the keys in [aliasVars] preceded by a percent sign with the result of the functions therein,
 * such as %sender with the name of the user who sent the message.
 */
fun replaceAliasVars(chat: Chat, message: String?, sender: User): String? {
    if (message == null)
        return null
    val stringBuilder = StringBuilder()
    var charIndex = -1
    val bitSet = BitSet(aliasVars.size)
    var anyTrue = false
    for (currentChar in message) {
        stringBuilder.append(currentChar)
        if (currentChar == '%')
            charIndex = 0
        if (charIndex >= 0) {
            var i = -1
            for (aliasVar in aliasVars) {
                i++ // I didn't use forEachIndexed here because of the return in this loop.
                if (bitSet[i]) {
                    anyTrue = true
                    if (charIndex == aliasVar.key.length) {
                        val baseInterface = baseInterfaceMap[chat.protocol]!!
                        stringBuilder.setLength(stringBuilder.length - aliasVar.key.length - 1)
                        stringBuilder.append(aliasVar.value(baseInterface, chat, sender))
                        charIndex = -1
                        break
                    }
                    if (currentChar != aliasVar.key[charIndex])
                        bitSet[i] = false
                }
            }
        }
        charIndex = if (anyTrue) charIndex + 1 else -1
    }
    return stringBuilder.toString()
}

fun getCommandData(message: String, sender: User): CommandData? {
    var commandData: CommandData? = null
    val chat = sender.chat
    try {
        commandData = parseCommand(message, commandDelimiters.getOrDefault(chat, defaultCommandDelimiter), chat)
    } catch (e: CommandDoesNotExist) {
        sendMessage(chat, "No command exists with name \"${e.message}\".")
    } catch (e: InvalidEscapeSequence) {
        sendMessage(chat, "Invalid escape sequence \"${e.message}\" passed. Are your backslashes correct?")
    }
    return commandData
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(message: String, sender: User) {
    val commandData = getCommandData(message, sender)
    if (commandData != null)
        runCommand(sender, commandData)
    forwardToLinkedChats(message, sender)
}

fun runCommand(sender: User, commandData: CommandData) {
    val chat = sender.chat
    val args = commandData.args
    sendMessage(chat, replaceAliasVars(chat, commandData.command.function(args, sender), sender))
}

val linkedChats = HashMap<Chat, MutableSet<Chat>>()
fun forwardToLinkedChats(message: String, sender: User) {
    val chat = sender.chat
    TODO("Implement me!")
}

const val allowedTimeDifference = 30

data class ScheduledCommand(val time: LocalDateTime, val sender: User, val commandData: CommandData, val id: Int)

object SchedulerThread: Thread() {
    private val scheduledCommands = TreeMap<LocalDateTime, ArrayList<ScheduledCommand>>()
    private val commandsList = HashMap<Int, ScheduledCommand>()
    private var currentId: Int = 0
    override fun run() {
        while (this.isAlive) {
            val now = LocalDateTime.now()
            val commandIter = scheduledCommands.iterator()
            while (commandIter.hasNext()) {
                val cmdEntry = commandIter.next()
                if (cmdEntry.key.until(now, ChronoUnit.SECONDS) in 0..allowedTimeDifference) {
                    for (cmdPair in cmdEntry.value)
                        runCommand(cmdPair.sender, cmdPair.commandData)
                    commandIter.remove()
                } else
                    break
            }
            sleep(1000)
        }
    }

    fun schedule(sender: User, command: CommandData, time: LocalDateTime): String? {
        if (time !in scheduledCommands)
            scheduledCommands[time] = ArrayList(2)
        scheduledCommands[time]!!.add(ScheduledCommand(time, sender, command, currentId++))
        return "Scheduled ${getUserName(sender)} to run \"$command\" to run at $time."
    }

    fun getCommands(sender: User): List<ScheduledCommand> {
        return commandsList.values.toList()
    }

    fun getCommandStrings(sender: User): List<String> {
        val list = ArrayList<String>()
        for (command in getCommands(sender))
            list.add("[${command.id}]@${command.time} ${command.sender} - \"${command.commandData.command} ${command.commandData.args.joinToString(" ")}\"")
        return list
    }

    fun unschedule(sender: User, index: Int): Boolean {
        val commandToRemove = commandsList.remove(index)
        if (commandToRemove != null)
            scheduledCommands[commandToRemove.time]?.remove(commandToRemove)
        else
            return false
        return true
    }
}

/**
 * Schedules [command] to run at [time] from [sender].
 */
fun schedule(sender: User, command: CommandData, time: LocalDateTime): String? = SchedulerThread.schedule(sender, command, time)

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
                    .type(File::class.java).default = File(System.getProperty("user.home"), ".convergence")
            paths.addArgument("-bpp", "--basic-plugin-path")
                    .type(File::class.java).default = File("consolePlugin/build/libs")
            try {
                commandLineArgs = argParser.parseArgs(args)
            } catch (e: ArgumentParserException) {
                System.err.println(e.message)
                return
            }
            println("Registering default commands...")
            registerDefaultCommands()
            println("Starting scheduler thread...")
            SchedulerThread.start()
            val plugins = PluginLoader.loadPlugin((commandLineArgs.get("plugin_path") as File).path)
            if (plugins.isEmpty())
                println("No plugins loaded.")
            else
                println("Loaded Plugins:\t${plugins.joinToString("\t") { it.name }}")
            for (plugin in plugins) {
                println("Initializing plugin: ${plugin.name}")
                Thread(plugin::init).start()
            }
        }
    }
}
