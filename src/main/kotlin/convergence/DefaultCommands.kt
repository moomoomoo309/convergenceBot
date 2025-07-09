@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import convergence.CommandScheduler.getCommands
import org.natty.DateGroup
import org.natty.Parser
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.math.ceil
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

class UnregisteredChat: Exception()

@Suppress("NOTHING_TO_INLINE") // It's inlined so that it won't show up in the stack trace.
inline fun unregisteredChat(chat: Chat) {
    try {
        throw UnregisteredChat()
    } catch(e: UnregisteredChat) {
        val writer = StringWriter()
        e.printStackTrace(PrintWriter(writer))
        chat.protocol.sendMessage(
            chat, "Bot error: Chat is missing a protocol.\nStack Trace:\n$writer"
        )
        writer.close()
    }
}

fun getUserFromName(chat: Chat, name: String): User? {
    var alternateOption: User? = null
    val protocol = chat.protocol
    for (user in protocol.getUsers(chat)) {
        val currentName = protocol.getName(chat, user)
        if (currentName == name)
            return user
        else if (alternateOption == null && name in currentName)
            alternateOption = user
    }
    return alternateOption
}

fun getFullName(chat: Chat, name: String): String? {
    val user = getUserFromName(chat, name)
    if (chat.protocol in protocols)
        return user?.let { chat.protocol.getName(chat, user) }
    unregisteredChat(chat)
    return null
}


const val commandsPerPage = 10
fun help(args: List<String>, chat: Chat): String {
    val numPages = ceil(sortedHelpText.size.toDouble() / commandsPerPage).toInt()
    val pageOrCommand = if (args.isEmpty()) 1 else args[0].toIntOrNull()?.coerceIn(1..numPages) ?: args[0]
    return when(pageOrCommand) {
        is Int -> {
            val helpText = StringBuilder("Help page $pageOrCommand/$numPages:\n")
            for (i in 0..commandsPerPage) {
                val index = i + (pageOrCommand - 1) * (commandsPerPage + 1)
                if (index >= sortedHelpText.size)
                    break
                val currentCommand = sortedHelpText[index]
                val indicator = if (currentCommand is Command) 'C' else 'A'
                helpText.append("($indicator) ${currentCommand.name}${if (currentCommand is Command) " - " + (currentCommand.helpText) else ""}\n")
            }
            helpText.toString()
        }

        is String -> {
            val currentCommand = try {
                getCommand(pageOrCommand.lowercase(), chat)
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
                } else
                    append("A)")
            }
        }

        else -> "Expected Int or String, got ${pageOrCommand::class.simpleName ?: pageOrCommand::class.jvmName}."
    }
}

fun echo(args: List<String>) = args.joinToString(" ")
fun ping() = "Pong!"

fun addAlias(args: List<String>, chat: Chat, scope: CommandScope): String {
    val commandDelimiter = commandDelimiters.getOrDefault(chat, defaultCommandDelimiter)
    val commandName = if (args[1].startsWith(commandDelimiter)) args[1].substringAfter(commandDelimiter) else args[1]
    val commandStr = "$commandDelimiter$commandName ${args.subList(2, args.size).joinToString(" ")}"
    val command = parseCommand(commandStr, commandDelimiter, chat)
        ?: return "Alias does not refer to a valid command!"
    if (!registerAlias(Alias(chat, args[0], command.command, command.args)))
        return "An alias with that name is already registered!"
    Settings.update()
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
    if (chat in aliases && aliases[server] is MutableMap && args[0] in aliases[server]!!) {
        aliases[server]!!.remove(args[0])
        Settings.update()
        return "Alias \"${args[0]}\" removed."
    }
    return "No alias with name \"${args[0]}\" found."
}

fun removeAlias(args: List<String>, chat: Chat): String {
    if (args.size != 1) {
        return "Only one argument should be passed."
    }
    if (chat in aliases && aliases[chat] is MutableMap && args[0] in aliases[chat]!!) {
        aliases[chat]!!.remove(args[0])
        Settings.update()
        return "Alias \"${args[0]}\" removed."
    }
    return "No alias with name \"${args[0]}\" found."
}

fun me(args: List<String>, chat: Chat, sender: User): String {
    val protocol = chat.protocol as? CanFormatMessages
    val (boldOpen, boldClose) = protocol?.getDelimiters(Format.bold) ?: Pair("", "")
    return "$boldOpen*${getUserName(chat, sender)} ${args.joinToString(" ")}$boldClose."
}

fun chats(): String {
    val builder = StringBuilder()
    for (protocol in protocols) {
        try {
            val chats = protocol.getChats()
            builder.append("${protocol.name}\n\t")
            chats.forEach {
                if (it !in reverseChatMap) {
                    while (currentChatID in chatMap)
                        currentChatID++
                    chatMap[currentChatID] = it
                    reverseChatMap[it] = currentChatID
                    builder.append(it).append(" (").append(currentChatID++).append("), ")
                } else
                    builder.append(it).append(" (").append(reverseChatMap[it]).append("), ")
            }
            builder.setLength(builder.length - 2) // Remove the last ", ".
            builder.append('\n')
        } catch(e: Exception) {
            println("Error getting chats for ${protocol.name}. Stack Trace:\n${getStackTraceText(e)}")
        }
    }
    return builder.toString()
}

val dateTimeParser = Parser()

val defaultDuration = Duration.ofMinutes(45)!!
private val locations = HashMap<User, Pair<OffsetDateTime, String>>()

fun goingto(args: List<String>, chat: Chat, sender: User): String {
    val location = StringBuilder(args[0])
    val timeStr = StringBuilder()
    val durationStr = StringBuilder()
    var continueUntil = -1
    var timeGroups: List<DateGroup> = emptyList()
    var hasAtInOn = false
    var hasFor = false
    for (i in 1 until args.size) {
        if (continueUntil > i)
            continue
        when(args[i]) {
            "at", "on", "in" -> {
                if (hasAtInOn)
                    return "You can't put multiple times in!"
                hasAtInOn = true
                if (i == args.size - 1)
                    return "You can't just put \"at\" and not put a time after it!"
                for (i2 in i + 1 until args.size)
                    if (args[i2] == "for") {
                        continueUntil = i2
                        if (i2 == i + 1)
                            return "You can't just put \"at\" and not put a time after it!"
                        break
                    } else {
                        timeStr.append(args[i2]).append(' ')
                        continueUntil = continueUntil.coerceAtLeast(i2 + 1)
                    }
                timeStr.setLength(timeStr.length - 1)
                timeGroups = dateTimeParser.parse(timeStr.toString())
            }

            "for" -> {
                if (hasFor)
                    return "You can't put multiple durations in!"
                hasFor = true
                for (i2 in i + 1 until args.size)
                    if (args[i2] in setOf("at", "in", "on")) {
                        continueUntil = i2
                        if (i2 == i + 1)
                            return "You can't just put \"for\" and not put a time after it!"
                        break
                    } else {
                        durationStr.append(args[i2]).append(' ')
                        continueUntil = continueUntil.coerceAtLeast(i2 + 1)
                    }
                durationStr.setLength(durationStr.length - 1)
                val groups = dateTimeParser.parse(durationStr.toString())
                if (groups.size > 1 || groups[0].dates.size > 1)
                    return "You can't have the duration be more than one time!"
                else if (groups.isEmpty() || groups[0].dates.isEmpty())
                    return "You can't just put \"for\" and not put a time after it!"
            }

            else -> {
                location.append(' ').append(args[i])
            }
        }
    }

    val builder = StringBuilder()
    if (timeGroups.isNotEmpty()) {
        for (group in timeGroups) {
            if (group.isRecurring) {
                return "Sorry, the bot doesn't support recurring events."
            } else {
                group.dates.forEach {
                    builder.append(
                        CommandScheduler.schedule(
                            chat, sender, "goingto",
                            if (durationStr.isEmpty())
                                listOf(location.toString())
                            else
                                listOf(location.toString(), "for", durationStr.toString()),
                            it.toOffsetDatetime()
                        )
                    )
                }
                Settings.update()
            }
        }
    } else {
        return "${
            getUserName(
                chat,
                sender
            )
        } is going to $location${if (durationStr.isEmpty()) "" else " for $durationStr"}."
    }
    return if (builder.isNotEmpty()) builder.toString() else "No events scheduled."
}

fun target(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ").replace("%target", chat.protocol.getName(chat, user))
}

fun targetNick(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    if (chat.protocol !is HasNicknames)
        return "This protocol doesn't support nicknames!"
    val protocol = chat.protocol as HasNicknames
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ").replace("%target", protocol.getUserNickname(chat, user)!!)
}

fun commands(unused: List<String>, chat: Chat): String {
    val commandList = mutableListOf<String>()
    commands[chat.protocol]?.forEach { commandList.add(it.key) }
    commands[UniversalProtocol]?.forEach { commandList.add(it.key) }
    linkedChats[chat]?.forEach { linked -> commands[linked.protocol]?.forEach { commandList.add(it.key) } }
    return if (commandList.isNotEmpty()) commandList.joinToString(", ") else "No commands found."
}

fun aliases(unused: List<String>, chat: Chat): String {
    val aliasList = mutableListOf<String>()
    aliases[chat]?.forEach { aliasList.add(it.key) }
    aliases[UniversalChat]?.forEach { aliasList.add(it.key) }
    linkedChats[chat]?.forEach { linked -> aliases[linked]?.forEach { aliasList.add(it.key) } }
    return if (aliasList.isNotEmpty()) "Aliases: ${aliasList.joinToString(", ")}" else "No aliases found."
}

fun schedule(args: List<String>, chat: Chat, sender: User): String {
    if (args.size != 2)
        return "Expected 2 arguments, got ${args.size} argument${if (args.size != 1) "s" else ""}."
    val timeList = dateTimeParser.parse(args[0])
    val delimiter = commandDelimiters[chat] ?: defaultCommandDelimiter
    val command = (if (args[1].startsWith(delimiter)) "" else delimiter) + args[1]
    val commandData = getCommandData(chat, command, sender)
    if (commandData != null)
        for (group in timeList)
            for (time in group.dates)
                CommandScheduler.schedule(
                    chat,
                    sender,
                    commandData.command.name,
                    commandData.args,
                    time.toOffsetDatetime()
                )
    Settings.update()
    return "Scheduled \"$command\" to run in ${args[0]}."
}

/**
 * Gets all the currently scheduled events sorted by ID.
 */
fun events(unused: List<String>, chat: Chat): String {
    val commands = getCommands()
    if (commands.isEmpty())
        return "No events are currently scheduled."
    val builder = StringBuilder()
    addEventToBuilder(commands.sortedBy { it.time } as MutableList<ScheduledCommand>, chat, builder)
    return builder.toString()
}

/**
 * Gets all the currently scheduled events that were scheduled by [sender].
 */
private fun getUserEvents(sender: User): Map<User, MutableList<ScheduledCommand>> {
    val eventsList = getCommands(sender)
    val eventMap = HashMap<User, MutableList<ScheduledCommand>>()
    for (event in eventsList) {
        if (!eventMap.containsKey(event.sender))
            eventMap[event.sender] = mutableListOf()
        eventMap[event.sender]!!.add(event)
    }
    return eventMap
}

fun eventsFromUser(unused: List<String>, chat: Chat, sender: User): String {
    val eventMap = getUserEvents(sender)
    if (eventMap.isEmpty())
        return "No events are currently scheduled."
    val builder = StringBuilder("Your currently scheduled events:\n")
    builder.append("${getUserName(chat, sender)}:\n")
    if (sender in eventMap) {
        val events = eventMap[sender] ?: mutableListOf()
        events.sortBy { it.time }
        addEventToBuilder(events, chat, builder)
    }
    return builder.toString()
}

fun eventsByUser(unused: List<String>, chat: Chat, sender: User): String {
    val eventMap = getUserEvents(sender)
    if (eventMap.isEmpty())
        return "No events are currently scheduled."
    val builder = StringBuilder("Scheduled events by user:\n")
    for ((user, events) in eventMap) {
        events.sortBy { it.time }
        builder.append("${getUserName(chat, user)}:\n")
        addEventToBuilder(events, chat, builder)
    }
    return builder.toString()
}

private fun addEventToBuilder(
    events: MutableList<ScheduledCommand>,
    chat: Chat,
    builder: StringBuilder
) {
    for (event in events) {
        val id = event.id
        val time = formatTime(event.time)
        val commandDelimiter = commandDelimiters.getOrDefault(chat, defaultCommandDelimiter)
        val name = event.commandName
        val argsStr = event.args.joinToString(" ")
        builder.append("\t[$id] $time: \"$commandDelimiter$name $argsStr\"")
    }
}

fun unschedule(args: List<String>): String {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch(e: NumberFormatException) {
        return "${args[0]} is not an event ID!"
    }

    Settings.update()
    return if (CommandScheduler.unschedule(index))
        "Unscheduled event with index $index."
    else
        "No event with index $index found."
}

fun link(args: List<String>, chat: Chat): String {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch(e: NumberFormatException) {
        return "${args[0]} is not a chat ID!"
    }

    val chatToLink = chatMap[index]
    return if (chatToLink != null) {
        if (chat !in linkedChats)
            linkedChats[chat] = mutableSetOf()
        linkedChats[chat]!!.add(chatToLink)
        Settings.update()
        "${chatToLink.name} linked to ${chat.name}."
    } else
        "No chat with ID $index found."
}

fun unlink(args: List<String>, chat: Chat): String {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch(e: NumberFormatException) {
        return "${args[0]} is not a chat ID!"
    }
    val toUnlink = chatMap[index] ?: return "No chat with ID $index found."

    return when {
        chat !in linkedChats -> "There are no chats linked to this one!"
        linkedChats[chat]!!.remove(toUnlink) -> {
            if (linkedChats[chat]!!.isEmpty())
                linkedChats.remove(chat)
            Settings.update()
            "Removed ${toUnlink.name} from this chat's links."
        }

        else -> "That chat isn't linked to this one!"
    }
}

fun links(unused: List<String>, chat: Chat): String {
    return if (chat in linkedChats)
        "Linked chats: ${linkedChats[chat]!!.joinToString(", ") { c: Chat -> "$c (${reverseChatMap[c]})" }}"
    else
        "No chats are linked to this one."
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

fun setDelimiter(args: List<String>, chat: Chat): String = when {
    args.isEmpty() -> "You need to pass the new delimiter!"
    setCommandDelimiter(chat, args[0]) -> "Command delimiter set to \"${args[0]}\".".also { Settings.update() }
    else -> "\"${args[0]}\" is not a valid command delimiter!"
}

fun createTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name in timers)
        return "That timer already exists!"
    timers[name] = OffsetDateTime.now()
    Settings.update()
    return "New timer \"$name\" created."
}

fun resetTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name !in timers)
        return "That timer doesn't exist!"
    val oldVal = timers[name]
    timers[name] = OffsetDateTime.now()
    Settings.update()
    return "Timer reset. The time it was created or last time the timer was reset was ${formatTime(oldVal!!)}."
}

fun checkTimer(args: List<String>): String {
    if (args.isEmpty())
        return "A name has to be provided."
    val name = args.joinToString(" ")
    if (name !in timers)
        return "That timer doesn't exist!"
    val oldVal = timers[name]
    Settings.update()
    return "The time it was created or last time the timer was reset was ${formatTime(oldVal!!)}."
}

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
            "serverAlias (commandName) \"commandName [arguments...]\" (Command inside parentheses takes however many parameters that command takes)"
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
            UniversalProtocol, "goingto",
            listOf(
                ArgumentSpec("Location", ArgumentType.STRING),
                ArgumentSpec("Duration", ArgumentType.STRING, true),
                ArgumentSpec("Time", ArgumentType.STRING, true)
            ),
            ::goingto,
            "Tells the chat you're going somewhere for some time.",
            "goingto \"location\" [for (duration)] [at (time)/in (timedelta)/on (datetime)] (Note: Order does not matter with for/at/in/on)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "commands",
            listOf(),
            ::commands,
            "Lists all of the commands in this chat.",
            "commands (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "aliases",
            listOf(),
            ::aliases,
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
            ::eventsFromUser,
            "Lists all of the events you've made.",
            "events (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "allevents",
            listOf(),
            ::events,
            "Lists all of the events in chronological order.",
            "allevents (Takes no arguments)"
        )
    )
    registerCommand(
        Command.of(
            UniversalProtocol, "eventsbyuser",
            listOf(),
            ::eventsByUser,
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
            ::links,
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
            { -> Settings.toDTO().toString() },
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
