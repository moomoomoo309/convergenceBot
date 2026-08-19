package convergence.command

import convergence.*
import convergence.model.Chat
import convergence.model.Image
import convergence.model.IncomingMessage
import convergence.model.User
import convergence.protocol.HasServer
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CommandDoesNotExist(cmd: String): Exception(cmd)

/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(command: Command): Boolean {
    val protocol = command.protocol
    val commandsInProtocol = bot.commands.getOrPut(protocol) { mutableMapOf() }

    if (command.name.lowercase() in commandsInProtocol)
        return false

    commandsInProtocol[command.name.lowercase()] = command
    return true
}

fun registerCommands(vararg command: Command): List<Boolean> {
    return command.map { registerCommand(it) }
}

/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */
fun registerAlias(alias: Alias): Boolean {
    val chat = alias.scope
    val aliases = settings.aliases
    val aliasesInChat = aliases.getOrPut(chat) { mutableMapOf() }

    if (alias.name.lowercase() in aliasesInChat)
        return false

    alias.protocol.aliasCreated(alias)
    aliasesInChat[alias.name.lowercase()] = alias
    return true
}

fun parseCommand(chat: Chat, sender: User, message: String): CommandWithArgs? = try {
    parseCommand(chat, message)
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
 * Log the message, forwarding it to linked chats if applicable, and running the command if present
 * in the message.
 */
fun processMessage(chat: Chat, sender: User, message: IncomingMessage, images: Array<Image> = emptyArray()) {
    val text = message.toSimple().text
    messageLogger.info(
        "[${if (chat is HasServer<*>) chat.server.name + "#" else ""}${chat.name}] ${getUserName(chat, sender)}: " +
                "$text${if (images.isNotEmpty()) " +${images.size} images" else ""}"
    )
    forwardToLinkedChats(chat, sender, message.toOutgoing(), images)
    try {
        parseCommand(chat, sender, text)?.let { (command, args) ->
            runCommand(chat, sender, command, args)
        }
    } catch(e: Exception) {
        val errMsg = if (settings.debugMode) getStackTraceText(e) else e.message
        sendMessage(chat, sender, "Error while running command! Stack trace:\n$errMsg")
        defaultLogger.error("Error while running command!", e)
    }
}

fun runCommand(chat: Chat, sender: User, command: Command, args: List<String>) =
    sendMessage(chat, sender, command(args, chat, sender))
