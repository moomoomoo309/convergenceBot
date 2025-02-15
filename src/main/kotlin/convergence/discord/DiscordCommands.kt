package convergence.discord

import convergence.Command
import convergence.CommandScheduler
import convergence.registerCommand
import convergence.syncedCalendars

fun registerDiscordCommands() {
    registerCommand(Command(
        DiscordProtocol,
        "syncCalendar",
        CalendarProcessor::syncCommand,
        "Sync a CalDAV calendar to discord events.",
        "syncCalendar (URL)"
    ))
    registerCommand(Command(
        DiscordProtocol,
        "resyncCalendar",
        {_, _, _ -> CommandScheduler.syncCalendars(); "Resyncing calendars..." },
        "Resyncs all calendars in all servers.",
        "resyncCalendar (Takes no parameters)"
    ))
    registerCommand(Command(
        DiscordProtocol,
        "syncedCalendars",
        {_, chat, _ -> syncedCalendars.filter {
                it.guildId == (chat as DiscordChat).server.guild.idLong
            }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
        },
        "Prints out all the synced calendars in this chat.",
        "syncedCalendars (Takes no parameters)"
    ))
}
