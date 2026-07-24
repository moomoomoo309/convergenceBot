package convergence

/**
 * Commands related to scheduling and event management.
 * Receives dependencies via constructor injection (Koin).
 */
class ScheduleCommands(
    private val scheduler: SchedulerThread,
    private val settings: Settings,
    private val messaging: MessagingService,
    private val commandParser: CommandParserService,
    private val commandRegistry: CommandRegistryService
) {
    fun schedule(args: List<String>, chat: Chat, sender: User): String {
        if (args.size != 2)
            return "Expected 2 arguments, got ${args.size} argument${if (args.size != 1) "s" else ""}."
        val timeList = dateTimeParser.parse(args[0])
        val delimiter = settings.commandDelimiters[chat] ?: DEFAULT_COMMAND_DELIMITER
        val command = (if (args[1].startsWith(delimiter)) "" else delimiter) + args[1]
        val commandWithArgs = commandParser.parseSafe(chat, command, sender, commandRegistry::getCommand)
            ?: return "\"$command\" does not refer to a valid command."
        for (group in timeList)
            for (time in group.dates)
                scheduler.schedule(
                    chat,
                    sender,
                    commandWithArgs.command.name,
                    commandWithArgs.args,
                    time.toOffsetDatetime()
                )
        updateSettings()
        return "Scheduled \"$command\" to run in ${args[0]}."
    }

    fun unschedule(args: List<String>): String {
        val index = args[0].toIntOrNull() ?: return "${args[0]} is not an event ID!"

        return if (scheduler.unschedule(index)) {
            updateSettings()
            "Unscheduled event with index $index."
        } else
            "No event with index $index found."
    }

    fun events(chat: Chat): String {
        val commands = scheduler.getCommands()
        if (commands.isEmpty())
            return "No events are currently scheduled."
        return buildString { addEventToBuilder(commands.sortedBy { it.time }, chat, this) }
    }

    fun eventsFromUser(chat: Chat, sender: User): String {
        val eventMap = getUserEvents(sender)
        if (eventMap.isEmpty())
            return "No events are currently scheduled."
        val builder = StringBuilder("Your currently scheduled events:\n")
        builder.append("${messaging.getUserName(chat, sender)}:\n")
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
            builder.append("${messaging.getUserName(chat, user)}:\n")
            addEventToBuilder(events, chat, builder)
        }
        return builder.toString()
    }

    private fun getUserEvents(sender: User): Map<User, MutableList<ScheduledCommand>> {
        val eventsList = scheduler.getCommands(sender)
        val eventMap = HashMap<User, MutableList<ScheduledCommand>>()
        for (event in eventsList) {
            eventMap.getOrPut(event.sender) { mutableListOf() }.add(event)
        }
        return eventMap
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
}
