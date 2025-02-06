package convergence.discord

import convergence.Command
import convergence.registerCommand

fun registerDiscordCommands() {
    registerCommand(Command(
        DiscordProtocol,
        "syncCalendar",
        CalendarProcessor::syncCommand,
        "Sync a CalDAV calendar to discord events.",
        "syncCalendar (URL)"
    ))
}
