package convergence.commands

import convergence.*
import convergence.command.*
import convergence.model.Chat
import convergence.model.CommandScope
import convergence.protocol.HasServer

fun addAlias(args: List<String>, chat: Chat, scope: CommandScope): String {
    val commandDelimiter = settings.commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER)
    val commandName = if (args[1].startsWith(commandDelimiter)) args[1].substringAfter(commandDelimiter) else args[1]
    val commandStr = "$commandDelimiter$commandName ${args.subList(2, args.size).joinToString(" ")}"
    val command = parseCommand(commandStr, commandDelimiter, chat)
        ?: return "Alias does not refer to a valid command!"
    if (!registerAlias(Alias(scope, args[0], command.command, command.args)))
        return "An alias with that name is already registered!"
    updateSettings()
    return "Alias \"${args[0]}\" registered to \"$commandStr\"."
}

fun addChatAlias(args: List<String>, chat: Chat) = addAlias(args, chat, chat)

fun addServerAlias(args: List<String>, chat: Chat) =
    if (chat !is HasServer<*>)
        "This protocol doesn't support servers!"
    else
        addAlias(args, chat, chat.server)

fun removeServerAlias(args: List<String>, chat: Chat): String {
    if (chat !is HasServer<*>)
        return "This protocol doesn't support servers!"
    if (args.size != 1) {
        return "Only one argument should be passed."
    }
    val server = chat.server
    if (server in settings.aliases && settings.aliases[server] is MutableMap && args[0] in settings.aliases[server]!!) {
        settings.aliases[server]!!.remove(args[0])
        updateSettings()
        return "Server alias \"${args[0]}\" removed."
    }
    return "No server alias with name \"${args[0]}\" found."
}

fun removeAlias(args: List<String>, chat: Chat): String {
    if (args.size != 1) {
        return "Only one argument should be passed."
    }
    if (chat in settings.aliases && settings.aliases[chat] is MutableMap && args[0] in settings.aliases[chat]!!) {
        settings.aliases[chat]!!.remove(args[0])
        updateSettings()
        return "Alias \"${args[0]}\" removed."
    }
    return "No alias with name \"${args[0]}\" found."
}
