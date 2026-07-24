package convergence

import org.natty.Parser
import java.time.OffsetDateTime
import kotlin.math.ceil
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

fun getUserFromName(chat: Chat, name: String): User? {
    var alternateOption: User? = null
    val protocol = chat.protocol
    if (protocol is CanMentionUsers) {
        protocol.getUserFromMentionText(chat, name)?.let {
            return it
        }
    }
    if (protocol is HasNicknames) {
        for (user in protocol.getUsers(chat)) {
            val currentName = protocol.getUserName(chat, user)
            val nickname = protocol.getUserNickname(chat, user)
            if (nickname == name || currentName == name)
                return user
            else if (alternateOption == null && name in currentName)
                alternateOption = user
        }
    } else {
        for (user in protocol.getUsers(chat)) {
            val currentName = protocol.getUserName(chat, user)
            if (currentName == name)
                return user
            else if (alternateOption == null && name in currentName)
                alternateOption = user
        }
    }
    return alternateOption
}


const val COMMANDS_PER_PAGE = 10
fun help(args: List<String>, chat: Chat): String {
    val sortedCommands = bot.commands.values.flatMap { it.values }.sortedBy { it.name }
    val numPages = ceil(sortedCommands.size.toDouble() / COMMANDS_PER_PAGE).toInt()
    val pageOrCommand = if (args.isEmpty()) 1 else args[0].toIntOrNull()?.coerceIn(1..numPages) ?: args[0]
    return when(pageOrCommand) {
        is Int -> buildString {
            append("Help page $pageOrCommand/$numPages:\n")
            for (i in 0 until COMMANDS_PER_PAGE) {
                val index = i + (pageOrCommand - 1) * COMMANDS_PER_PAGE
                if (index >= sortedCommands.size)
                    break
                val currentCommand = sortedCommands[index]
                append("${currentCommand.name} - ${currentCommand.helpText}\n")
            }
        }

        is String -> {
            val commandRegistry = getKoinService<CommandRegistryService>()
            val currentCommand = try {
                commandRegistry.getCommand(pageOrCommand.lowercase(), chat)
            } catch(_: CommandDoesNotExist) {
                return "There is no command with the name \"$pageOrCommand\"."
            }
            buildString {
                append("(")
                if (currentCommand is Command) {
                    append("C) - ")
                    append(currentCommand.helpText)
                    append("\nUsage: ")
                    append(currentCommand.syntaxText)
                } else {
                    append("A) - Runs \"")
                    append((currentCommand as Alias).commandText())
                    append("\"")
                }
            }
        }

        else -> "Expected Int or String, got ${pageOrCommand::class.simpleName ?: pageOrCommand::class.jvmName}."
    }
}

fun echo(args: List<String>) = args.joinToString(" ")
@Suppress("FunctionOnlyReturningConstant")
fun ping() = "Pong!"

fun addAlias(args: List<String>, chat: Chat, scope: CommandScope): String {
        val commandDelimiter = settings.commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER)
    val commandName = if (args[1].startsWith(commandDelimiter)) args[1].substringAfter(commandDelimiter) else args[1]
    val commandStr = "$commandDelimiter$commandName ${args.subList(2, args.size).joinToString(" ")}"
    val commandRegistry = getKoinService<CommandRegistryService>()
    val commandParserService = getKoinService<CommandParserService>()
    val command = commandParserService.parse(commandStr, commandDelimiter, chat, commandRegistry::getCommand)
        ?: return "Alias does not refer to a valid command!"
    if (!commandRegistry.registerAlias(Alias(scope, args[0], command.command, command.args)))
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

fun me(args: List<String>, chat: Chat, sender: User): String {
    val protocol = chat.protocol as? CanFormatMessages
    val (boldOpen, boldClose) = protocol?.getDelimiters(Format.bold) ?: Pair("", "")
    val messaging = getKoinService<MessagingService>()
    return "$boldOpen*${messaging.getUserName(chat, sender)} ${args.joinToString(" ")}$boldClose."
}

fun chats(): String {
    val builder = StringBuilder()
    for (protocol in bot.protocols) {
        try {
            val chats = protocol.getChats()
            builder.append("${protocol.name}\n\t")
            chats.forEach {
                val id = bot.reverseChatMap[it]
                if (id != null)
                    builder.append(it).append(" (").append(id).append("), ")
            }
            if (chats.any { it in bot.reverseChatMap }) {
                builder.setLength(builder.length - 2) // Remove the last ", ".
            }
            builder.append('\n')
        } catch(e: Exception) {
            defaultLogger.error("Error getting chats for ${protocol.name}.", e)
        }
    }
    return builder.toString()
}

val dateTimeParser = Parser()

fun target(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ").replace("%target", chat.protocol.getUserName(chat, user))
}

fun targetNick(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    if (chat.protocol !is HasNicknames)
        return "This protocol doesn't support nicknames!"
    val protocol = chat.protocol as HasNicknames
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ")
        .replace("%target", protocol.getUserNickname(chat, user) ?: chat.protocol.getUserName(chat, user))
}

fun commands(chat: Chat): String {
    val commandList = mutableListOf<String>()
    bot.commands[chat.protocol]?.forEach { commandList.add(it.key) }
    bot.commands[UniversalProtocol]?.forEach { commandList.add(it.key) }
    settings.linkedChats[chat]?.forEach { linked -> bot.commands[linked.protocol]?.forEach { commandList.add(it.key) } }
    commandList.sort()
    return if (commandList.isNotEmpty()) commandList.joinToString(", ") else "No commands found."
}

fun aliases(chat: Chat): String {
    val aliasList = mutableListOf<String>()
    settings.aliases[chat]?.forEach { aliasList.add(it.key) }
    settings.aliases[UniversalChat]?.forEach { aliasList.add(it.key) }
    settings.linkedChats[chat]?.forEach { linked -> settings.aliases[linked]?.forEach { aliasList.add(it.key) } }
    aliasList.sort()
    return if (aliasList.isNotEmpty()) "Aliases: ${aliasList.joinToString(", ")}" else "No aliases found."
}

fun unschedule(args: List<String>): String {
    val scheduleCommands = getKoinService<ScheduleCommands>()
    return scheduleCommands.unschedule(args)
}

fun link(args: List<String>, chat: Chat): String {
    val index = args[0].toIntOrNull() ?: return "${args[0]} is not a chat ID!"

    val chatToLink = bot.chatMap[index]
    return if (chatToLink != null) {
        settings.linkedChats.getOrPut(chat) { mutableSetOf() }.add(chatToLink)
        updateSettings()
        "${chatToLink.name} linked to ${chat.name}."
    } else
        "No chat with ID $index found."
}

fun unlink(args: List<String>, chat: Chat): String {
    val index = args[0].toIntOrNull() ?: return "${args[0]} is not a chat ID!"
    val toUnlink = bot.chatMap[index] ?: return "No chat with ID $index found."

    return when {
        chat !in settings.linkedChats -> "There are no chats linked to this one!"
        settings.linkedChats[chat]!!.remove(toUnlink) -> {
            if (settings.linkedChats[chat]!!.isEmpty())
                settings.linkedChats.remove(chat)
            updateSettings()
            "Removed ${toUnlink.name} from this chat's links."
        }

        else -> "That chat isn't linked to this one!"
    }
}

fun links(chat: Chat): String {
    return if (chat in settings.linkedChats)
        "Linked chats: ${settings.linkedChats[chat]!!.joinToString(", ") { "$it (${bot.reverseChatMap[it]})" }}"
    else
        "No chats are linked to this one."
}

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

fun createTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name in settings.timers)
        return "That timer already exists!"
    settings.timers[name] = OffsetDateTime.now()
    updateSettings()
    return "New timer \"$name\" created."
}

fun resetTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name !in settings.timers)
        return "That timer doesn't exist!"
    val oldVal = settings.timers[name]
    settings.timers[name] = OffsetDateTime.now()
    updateSettings()
    return "Timer reset. The time it was created or last time the timer was reset was " +
            "${formatTime(oldVal!!)} ($oldVal)."
}

fun checkTimer(args: List<String>): String {
    if (args.isEmpty())
        return "A name has to be provided."
    val name = args.joinToString(" ")
    if (name !in settings.timers)
        return "That timer doesn't exist!"
    val oldVal = settings.timers[name]
    updateSettings()
    return "The time it was created or last time the timer was reset was ${formatTime(oldVal!!)} ($oldVal)."
}

@Suppress("LongMethod")
fun registerDefaultCommands() {
    val commandRegistry = getKoinService<CommandRegistryService>()
    val scheduleCommands = getKoinService<ScheduleCommands>()

    commandRegistry.registerCommand(
        Command(
            UniversalProtocol, "exit", listOf(),
            { -> exitProcess(0) },
            "Exits the bot.",
            "exit (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "help", listOf(ArgumentSpec("Command-or-page", ArgumentType.STRING)), ::help,
            "Provides a paginated list of commands and their syntax, or specific help on a single command.",
            "help [command] or help [page number]"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "echo",
            listOf(ArgumentSpec("Message", ArgumentType.STRING)),
            ::echo,
            "Replies with the string passed to it.",
            "echo [message...] (All arguments are appended to each other with spaces)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "ping", listOf(), ::ping,
            "Replies with \"Pong!\".",
            "ping (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
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
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "removeAlias",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::removeAlias,
            "Removes an existing alias by its name.",
            "removeAlias (aliasName)"
        )
    )
    commandRegistry.registerCommand(
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
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "removeServerAlias",
            listOf(ArgumentSpec("name", ArgumentType.STRING)),
            ::removeServerAlias,
            "Removes an existing alias by its name.",
            "removeServerAlias (aliasName)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "me",
            listOf(ArgumentSpec("Message", ArgumentType.STRING)),
            ::me,
            "Replied \"*(username) (message)\" e.g. \"*Gian Laput is French.\"",
            "me [message...] (All arguments are appended to each other with spaces)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "chats", listOf(), ::chats,
            "Lists all chats the bot knows of by name.",
            "chats (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "commands",
            listOf(),
            { _, chat -> commands(chat) },
            "Lists all of the commands in this chat.",
            "commands (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "aliases",
            listOf(),
            { _, chat -> aliases(chat) },
            "Lists all of the aliases in this chat.",
            "aliases (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "schedule",
            listOf(
                ArgumentSpec("Time", ArgumentType.STRING),
                ArgumentSpec("Command", ArgumentType.STRING)
            ),
            { args, chat, sender -> scheduleCommands.schedule(args, chat, sender) },
            "Schedules a command to run later.",
            "schedule \"time\" \"command (with delimiter and arguments)\""
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "unschedule",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            { args -> scheduleCommands.unschedule(args) },
            "Unschedules a command, so it will not be run later. The ID can be obtained from the events command.",
            "unschedule (ID)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "events",
            listOf(),
            { _, chat, sender -> scheduleCommands.eventsFromUser(chat, sender)},
            "Lists all of the events you've made.",
            "events (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "allevents",
            listOf(),
            { _, chat -> scheduleCommands.events(chat) },
            "Lists all of the events in chronological order.",
            "allevents (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "eventsbyuser",
            listOf(),
            { _, chat, sender -> scheduleCommands.eventsByUser(chat, sender) },
            "Lists all events by user, then in chronological order.",
            "eventsbyuser (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "link",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            ::link,
            "Links a chat to this one. The ID can be obtained from the chats command.",
            "link (ID)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "unlink",
            listOf(ArgumentSpec("ID", ArgumentType.INTEGER)),
            ::unlink,
            "Unlinks a chat from this one. The ID can be obtained from the chats command.",
            "unlink (ID)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "links",
            listOf(),
            { _, chat -> links(chat) },
            "Lists all of the chats linked to this one.",
            "links (Takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol, "setdelimiter",
            listOf(ArgumentSpec("Delimiter", ArgumentType.STRING)),
            ::setDelimiter,
            "Changes the command delimiter of the current chat (default is !)",
            "setdelimiter (New delimiter)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol,
            "dumpSettings",
            listOf(),
            { -> objectMapper.writeValueAsString(settings) },
            "Prints out the content of the settings file.",
            "dumpSettings (takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol,
            "createTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::createTimer,
            "Creates a named timer, which you can reset later to see how long it's been since the last time.",
            "createTimer (name)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol,
            "resetTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::resetTimer,
            "Resets a timer created by createTimer and tells you how long it was running for.",
            "resetTimer (name)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol,
            "checkTimer",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            ::checkTimer,
            "Tells you how long a timer created by createTimer was running for.",
            "resetTimer (name)"
        )
    )
    commandRegistry.registerCommand(
        Command.of(
            UniversalProtocol,
            "timers",
            listOf(),
            { -> "The current active timers are: ${settings.timers.keys.joinToString(", ") }" },
            "Lists out all currently active timers.",
            "timers (takes no arguments)"
        )
    )
    commandRegistry.registerCommand(
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
    commandRegistry.registerCommand(
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
