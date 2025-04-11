package convergence.discord

import com.sigpwned.emoji4j.core.Grapheme
import com.sigpwned.emoji4j.core.GraphemeMatcher
import convergence.*
import java.net.URI
import java.net.URISyntaxException

val discordEmojiRegex = Regex("^:[a-zA-Z0-9_]+:$")
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
    registerCommand(Command.of(
        DiscordProtocol,
        "registerReactChannel",
        listOf(ArgumentSpec("emoji", ArgumentType.STRING), ArgumentSpec("threshold", ArgumentType.INTEGER)),
        cmd@{ args, chat ->
            if (chat !is DiscordChat)
                return@cmd "This command can only be run on discord."
            if (args.size != 2)
                return@cmd "2 args required: emoji and threshold."
            val emoji = args[0]
            val matcher = GraphemeMatcher(emoji)
            val isUnicodeEmoji = matcher.matches() && matcher.grapheme().type == Grapheme.Type.EMOJI
            val isDiscordEmoji = discordEmojiRegex.matches(emoji)
            if (!isUnicodeEmoji && !isDiscordEmoji)
                return@cmd "Emoji must be a valid unicode or discord emoji."
            val threshold = args[1].toIntOrNull()
            if (threshold == null || threshold <= 0)
                return@cmd "Threshold must be a positive integer."
            reactServers.getOrPut(chat.server) { mutableListOf(ReactConfig(chat, mutableMapOf())) }
                .first { it.destination == chat }.emojis[emoji] = threshold
            Settings.update()
            "Registered messages to be forwarded to this channel if they are reacted with $emoji $threshold times or more."
        },
        "Registers messages to be forwarded to this channel if they are reacted with emoji threshold times or more.",
        "registerReactChannel (emoji) (threshold)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "removeReactChannel",
        listOf(),
        cmd@{ _, chat ->
            if (chat !is DiscordChat)
                return@cmd "This command can only be run on discord."
            reactServers.remove(chat.server)
            Settings.update()
            "Messages will no longer be forwarded to this channel based on reactions."
        },
        "Removes messages being forwarded to this channel based on reactions.",
        "removeReactChannel (takes no arguments)"
    ))
    registerCommand(Command.of(
        DiscordProtocol,
        "reactChannels",
        listOf(),
        cmd@{ _, chat ->
            if (chat !is DiscordChat)
                return@cmd "This command can only be run on discord."
            "Reactions that will be sent to this channel: ${
                reactServers[chat.server]?.firstOrNull { it.destination == chat }
                    ?.emojis?.toList()?.joinToString(", ") { (emoji, threshold) ->
                        "$emoji: $threshold"
                    } ?: "None"
            }"
        },
        "Lists all react channels.",
        "reactChannels (takes no arguments)"
    ))
}
