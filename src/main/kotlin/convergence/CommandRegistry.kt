package convergence

import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CommandDoesNotExist(cmd: String): Exception(cmd)

/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(command: Command): Boolean {
    val protocol = command.protocol
    if (protocol !in commands || commands[protocol] !is MutableMap)
        commands[protocol] = mutableMapOf(command.name.lowercase() to command)
    val commandsInProtocol = commands[protocol] ?: return false

    if (command.name.lowercase() in commandsInProtocol)
        return false

    if (sortedHelpText.isEmpty())
        sortedHelpText.add(command)
    else
        sortedHelpText.add(-sortedHelpText.binarySearch(command) - 1, command)
    commandsInProtocol[command.name.lowercase()] = command
    return true
}

/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */
fun registerAlias(alias: Alias): Boolean {
    val chat = alias.scope
    val aliases = aliases
    if (chat !in aliases || aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf()
    val aliasesInChat = aliases[chat]!!

    if (alias.name.lowercase() in aliasesInChat)
        return false

    aliasesInChat[alias.name.lowercase()] = alias
    return true
}

fun getCommandData(chat: Chat, message: String, sender: User): CommandData? = try {
    parseCommand(message, chat)
} catch(e: CommandDoesNotExist) {
    sendMessage(chat, sender, "No command exists with name \"${e.message}\".")
    null
} catch(e: InvalidEscapeSequenceException) {
    sendMessage(chat, sender, "Invalid escape sequence \"${e.message}\" passed. Are your backslashes correct?")
    null
}

fun getStackTraceText(e: Exception): String = ByteArrayOutputStream().let {
    e.printStackTrace(PrintStream(it))
    it.toString("UTF8")
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(chat: Chat, message: IncomingMessage, sender: User, images: Array<Image> = emptyArray()) {
    val text = message.toSimple().text
    defaultLogger.info(
        "[${getUserName(chat, sender)}]: $text${if (images.isNotEmpty()) " +${images.size} images" else ""}"
    )
    forwardToLinkedChats(chat, message.toOutgoing(), sender, images)
    try {
        getCommandData(chat, text, sender)?.let { (command, args) ->
            runCommand(chat, sender, command, args)
        }
    } catch(e: Exception) {
        sendMessage(chat, sender, "Error while running command! Stack trace:\n${getStackTraceText(e)}")
    }
}

fun runCommand(chat: Chat, sender: User, command: Command, args: List<String>) =
    sendMessage(chat, sender, replaceAliasVars(chat, command.function(args, chat, sender), sender))

fun runCommand(scheduledCommand: ScheduledCommand) {
    val command = getCommand(scheduledCommand.commandName.lowercase(), scheduledCommand.chat) as Command
    runCommand(scheduledCommand.chat, scheduledCommand.sender, command, scheduledCommand.args)
}
