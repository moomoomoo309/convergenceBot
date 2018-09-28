package convergence

import java.lang.StringBuilder
import java.util.*
import kotlin.math.nextUp

const val commandsPerPage = 10
private fun help(chat: Chat, args: List<String>, sender: User): String? {
    val pageOrCommand = if (args.isEmpty()) 1 else args[0].toIntOrNull() ?: args[0]
    val numPages = Math.ceil(sortedHelpText.size.toDouble() / commandsPerPage).nextUp().toInt()
    return when (pageOrCommand) {
        is Int -> {
            val helpText = StringBuilder("Help page $pageOrCommand/$numPages:\n")
            for (i in 0..commandsPerPage) {
                val currentCommand = sortedHelpText[i + pageOrCommand * commandsPerPage]
                helpText.append("(${if (currentCommand is Command) 'C' else 'A'}) ${currentCommand.name} - ${currentCommand.helpText}\n")
            }
            helpText.toString()
        }
        is String -> {
            val currentCommand = getCommand(pageOrCommand, chat)
            "(${if (currentCommand is Command) 'C' else 'A'}) - ${currentCommand.helpText}\nUsage: ${currentCommand.syntaxText}"
        }
        else -> "The help command has gone belly up somehow."
    }
}

private fun echo(chat: Chat, args: List<String>, sender: User): String? {
    return args.joinToString(" ")
}

private fun ping(chat: Chat, args: List<String>, sender: User): String? {
    return "Pong!"
}

private fun addAlias(chat: Chat, args: List<String>, sender: User): String? {
    val commandDelimiter = commandDelimiters.getOrDefault(chat, defaultCommandDelimiter)
    val command = parseCommand(commandDelimiter + args[1], commandDelimiter, chat)
            ?: return "Alias does not refer to a valid command!"
    if (!registerAlias(chat, Alias(args[0], command.command, command.args, command.command.helpText, command.command.syntaxText)))
        return ""
    return "Alias \"${args[0]}\" registered to \"${args[1]}\"."
}

private fun removeAlias(chat: Chat, args: List<String>, sender: User): String? {
    val validAliases = BitSet(args.size)
    var anyInvalid = false
    for (i in 0..args.size) {
        if (chat in aliases && aliases[chat] is MutableMap && args[i] in aliases[chat]!!) {
            aliases[chat]!!.remove(args[i])
            validAliases[i] = true
        } else
            anyInvalid = true
    }
    return if (anyInvalid)
        "Aliases \"${args.asSequence().filterIndexed { i, _ -> validAliases[i] }.joinToString("\", \"")}\" removed, " +
                "${args.asSequence().filterIndexed { i, _ -> !validAliases[i] }.joinToString("\", \"")} not removed."
    else "Aliases \"${args.asSequence().filterIndexed { i, _ -> validAliases[i] }.joinToString("\", \"")}\" removed."
}

private fun me(chat: Chat, args: List<String>, sender: User): String? {
    return "*${getUserName(chat, sender)} ${args.joinToString("")}"
}

private fun chats(unused: Chat, args: List<String>, sender: User): String? {
    val builder = StringBuilder()
    for (protocolEntry in protocols) {
        builder.append("${protocolEntry.value.name}\n\t")
        for (chat in protocolEntry.value.getChats())
            builder.append(chat.name).append(", ")
        builder.setLength(builder.length - 2) // Remove the last ", ".
        builder.append('\n')
    }
    return builder.toString()
}


fun registerDefaultCommands() {
    registerCommand(UniversalChat, Command("help", ::help,
            "Provides a paginated list of commands and their syntax, or specific help on a single command.",
            "help [command] or help [page number]"))
    registerCommand(UniversalChat, Command("echo", ::echo,
            "Replies with the string passed to it.",
            "echo [message...] (All arguments are appended to each other with spaces)"))
    registerCommand(UniversalChat, Command("ping", ::ping,
            "Replies with \"Pong!\".",
            "ping (Takes no arguments)"))
    registerCommand(UniversalChat, Command("alias", ::addAlias,
            "Registers an alias to an existing command.",
            "alias (commandName) \"commandName [arguments...]\" (Command inside parentheses takes however many parameters that command takes)"))
    registerCommand(UniversalChat, Command("me", ::me,
            "Replied \"*(username) (message)\" e.g. \"*Gian Laput is French.\"",
            "me [message...] (All arguments are appended to each other with spaces)"))
    registerCommand(UniversalChat, Command("chats", ::chats,
            "Lists all chats the bot knows of by name.",
            "chats (Takes no arguments)"))
}

