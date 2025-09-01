package convergence.discord

import convergence.*
import java.net.URI
import java.net.URISyntaxException

val discordEmojiRegex = Regex("^<a?:[a-zA-Z0-9_-]{1,100}:[0-9]{1,20}>$")

private fun registerReactChannel(args: List<String>, chat: Chat): String {
    if (chat !is DiscordChat)
        return "This command can only be run on discord."
    if (args.size != 2)
        return "2 args required: emoji and threshold."
    val emoji = args[0]
    val isUnicodeEmoji = emoji.toEmoji() != null
    val isDiscordEmoji = discordEmojiRegex.matches(emoji)
    if (!isUnicodeEmoji && !isDiscordEmoji)
        return "Emoji must be a valid unicode or discord emoji."
    val threshold = args[1].toIntOrNull()
    if (threshold == null || threshold <= 0)
        return "Threshold must be a positive integer."
    val reactConfigs = reactServers.getOrPut(chat.server) {
        mutableListOf(ReactConfig(chat, mutableMapOf()))
    }
    var reactConfig = reactConfigs.firstOrNull {
        it.destination == chat
    }
    if (reactConfig == null) {
        reactConfig = ReactConfig(chat, mutableMapOf())
        reactConfigs.add(reactConfig)
    }
    reactConfig.emojis[emoji] = threshold
    Settings.update()
    return "Registered messages to be forwarded to this channel " +
            "if they are reacted with $emoji $threshold times or more."
}

private fun uploadImagesTo(args: List<String>, chat: Chat): String {
    if (args.isEmpty())
        return "A URL has to be provided."
    val url = try {
        URI(args[0])
    } catch(e: URISyntaxException) {
        discordLogger.error("Could not parse URL! Exception: ", e)
        return "\"${args[0]}\" is not a valid URL."
    }
    imageUploadChannels[chat] = url
    Settings.update()
    return "Images will now be uploaded to $url."
}

@Suppress("LongMethod")
fun registerDiscordCommands() {
    registerCommand(Command.of(
        DiscordProtocol,
        "uploadImagesTo",
        listOf(ArgumentSpec("URL", ArgumentType.STRING)),
        ::uploadImagesTo,
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
        ::registerReactChannel,
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
        "Lists all reactions that may cause messages to be forwarded to this channel.",
        "reactChannels (takes no arguments)"
    ))
}
