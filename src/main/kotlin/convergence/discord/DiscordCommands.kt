package convergence.discord

import convergence.*
import java.net.URI
import java.net.URISyntaxException

fun registerDiscordCommands() {
    registerCommand(Command.of(
        DiscordProtocol,
        "syncCalendar",
        listOf(ArgumentSpec("URL", ArgumentType.STRING)),
        CalendarProcessor::syncCommand,
        "Sync a CalDAV calendar to discord events.",
        "syncCalendar (URL)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "resyncCalendar",
        listOf(),
        { -> CommandScheduler.syncCalendars(); "Resyncing calendars..." },
        "Resyncs all calendars in all servers.",
        "resyncCalendar (Takes no parameters)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "unsyncCalendar",
        listOf(ArgumentSpec("URL", ArgumentType.STRING)),
        {args, chat -> val removed = syncedCalendars.remove(syncedCalendars.firstOrNull { it.guildId == (chat as DiscordChat).server.guild.idLong && it.calURL == args[0] }); if (removed) "Calendar removed." else "No calendar with URL ${args[0]} found." },
        "Removes a synced calendar.",
        "unsyncCalendar (URL)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "syncedCalendars",
        listOf(),
        {_, chat -> syncedCalendars.filter {
                it.guildId == (chat as DiscordChat).server.guild.idLong
            }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
        },
        "Prints out all the synced calendars in this chat.",
        "syncedCalendars (Takes no parameters)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "uploadImagesTo",
        listOf(ArgumentSpec("URL", ArgumentType.STRING)),
        { args: List<String>, chat: Chat ->
            if (args.isEmpty())
                return@of "A URL has to be provided."
            val url = try {
                URI(args[0])
            } catch(e: URISyntaxException) {
                e.printStackTrace()
                return@of "\"${args[0]}\" is not a valid URL."
            }
            imageUploadChannels[chat] = url
            Settings.update()
            "Images will now be uploaded to $url."
        },
        "Sets all images in this channel from here on out to be uploaded to the provided WebDAV URL.",
        "uploadImagesTo (URL)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "stopUploadingImages",
        listOf(),
        { _, chat: Chat ->
            imageUploadChannels.remove(chat)
            Settings.update()
            "Images will no longer be uploaded."
        },
        "Stops images in this channel from being uploaded anywhere.",
        "stopUploadingImages (takes no arguments)"
    ))
}
