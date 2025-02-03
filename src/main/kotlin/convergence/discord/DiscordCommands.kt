package convergence.discord

import convergence.Command
import convergence.registerCommand

fun registerDiscordCommands() {
    registerCommand(Command(
        DiscordProtocol,
        "syncCalendar",
        {_,_,_-> ""},
        "Sync a CalDAV calendar to discord events.",
        "syncCalendar (URL)"
    ))
}
