package convergence.commands

import convergence.*
import convergence.command.parseCommand
import convergence.model.Chat
import convergence.model.User
import org.natty.Parser

val dateTimeParser = Parser()

fun schedule(args: List<String>, chat: Chat, sender: User): String {
    if (args.size != 2)
        return "Expected 2 arguments, got ${args.size} argument${if (args.size != 1) "s" else ""}."
    val timeList = dateTimeParser.parse(args[0])
    val delimiter = settings.commandDelimiters[chat] ?: DEFAULT_COMMAND_DELIMITER
    val command = (if (args[1].startsWith(delimiter)) "" else delimiter) + args[1]
    val commandWithArgs = parseCommand(chat, command, sender)
        ?: return "\"$command\" does not refer to a valid command."
    for (group in timeList)
        for (time in group.dates)
            Scheduler.schedule(
                chat,
                sender,
                commandWithArgs.command.name,
                commandWithArgs.args,
                time.toOffsetDatetime()
            )
    updateSettings()
    return "Scheduled \"$command\" to run in ${args[0]}."
}

/**
 * Gets all the currently scheduled events sorted by ID.
 */
fun events(chat: Chat): String {
    val commands = Scheduler.getCommands()
    if (commands.isEmpty())
        return "No events are currently scheduled."
    return buildString { addEventToBuilder(commands.sortedBy { it.time }, chat, this) }
}

/**
 * Gets all the currently scheduled events that were scheduled by [sender].
 */
private fun getUserEvents(sender: User): Map<User, MutableList<ScheduledCommand>> {
    val eventsList = Scheduler.getCommands(sender)
    val eventMap = HashMap<User, MutableList<ScheduledCommand>>()
    for (event in eventsList) {
        eventMap.getOrPut(event.sender) { mutableListOf() }.add(event)
    }
    return eventMap
}

fun eventsFromUser(chat: Chat, sender: User): String {
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

fun eventsByUser(chat: Chat, sender: User): String {
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
    events: List<ScheduledCommand>,
    chat: Chat,
    builder: StringBuilder
) {
    for (event in events) {
        val id = event.id
        val time = formatTime(event.time)
        val commandDelimiter = settings.commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER)
        val name = event.commandName
        val argsStr = event.args.joinToString(" ")
        builder.append("\t[$id] $time (${event.time}): \"$commandDelimiter$name $argsStr\"\n")
    }
}

fun unschedule(args: List<String>): String {
    val index = args[0].toIntOrNull() ?: return "${args[0]} is not an event ID!"

    updateSettings()
    return if (Scheduler.unschedule(index))
        "Unscheduled event with index $index."
    else
        "No event with index $index found."
}
