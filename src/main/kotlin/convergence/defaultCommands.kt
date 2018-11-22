@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import com.joestelmach.natty.DateGroup
import com.joestelmach.natty.Parser
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.StringBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
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
                "registered to that protocol.\nStack Trace:\n$writer")
        writer.close()
    }
}

fun getUserFromName(chat: Chat, name: String): User? {
    var alternateOption: User? = null
    val baseInterface = chat.protocol.baseInterface
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
        return if (user != null) chat.protocol.baseInterface.getName(chat, user) else null
    unregisteredChat(chat)
    return null
}


const val commandsPerPage = 10
fun help(chat: Chat, args: List<String>, sender: User): String? {
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

fun echo(chat: Chat, args: List<String>, sender: User): String? {
    return args.joinToString(" ")
}

fun ping(chat: Chat, args: List<String>, sender: User): String? {
    return "Pong!"
}

fun addAlias(chat: Chat, args: List<String>, sender: User): String? {
    val commandDelimiter = commandDelimiters.getOrDefault(chat, defaultCommandDelimiter)
    val command = parseCommand(commandDelimiter + args[1], commandDelimiter, chat)
            ?: return "Alias does not refer to a valid command!"
    if (!registerAlias(chat, Alias(args[0], command.command, command.args, command.command.helpText, command.command.syntaxText)))
        return ""
    return "Alias \"${args[0]}\" registered to \"${args[1]}\"."
}

fun removeAlias(chat: Chat, args: List<String>, sender: User): String? {
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

fun me(chat: Chat, args: List<String>, sender: User): String? {
    return "*${getUserName(chat, sender)} ${args.joinToString("")}"
}

fun chats(unused: Chat, args: List<String>, sender: User): String? {
    val builder = StringBuilder()
    for (protocol in protocols) {
        builder.append("${protocol.name}\n\t")
        for (chat in protocol.baseInterface.getChats())
            builder.append(chat.name).append(", ")
        builder.setLength(builder.length - 2) // Remove the last ", ".
        builder.append('\n')
    }
    return builder.toString()
}

val dateTimeParser = Parser()

fun dateToLocalDateTime(d: Date, tz: ZoneId? = null): LocalDateTime {
    return LocalDateTime.ofInstant(d.toInstant(), tz ?: ZoneId.systemDefault())
}

private fun scheduleLoc(groups: List<DateGroup>, chat: Chat, location: String, durationStr: String,
                        duration: Duration, time: String): String {
    val thisCommand = commands[UniversalChat]!!["goingto"]!!
    for (group in groups) {
        if (group.isRecurring) {
            return "Sorry, the bot doesn't support recurring events."
        } else {
            group.dates.forEach {
                schedule(chat, CommandData(thisCommand,
                        if (durationStr.isEmpty())
                            listOf(location)
                        else
                            listOf(location, "for", durationStr)),
                        dateToLocalDateTime(it))
            }
        }
    }
    return ""
}

val defaultDuration = Duration.ofMinutes(45)!!
private val locations = HashMap<User, Pair<LocalDateTime, String>>()
fun setLocation(chat: Chat, args: List<String>, sender: User): String? {
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
                duration = Duration.between(LocalDateTime.now(), dateToLocalDateTime(groups[0].dates[0]))
            }
            else -> {
                location.append(' ').append(args[i])
            }
        }
    }

    return scheduleLoc(timeGroups, chat, location.toString(), durationStr.toString(), duration, timeStr.toString())
}

fun target(chat: Chat, args: List<String>, sender: User): String? {
    //TODO: Check if -1 works for subList
    val baseInterface = chat.protocol.baseInterface
    val user = getUserFromName(chat, args[args.size - 1]) ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, -1).joinToString(" ").replace("%target", baseInterface.getName(chat, user))
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
    registerCommand(UniversalChat, Command("goingto", ::setLocation,
            "Tells the chat you're going somewhere for some time.",
            "goingto \"location\" [for (duration)] [at (time)/in (timedelta)/on (datetime)] (Order does not matter with for/at/in/on)"))

}

