package convergence.discord

import convergence.*
import java.net.URI
import java.net.URISyntaxException

val emojiRegex = Regex(":[a-zA-Z0-9_]+:|(?:[\uD83E\uDD00-\uD83E\uDDFF]|" +
            "[☀-⛿]\uFE0F?|[✀-➿]\uFE0F?|Ⓜ\uFE0F?|" +
            "[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}|" +
            "[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?|" +
            "[#*0-9]\uFE0F?⃣|[↔-↙↩-↪]\uFE0F?|[⬅-⬇⬛⬜⭐⭕]\uFE0F?|" +
            "[⤴⤵]\uFE0F?|[〰〽]\uFE0F?|[㊗㊙]\uFE0F?|" +
            "[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?|" +
            "[‼⁉]\uFE0F?|[▪▫▶◀◻-◾]\uFE0F?|" +
            "[©®]\uFE0F?|[™ℹ]\uFE0F?|\uD83C\uDCCF\uFE0F?|" +
            "[⌚⌛⌨⏏⏩-⏳⏸-⏺]\uFE0F?)+")
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
            if (!emoji.matches(emojiRegex))
                return@cmd "Emoji must be a valid unicode or discord emoji."
            val threshold = args[1].toIntOrNull()
            if (threshold == null || threshold <= 0)
                return@cmd "Threshold must be a positive integer."
            if (chat.server !in reactServers)
                reactServers[chat.server] = ReactConfig(chat, mutableMapOf(emoji to threshold))
            else
                reactServers[chat.server]?.emojis?.put(emoji, threshold) ?: "Could not find server in map."
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
}
