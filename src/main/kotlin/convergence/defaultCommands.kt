@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import com.joestelmach.natty.DateGroup
import com.joestelmach.natty.Parser
import kotlinx.serialization.ImplicitReflectionSerializer
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.ceil
import kotlin.math.nextUp

class UnregisteredChat: Exception()

@Suppress("NOTHING_TO_INLINE") // It's inlined so it won't show up in the stack trace, not for performance.
inline fun unregisteredChat(chat: Chat) {
    try {
        throw UnregisteredChat()
    } catch (e: UnregisteredChat) {
        val writer = StringWriter()
        e.printStackTrace(PrintWriter(writer))
        sendMessage(chat, "Bot error: Chat either is missing a protocol, or its BaseInterface is not" +
                " registered to that protocol.\nStack Trace:\n$writer")
        writer.close()
    }
}

fun getUserFromName(chat: Chat, name: String): User? {
    var alternateOption: User? = null
    val baseInterface = baseInterfaceMap[chat.protocol]!!
    for (user in baseInterface.getUsers(chat)) {
        val currentName = baseInterface.getName(chat, user)
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
        return if (user != null) baseInterfaceMap[chat.protocol]!!.getName(chat, user) else null
    unregisteredChat(chat)
    return null
}


const val commandsPerPage = 10
fun help(args: List<String>, sender: User): String? {
    val chat = sender.chat
    val pageOrCommand = if (args.isEmpty()) 1 else args[0].toIntOrNull() ?: args[0]
    val numPages = ceil(sortedHelpText.size.toDouble() / commandsPerPage).nextUp().toInt()
    return when (pageOrCommand) {
        is Int -> {
            val helpText = StringBuilder("Help page $pageOrCommand/$numPages:\n")
            for (i in 0..commandsPerPage) {
                val index = i + (pageOrCommand - 1) * (commandsPerPage + 1)
                if (index >= sortedHelpText.size)
                    break
                val currentCommand = sortedHelpText[index]
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

fun echo(args: List<String>, sender: User): String? = args.joinToString(" ")

fun ping(args: List<String>, sender: User): String? = "Pong!"

fun addAlias(args: List<String>, sender: User): String? {
    val chat = sender.chat
    val commandDelimiter = commandDelimiters.getOrDefault(chat, defaultCommandDelimiter)
    val command = parseCommand(commandDelimiter + args[1], commandDelimiter, chat)
            ?: return "Alias does not refer to a valid command!"
    if (!registerAlias(Alias(chat, args[0], command.command, command.args, args[1], command.command.syntaxText)))
        return ""
    return "Alias \"${args[0]}\" registered to \"${args[1]}\"."
}

fun removeAlias(args: List<String>, sender: User): String? {
    val chat = sender.chat
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

fun me(args: List<String>, sender: User): String? {
    return "*${getUserName(sender)} ${args.joinToString(" ")}"
}

fun chats(args: List<String>, sender: User): String? {
    val builder = StringBuilder()
    for (protocol in protocols) {
        builder.append("${protocol.name}\n\t")
        for ((id, chat) in chatMap)
            builder.append(chat.name).append(" (").append(id).append("), ")
        builder.setLength(builder.length - 2) // Remove the last ", ".
        builder.append('\n')
    }
    return builder.toString()
}

val dateTimeParser = Parser()

fun dateToOffsetDateTime(d: Date, tz: ZoneId? = null): OffsetDateTime {
    return OffsetDateTime.ofInstant(d.toInstant(), tz ?: ZoneId.systemDefault())
}

@ImplicitReflectionSerializer
private fun scheduleLoc(groups: List<DateGroup>, sender: User, location: String, durationStr: String,
                        duration: Duration, time: String): String {
    val thisCommand = commands[UniversalChat]!!["goingto"]!!
    val builder = StringBuilder()
    for (group in groups) {
        if (group.isRecurring) {
            return "Sorry, the bot doesn't support recurring events."
        } else {
            group.dates.forEach {
                builder.append(SchedulerThread.schedule(sender, CommandData(thisCommand,
                        if (durationStr.isEmpty())
                            listOf(location)
                        else
                            listOf(location, "for", durationStr)), dateToOffsetDateTime(it))
                        ?: "No events scheduled.")
            }
        }
    }
    return if (builder.isNotEmpty()) builder.toString() else "No events scheduled."
}

val defaultDuration = Duration.ofMinutes(45)!!
private val locations = HashMap<User, Pair<OffsetDateTime, String>>()
@ImplicitReflectionSerializer
fun setLocation(args: List<String>, sender: User): String? {
    val location = StringBuilder(args[0])
    val timeStr = StringBuilder()
    val durationStr = StringBuilder()
    var duration: Duration = defaultDuration
    var continueUntil = -1
    var timeGroups: List<DateGroup> = emptyList()
    var hasAtInOn = false
    var hasFor = false
    findInOrAt@ for (i in 1 until args.size) {
        if (continueUntil > i)
            continue
        when (args[i]) {
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
                    } else
                        timeStr.append(args[i2]).append(' ')
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
                    } else
                        durationStr.append(args[i2]).append(' ')
                durationStr.setLength(durationStr.length - 1)
                val groups = dateTimeParser.parse(durationStr.toString())
                if (groups.size > 1 || groups[0].dates.size > 1)
                    return "You can't have the duration be more than one time!"
                else if (groups.size == 0 || groups[0].dates.size == 0)
                    return "You can't just put \"for\" and not put a time after it!"
                duration = Duration.between(OffsetDateTime.now(), dateToOffsetDateTime(groups[0].dates[0]))
            }
            else -> {
                location.append(' ').append(args[i])
            }
        }
    }

    return scheduleLoc(timeGroups, sender, location.toString(), durationStr.toString(), duration, timeStr.toString())
}

fun target(args: List<String>, sender: User): String? {
    val chat = sender.chat
    val baseInterface = baseInterfaceMap[chat.protocol]!!
    val user = getUserFromName(chat, args[args.size - 1])
            ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, -1).joinToString(" ").replace("%target", baseInterface.getName(chat, user))
}

fun commands(args: List<String>, sender: User): String? {
    val commandList = mutableListOf<String>()
    commands[sender.chat]?.forEach { commandList.add(it.key) }
    commands[UniversalChat]?.forEach { commandList.add(it.key) }
    linkedChats[sender.chat]?.forEach { chat -> commands[chat]?.forEach { commandList.add(it.key) } }
    return if (commandList.isNotEmpty()) commandList.joinToString(", ") else "No commands found."
}

fun aliases(args: List<String>, sender: User): String? {
    val aliasList = mutableListOf<String>()
    aliases[sender.chat]?.forEach { aliasList.add(it.key) }
    aliases[UniversalChat]?.forEach { aliasList.add(it.key) }
    linkedChats[sender.chat]?.forEach { chat -> aliases[chat]?.forEach { aliasList.add(it.key) } }
    return if (aliasList.isNotEmpty()) aliasList.joinToString(", ") else "No aliases found."
}

@ImplicitReflectionSerializer
fun schedule(args: List<String>, sender: User): String? {
    if (args.size != 2)
        return "Expected 2 arguments, got ${args.size}."
    val timeList = dateTimeParser.parse(args[0])
    val command = args[1]
    val commandData = getCommandData(command, sender)
    if (commandData != null)
        for (group in timeList)
            for (time in group.dates)
                SchedulerThread.schedule(sender, commandData, dateToOffsetDateTime(time))
    return "Scheduled \"$command\" to run in ${args[0]}."
}

/**
 * Gets all of the currently scheduled events sorted by ID.
 */
@ImplicitReflectionSerializer
fun events(args: List<String>, sender: User): String? {
    val strs = SchedulerThread.getCommandStrings(sender, true)
    if (strs.isEmpty())
        return "No events are currently scheduled."
    return strs.joinToString("\n")
}

/**
 * Gets all of the currently scheduled events that were scheduled by [sender].
 */
@ImplicitReflectionSerializer
private fun getUserEvents(sender: User): Map<User, MutableList<ScheduledCommand>> {
    val eventsList = SchedulerThread.getCommands(sender)
    val eventMap = HashMap<User, MutableList<ScheduledCommand>>()
    for (event in eventsList) {
        if (!eventMap.containsKey(event.sender))
            eventMap[event.sender] = mutableListOf()
        eventMap[event.sender]!!.add(event)
    }
    return eventMap
}

@ImplicitReflectionSerializer
fun eventsFromUser(args: List<String>, sender: User): String? {
    val eventMap = getUserEvents(sender)
    if (eventMap.isEmpty())
        return "No events are currently scheduled."
    val builder = StringBuilder("Your currently scheduled events:\n")
    builder.append("${getUserName(sender)}:\n")
    if (sender in eventMap) {
        val events = eventMap[sender] ?: mutableListOf()
        events.sortBy { it.time }
        for (event in events)
            builder.append("\t[${event.id}] ${formatTime(event.time)}: \"${commandDelimiters[sender.chat]}${event.commandData.command.name} ${event.commandData.args.joinToString(" ")}\"")
    }
    return builder.toString()
}

@ImplicitReflectionSerializer
fun eventsByUser(args: List<String>, sender: User): String? {
    val eventMap = getUserEvents(sender)
    if (eventMap.isEmpty())
        return "No events are currently scheduled."
    val builder = StringBuilder("Scheduled events by user:\n")
    for ((user, events) in eventMap) {
        events.sortBy { it.time }
        builder.append("${getUserName(user)}:\n")
        for (event in events)
            builder.append("\t[${event.id}] ${formatTime(event.time)}: \"${commandDelimiters[sender.chat]}${event.commandData.command.name} ${event.commandData.args.joinToString(" ")}\"")
    }
    return builder.toString()
}

@ImplicitReflectionSerializer
fun unschedule(args: List<String>, sender: User): String? {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch (e: NumberFormatException) {
        return "${args[0]} is not an event ID!"
    }

    return if (SchedulerThread.unschedule(sender, index))
        "Unscheduled event with index $index."
    else
        "No event with index $index found."
}

fun link(args: List<String>, sender: User): String? {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch (e: NumberFormatException) {
        return "${args[0]} is not a chat ID!"
    }

    val chatToLink = chatMap[index]
    return if (chatToLink != null) {
        if (sender.chat !in linkedChats)
            linkedChats[sender.chat] = HashSet()
        linkedChats[sender.chat]!!.add(chatToLink)
        "${chatToLink.name} linked to ${sender.chat.name}."
    } else
        "No chat with ID $index found."
}

fun unlink(args: List<String>, sender: User): String? {
    val index: Int = try {
        Integer.parseInt(args[0])
    } catch (e: NumberFormatException) {
        return "${args[0]} is not a chat ID!"
    }
    val chat = chatMap[index]
    return if (chat != null)
        if (sender.chat in linkedChats)
            if (linkedChats[sender.chat]!!.remove(chat))
                "Removed ${chat.name} from this chat's links."
            else
                "That chat isn't linked to this one!"
        else
            "There are no chats linked to this one!"
    else
        "No chat with ID $index found."
}

fun links(args: List<String>, sender: User): String? {
    val chat = sender.chat
    return if (chat in linkedChats)
        "Linked chats: ${linkedChats[chat]!!.joinToString(", ")}"
    else
        "No chats are linked to this one."
}

fun setDelimiter(args: List<String>, sender: User): String? = when {
    args.isEmpty() -> "You need to pass the new delimiter!"
    setCommandDelimiter(sender.chat, args[0]) -> "Command delimiter set to \"${args[0]}\"."
    else -> "\"${args[0]}\" is not a valid command delimiter!"
}

@ImplicitReflectionSerializer
fun registerDefaultCommands() {
    registerCommand(Command(UniversalChat, "help", ::help,
            "Provides a paginated list of commands and their syntax, or specific help on a single command.",
            "help [command] or help [page number]"))
    registerCommand(Command(UniversalChat, "echo", ::echo,
            "Replies with the string passed to it.",
            "echo [message...] (All arguments are appended to each other with spaces)"))
    registerCommand(Command(UniversalChat, "ping", ::ping,
            "Replies with \"Pong!\".",
            "ping (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "alias", ::addAlias,
            "Registers an alias to an existing command.",
            "alias (commandName) \"commandName [arguments...]\" (Command inside parentheses takes however many parameters that command takes)"))
    registerCommand(Command(UniversalChat, "me", ::me,
            "Replied \"*(username) (message)\" e.g. \"*Gian Laput is French.\"",
            "me [message...] (All arguments are appended to each other with spaces)"))
    registerCommand(Command(UniversalChat, "chats", ::chats,
            "Lists all chats the bot knows of by name.",
            "chats (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "goingto", ::setLocation,
            "Tells the chat you're going somewhere for some time.",
            "goingto \"location\" [for (duration)] [at (time)/in (timedelta)/on (datetime)] (Note: Order does not matter with for/at/in/on)"))
    registerCommand(Command(UniversalChat, "commands", ::commands,
            "Lists all of the commands in this chat.",
            "commands (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "aliases", ::aliases,
            "Lists all of the aliases in this chat.",
            "aliases (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "schedule", ::schedule,
            "Schedules a command to run later.",
            "schedule \"time\" \"command (with delimiter and arguments)\""))
    registerCommand(Command(UniversalChat, "unschedule", ::unschedule,
            "Unschedules a command, so it will not be run later. The ID can be obtained from the events command.",
            "unschedule (ID)"))
    registerCommand(Command(UniversalChat, "events", ::eventsFromUser,
            "Lists all of the events you've made.",
            "events (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "allevents", ::events,
            "Lists all of the events in chronological order.",
            "allevents (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "eventsbyuser", ::eventsByUser,
            "Lists all events by user, then in chronological order.",
            "eventsbyuser (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "link", ::link,
            "Links a chat to this one. The ID can be obtained from the chats command.",
            "link (ID)"))
    registerCommand(Command(UniversalChat, "unlink", ::unlink,
            "Unlinks a chat from this one. The ID can be obtained from the chats command.",
            "unlink (ID)"))
    registerCommand(Command(UniversalChat, "links", ::links,
            "Lists all of the chats linked to this one.",
            "links (Takes no arguments)"))
    registerCommand(Command(UniversalChat, "setdelimiter", ::setDelimiter,
            "Changes the command delimiter of the current chat (default is !)",
            "setdelimiter (New delimiter)"))
}

