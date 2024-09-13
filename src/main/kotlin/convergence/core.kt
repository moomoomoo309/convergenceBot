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
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.set

class CommandDoesNotExist(cmd: String): Exception(cmd)

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
    val commandsInChat = commands[chat] ?: return false

    if (command.name in commandsInChat)
        return false

    if (sortedHelpText.isEmpty())
        sortedHelpText.add(command)
    else
        sortedHelpText.add(-sortedHelpText.binarySearch(command) - 1, command)
    commandsInChat[command.name] = command
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
    val aliasesInChat = aliases[chat]!!

    if (alias.name in aliasesInChat)
        return false

    if (sortedHelpText.isNotEmpty())
        for ((i, commandLike) in sortedHelpText.withIndex())
            if (alias.name < commandLike.name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, alias)
                break
            } else
                sortedHelpText[0] = alias

    aliasesInChat[alias.name] = alias
    return true
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
        defaultLogger.error("Protocol \"${chat.protocol.name}\" not properly registered!")
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
    defaultLogger.info("[${getUserName(sender)}]: $message")
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
): JsonConvertible

/**
 * Checks if commands scheduled for later are ready to be run yet. Can give information on the commands in its queue.
 */
object CommandScheduler: Thread() {
    private const val allowedTimeDifferenceSeconds = 30
    private const val updatesPerSecond = 1

    private val scheduledCommands = sortedMapOf<OffsetDateTime, MutableList<ScheduledCommand>>()
    private val commandsList = sortedMapOf<Int, ScheduledCommand>()
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
        while (isAlive) {
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
                } else // It's already sorted chronologically, so all following events are early.
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
        while (commandsList.containsKey(currentId))
            currentId++
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            defaultLogger.error("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        serializedCommands[cmd.id] = cmd.toJson()
        return "Scheduled ${getUserName(sender)} to run \"$command\" to run at $time."
    }

    fun schedule(scheduled: ScheduledCommand) = schedule(scheduled.sender, scheduled.commandData, scheduled.time)

    /**
     * Gets all the commands scheduled by [sender].
     */
    fun getCommands(sender: User?): List<ScheduledCommand> =
        if (sender == null)
            getCommands()
        else
            commandsList.values.filter { it.sender == sender }

    /**
     * Gets all the commands scheduled.
     */
    fun getCommands(): List<ScheduledCommand> = commandsList.values.toList()

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
fun schedule(sender: User, command: CommandData, time: OffsetDateTime) =
    CommandScheduler.schedule(sender, command, time)

val pluginWrappers: MutableList<PluginWrapper> get() = pluginManager.plugins

class core {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Parse command line arguments.
            val argParser = ArgumentParsers.newFor("Convergence Bot").build()
                .defaultHelp(true)
                .description("Sets the paths used by the bot.")

            val paths = argParser.addArgumentGroup("Paths")
            paths.addArgument("-c", "--convergence-path")
                .dest("convergencePath")
                .nargs(1)
                .type(String::class.java)
                .default = listOf(Paths.get(System.getProperty("user.home"), ".convergence").toString())


            paths.addArgument("-pp", "--plugin-path")
                .dest("pluginPath")
                .nargs("+")
                .type(String::class.java)
                .default = listOf(Paths.get(System.getProperty("user.home"), ".convergence", "plugins").toString())
            commandLineArgs = try {
                argParser.parseArgs(args)
            } catch (e: ArgumentParserException) {
                defaultLogger.error("Failed to parse command line arguments. Printing stack trace:")
                defaultLogger.error(getStackTraceText(e))
                return
            }

            convergencePath = Paths.get(commandLineArgs.get<List<String>>("convergencePath").first())

            // It'd be silly to get this list, convert it to a set, then convert it back to a list for the plugin
            // manager, so we'll keep this locally, so we can construct the plugin manager.
            commandLineArgs.get<List<String>>("pluginPath").map { Paths.get(it) }.let {
                pluginManager = DefaultPluginManager(it)
                pluginPaths = it.toMutableList()
            }

            // Remove "just now" as an option for time formatting. 5 minutes for "just now" is annoying.
            Humanize.prettyTimeFormat().prettyTime.removeUnit(JustNow::class.java)

            Settings.putAll(initSettings())
            SharedVariables.putAll(SharedVariable.entries.associate { it.name to it.defaultValue }
                .filter { it.key !in SharedVariables && it.value != null })

            registerProtocol(UniversalProtocol, FakeBaseInterface)

            defaultLogger.info("Registering default commands...")
            registerDefaultCommands()

            defaultLogger.info("Starting command scheduler...")
            CommandScheduler.start()

            pluginManager.loadPlugins()
            if (pluginWrappers.isEmpty())
                defaultLogger.info("No plugins loaded.")
            else
                defaultLogger.info("Loaded Plugins:\t${pluginWrappers.joinToString("\t") { it.pluginId }}")

            for (wrapper in pluginWrappers) {
                defaultLogger.info("Preinitializing plugin: ${wrapper.pluginId}")
                (wrapper.plugin as Plugin).preinit()
            }

            moshi = moshiBuilder
                .addLast(chatAdapterFactory)
                .addLast(userAdapterFactory)
                .addLast(imageAdapterFactory)
                .addLast(messageHistoryAdapterFactory)
                .addLast(stickerAdapterFactory)
                .addLast(formatAdapterFactory)
                .addLast(customEmojiAdapterFactory)
                .build()
            for (wrapper in pluginWrappers) {
                defaultLogger.info("Initializing plugin: ${wrapper.pluginId}")
                Thread({
                    wrapper.plugin.start()
                }, "${wrapper.pluginId}-thread").start()
                val plugin = wrapper.plugin as Plugin
                registerProtocols(plugin.baseInterface.protocols, plugin.baseInterface)
            }
            Thread.sleep(3000L)

            // Update the chat map
            for (protocol in protocols) {
                defaultLogger.info("Initializing ${protocol.name}...")
                try {
                    val chats = baseInterfaceMap[protocol]?.getChats()
                        ?: throw Exception("Protocol ${protocol.name} is not in the base interface map!")
                    for (chat in chats) {
                        if (chat !in reverseChatMap) {
                            while (currentChatID in chatMap)
                                currentChatID++
                            chatMap[currentChatID] = chat
                            reverseChatMap[chat] = currentChatID
                        }
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
