package convergence

import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CommandDoesNotExist(cmd: String): Exception(cmd)

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

fun getCommandData(chat: Chat, message: String, sender: User): CommandData? = try {
    parseCommand(message, chat)
} catch(e: CommandDoesNotExist) {
    sendMessage(chat, sender, "No command exists with name \"${e.message}\".")
    null
} catch(e: InvalidEscapeSequenceException) {
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
