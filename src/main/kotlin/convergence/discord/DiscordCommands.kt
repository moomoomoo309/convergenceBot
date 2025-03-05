package convergence.discord

import convergence.*

fun registerDiscordCommands() {
    registerCommand(Command.of(
        DiscordProtocol,
        "syncCalendar",
        CalendarProcessor::syncCommand,
        "Sync a CalDAV calendar to discord events.",
        "syncCalendar (URL)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "resyncCalendar",
        { -> CommandScheduler.syncCalendars(); "Resyncing calendars..." },
        "Resyncs all calendars in all servers.",
        "resyncCalendar (Takes no parameters)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "unsyncCalendar",
        {args, chat -> val removed = syncedCalendars.remove(syncedCalendars.firstOrNull { it.guildId == (chat as DiscordChat).server.guild.idLong && it.calURL == args[0] }); if (removed) "Calendar removed." else "No calendar with URL ${args[0]} found." },
        "Removes a synced calendar.",
        "unsyncCalendar (URL)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "syncedCalendars",
        {_, chat -> syncedCalendars.filter {
                it.guildId == (chat as DiscordChat).server.guild.idLong
            }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
        },
        "Prints out all the synced calendars in this chat.",
        "syncedCalendars (Takes no parameters)"
    ))
}
