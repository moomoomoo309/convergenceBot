@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import com.fasterxml.jackson.databind.ObjectMapper
import convergence.console.ConsoleProtocol
import convergence.discord.DiscordProtocol
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import to.lova.humanize.time.HumanizeTime
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.set

class CommandDoesNotExist(cmd: String): Exception(cmd)

inline fun <reified T> ObjectMapper.readValue(value: String): T {
    return this.readValue(value, T::class.java)
}

inline fun <reified T> ObjectMapper.readValue(value: File): T {
    return this.readValue(value, T::class.java)
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

    if (sortedHelpText.isEmpty())
        sortedHelpText.add(alias)
    else
        sortedHelpText.add(-sortedHelpText.binarySearch(alias) - 1, alias)

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
fun sendMessage(chat: Chat, sender: User, message: String?) {
    if (sender != chat.protocol.getBot(chat))
        sendMessage(chat, message)
    forwardToLinkedChats(chat, message, sender, true)
}

/**
 * Sends [message] in [chat], not forwarding it to linked chats.
 */
fun sendMessage(chat: Chat, message: String?) {
    if (message == null)
        return
    chat.protocol.sendMessage(chat, message)
}

/**
 * Gets the nickname (if applicable) or name of a user.
 */
fun getUserName(chat: Chat, sender: User): String {
    val protocol = chat.protocol
    return if (protocol is HasNicknames)
        protocol.getUserNickname(chat, sender) ?: protocol.getName(chat, sender)
    else
        protocol.getName(chat, sender)
}

/**
 * Replaces instances of the keys in [aliasVars] preceded by a percent sign with the result of the functions therein,
 * such as %sender with the name of the user who sent the message.
 */
fun replaceAliasVars(chat: Chat, message: String?, sender: User): String? {
    if (message == null)
        return null
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
                        stringBuilder.setLength(stringBuilder.length - string.length - 1)
                        stringBuilder.append(aliasVar(chat, sender))
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

fun getCommandData(chat: Chat, message: String, sender: User): CommandData? = try {
    parseCommand(message, chat)
} catch(e: CommandDoesNotExist) {
    sendMessage(chat, sender, "No command exists with name \"${e.message}\".")
    null
} catch(e: InvalidEscapeSequence) {
    sendMessage(chat, sender, "Invalid escape sequence \"${e.message}\" passed. Are your backslashes correct?")
    null
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(chat: Chat, message: String, sender: User, images: Array<Image> = emptyArray()) {
    defaultLogger.info(
        "[${
            getUserName(
                chat,
                sender
            )
        }]: $message ${if (images.isNotEmpty()) "+${images.size} images" else ""}"
    )
    forwardToLinkedChats(chat, message, sender, images)
    getCommandData(chat, message, sender)?.let { runCommand(chat, sender, it) }
}

fun getStackTraceText(e: Exception): String = ByteArrayOutputStream().let {
    e.printStackTrace(PrintStream(it))
    it.toString("UTF8")
}

fun runCommand(chat: Chat, sender: User, command: CommandData) = try {
    sendMessage(chat, sender, replaceAliasVars(chat, command(chat, sender), sender))
} catch(e: Exception) {
    sendMessage(chat, sender, "Error while running command! Stack trace:\n${getStackTraceText(e)}")
}

fun forwardToLinkedChats(chat: Chat, message: String?, sender: User, isCommand: Boolean = false) =
    forwardToLinkedChats(chat, message, sender, emptyArray(), isCommand)

fun forwardToLinkedChats(
    chat: Chat,
    message: String?,
    sender: User,
    images: Array<Image> = emptyArray(),
    isCommand: Boolean = false
) {
    if (message == null)
        return
    val protocol = chat.protocol
    var boldOpen = ""
    var boldClose = ""
    // Try to get the delimiters for bold, if possible.
    if (protocol is CanFormatMessages && Format.bold in protocol.supportedFormats) {
        val delimiters = protocol.getDelimiters(Format.bold)
        boldOpen = delimiters?.first ?: boldOpen
        boldClose = delimiters?.second ?: boldClose
    }

    // Send the messages out to the linked chats, if there are any. Don't error if there aren't any.
    val bot = chat.protocol.getBot(chat)
    if (isCommand || sender != bot)
        if (chat in linkedChats)
            for (linkedChat in linkedChats[chat]!!) {
                val msg = "$boldOpen${getUserName(chat, if (isCommand) bot else sender)}:$boldClose $message"
                if (linkedChat.protocol is HasImages && images.isNotEmpty())
                    linkedChat.protocol.sendImages(linkedChat, msg, sender, *images)
                else
                    sendMessage(linkedChat, msg)
            }
}

fun formatTime(time: OffsetDateTime): String = HumanizeTime.fromNow(time)

data class ScheduledCommand(
    val time: OffsetDateTime,
    val chat: Chat,
    val sender: User,
    val commandData: CommandData,
    val id: Int,
)

/**
 * Checks if commands scheduled for later are ready to be run yet. Can give information on the commands in its queue.
 */
object CommandScheduler: Thread() {
    private const val allowedTimeDifferenceSeconds = 30
    private const val updatesPerSecond = 1

    private val scheduledCommands = sortedMapOf<OffsetDateTime, MutableList<ScheduledCommand>>()
    private val commandsList = sortedMapOf<Int, ScheduledCommand>()
    private var currentId: Int = 0

    fun loadFromFile() {
        serializedCommands.values.forEach { cmd ->
            commandsList[cmd.id] = cmd
            scheduledCommands.getOrPut(cmd.time) { mutableListOf(cmd) }
        }
    }

    override fun run() {
        while (isAlive) {
            val now = OffsetDateTime.now()
            for ((cmdTime, cmdList) in scheduledCommands) {
                if (cmdTime.isBefore(now)) {
                    if (cmdTime.until(now, ChronoUnit.SECONDS) in 0..allowedTimeDifferenceSeconds) {
                        for (cmd in cmdList) {
                            runCommand(cmd.chat, cmd.sender, cmd.commandData)
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
                    } else {
                        for (cmd in cmdList) {
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
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
    fun schedule(chat: Chat, sender: User, command: CommandData, time: OffsetDateTime): String {
        if (time !in scheduledCommands)
            scheduledCommands[time] = mutableListOf()
        val cmd = ScheduledCommand(time, chat, sender, command, currentId)
        while (commandsList.containsKey(currentId))
            currentId++
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            defaultLogger.error("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        serializedCommands[cmd.id] = cmd
        Settings.update()
        return "Scheduled ${getUserName(chat, sender)} to run \"$command\" to run at $time."
    }

    fun schedule(scheduled: ScheduledCommand) =
        schedule(scheduled.chat, scheduled.sender, scheduled.commandData, scheduled.time)

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
fun schedule(chat: Chat, sender: User, command: CommandData, time: OffsetDateTime) =
    CommandScheduler.schedule(chat, sender, command, time)

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

            val commandLineArgs = try {
                argParser.parseArgs(args)
            } catch(e: ArgumentParserException) {
                defaultLogger.error("Failed to parse command line arguments. Printing stack trace:")
                defaultLogger.error(getStackTraceText(e))
                return
            }

            convergencePath = Paths.get(commandLineArgs.get<List<String>>("convergencePath").first())
            readSettings()

            defaultLogger.info("Registering default commands...")
            registerDefaultCommands()

            protocols.add(UniversalProtocol)
            protocols.add(ConsoleProtocol)
            protocols.add(DiscordProtocol)
            // Update the chat map
            for (protocol in protocols) {
                defaultLogger.info("Initializing ${protocol.name}...")
                try {
                    protocol.init()
                    val chats = protocol.getChats()
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

            defaultLogger.info("Starting command scheduler...")
            CommandScheduler.start()

            CommandScheduler.loadFromFile()
        }
    }
}
