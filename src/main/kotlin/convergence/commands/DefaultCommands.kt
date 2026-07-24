package convergence.commands

import convergence.UniversalProtocol
import convergence.command.ArgumentSpec
import convergence.command.ArgumentType
import convergence.command.Command
import convergence.command.registerCommand
import convergence.model.Chat
import convergence.objectMapper
import convergence.settings
import convergence.updateSettings
import kotlin.system.exitProcess

/**
 * Sets the Command delimiter used for the bot's commands. (is it !help, |help, @help, or something else?)
 */
fun setCommandDelimiter(chat: Chat, commandDelimiter: String): Boolean {
    if (commandDelimiter.any { it.isWhitespace() || it == '"' })
        return false
    settings.commandDelimiters[chat] = commandDelimiter
    updateSettings()
    return true
}

fun setDelimiter(args: List<String>, chat: Chat): String = when {
    args.isEmpty() -> "You need to pass the new delimiter!"
    setCommandDelimiter(chat, args[0]) -> "Command delimiter set to \"${args[0]}\".".also { updateSettings() }
    else -> "\"${args[0]}\" is not a valid command delimiter!"
}

@Suppress("LongMethod")
fun registerDefaultCommands() {
    registerCommand(
        Command(
            UniversalProtocol, "exit", listOf(),
            { -> exitProcess(0) },
            "Exits the bot.",
            "exit (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "help", listOf(ArgumentSpec("Command-or-page", ArgumentType.STRING)), ::help,
            "Provides a paginated list of commands and their syntax, or specific help on a single command.",
            "help [command] or help [page number]"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "echo",
            listOf(ArgumentSpec("Message", ArgumentType.STRING)),
            ::echo,
            "Replies with the string passed to it.",
            "echo [message...] (All arguments are appended to each other with spaces)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "ping", listOf(), ::ping,
            "Replies with \"Pong!\".",
            "ping (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "alias",
            listOf(
                ArgumentSpec("Commandname", ArgumentType.STRING),
                ArgumentSpec("Command", ArgumentType.STRING)
            ), ::addChatAlias,
            "Registers an alias to an existing command in this channel.",
            "alias (commandName) \"!commandName [arguments...]\""
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "removeAlias",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::removeAlias,
            "Removes an existing alias by its name.",
            "removeAlias (aliasName)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "serverAlias",
            listOf(
                ArgumentSpec("name", ArgumentType.STRING),
                ArgumentSpec("Command", ArgumentType.STRING)
            ), ::addServerAlias,
            "Registers an alias to an existing command in this server.",
            "serverAlias (commandName) \"commandName [arguments...]\" " +
                    "(Command inside parentheses takes however many parameters that command takes)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "removeServerAlias",
            listOf(ArgumentSpec("name", ArgumentType.STRING)),
            ::removeServerAlias,
            "Removes an existing alias by its name.",
            "removeServerAlias (aliasName)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "me",
            listOf(ArgumentSpec("Message", ArgumentType.STRING)),
            ::me,
            "Replied \"*(username) (message)\" e.g. \"*Gian Laput is French.\"",
            "me [message...] (All arguments are appended to each other with spaces)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "chats", listOf(), ::chats,
            "Lists all chats the bot knows of by name.",
            "chats (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "commands",
            listOf(),
            { _, chat -> commands(chat) },
            "Lists all of the commands in this chat.",
            "commands (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "aliases",
            listOf(),
            { _, chat -> aliases(chat) },
            "Lists all of the aliases in this chat.",
            "aliases (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "schedule",
            listOf(
                ArgumentSpec("Time", ArgumentType.STRING),
                ArgumentSpec("Command", ArgumentType.STRING)
            ),
            ::schedule,
            "Schedules a command to run later.",
            "schedule \"time\" \"command (with delimiter and arguments)\""
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "unschedule",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            ::unschedule,
            "Unschedules a command, so it will not be run later. The ID can be obtained from the events command.",
            "unschedule (ID)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "events",
            listOf(),
            { _, chat, sender -> eventsFromUser(chat, sender)},
            "Lists all of the events you've made.",
            "events (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "allevents",
            listOf(),
            { _, chat -> events(chat) },
            "Lists all of the events in chronological order.",
            "allevents (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "eventsbyuser",
            listOf(),
            { _, chat, sender -> eventsByUser(chat, sender) },
            "Lists all events by user, then in chronological order.",
            "eventsbyuser (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "link",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            ::link,
            "Links a chat to this one. The ID can be obtained from the chats command.",
            "link (ID)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "unlink",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            ::unlink,
            "Unlinks a chat from this one. The ID can be obtained from the chats command.",
            "unlink (ID)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "links",
            listOf(),
            { _, chat -> links(chat) },
            "Lists all of the chats linked to this one.",
            "links (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "setdelimiter",
            listOf(ArgumentSpec("Delimiter", ArgumentType.STRING)),
            ::setDelimiter,
            "Changes the command delimiter of the current chat (default is !)",
            "setdelimiter (New delimiter)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "dumpSettings",
            listOf(),
            { -> objectMapper.writeValueAsString(settings) },
            "Prints out the content of the settings file.",
            "dumpSettings (takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "createTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::createTimer,
            "Creates a named timer, which you can reset later to see how long it's been since the last time.",
            "createTimer (name)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "resetTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::resetTimer,
            "Resets a timer created by createTimer and tells you how long it was running for.",
            "resetTimer (name)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "checkTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::checkTimer,
            "Tells you how long a timer created by createTimer was running for.",
            "resetTimer (name)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "timers",
            listOf(),
            { -> "The current active timers are: ${settings.timers.keys.joinToString(", ") }" },
            "Lists out all currently active timers.",
            "timers (takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "target",
            listOf(
                ArgumentSpec("Message", ArgumentType.STRING),
                ArgumentSpec("User", ArgumentType.STRING)
            ),
            ::target,
            "Allows you to target another user to use them as an alias var. Used primarily to make aliases.",
            "!target (message...) (user)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol,
            "targetnick",
            listOf(
                ArgumentSpec("Message", ArgumentType.STRING),
                ArgumentSpec("User", ArgumentType.STRING)
            ),
            ::targetNick,
            "Allows you to target another user to use them as an alias var. Uses the target's nickname.",
            "!target (message...) (user)"
        )
    )
}
