package convergence.testPlugins.discordPlugin

import com.squareup.moshi.*
import convergence.fromJson
import convergence.getReadStringOrThrow
import convergence.toJson

object DiscordMessageHistoryAdapter: JsonAdapter<DiscordMessageHistory>() {
    @FromJson
    override fun fromJson(reader: JsonReader): DiscordMessageHistory? {
        val readStringOrThrow = getReadStringOrThrow(reader, "DiscordMessageHistory")
        if (readStringOrThrow("type") != "DiscordMessageHistory")
            return null
        val msgChannelId = readStringOrThrow("msgChannel")
        val msgChannel = jda.getTextChannelById(msgChannelId) ?: return null
        val msgId = readStringOrThrow("msg")
        val msg = msgChannel.retrieveMessageById(msgId).complete()
        val id = readStringOrThrow("id")
        return DiscordMessageHistory(msg, id.toLong())
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DiscordMessageHistory?) {
        value?.let {
            writer.name("type")
            writer.value("DiscordMessageHistory")
            writer.name("msgChannel")
            writer.value(it.msg.channel.id)
            writer.name("msg")
            writer.value(it.msg.id)
            writer.name("id")
            writer.value(it.id)
        }
    }
}

object DiscordEmojiAdapter: JsonAdapter<DiscordEmoji>() {
    @FromJson
    override fun fromJson(reader: JsonReader): DiscordEmoji? {
        val readStringOrThrow = getReadStringOrThrow(reader, "DiscordEmoji")
        if (readStringOrThrow("type") != "DiscordEmoji")
            return null
        val url = readStringOrThrow("url")
        val name = readStringOrThrow("name")
        val emoji = readStringOrThrow("emoji")
        val guild = readStringOrThrow("guild")
        val id = readStringOrThrow("id")
        val emote = jda.getGuildById(guild)?.getEmojiById(emoji) ?: return null
        return DiscordEmoji(url, name, emote, id.toLong())
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DiscordEmoji?) {
        value?.let {
            writer.name("type")
            writer.value("DiscordEmoji")
            writer.name("url")
            writer.value(it.url)
            writer.name("name")
            writer.value(it.name)
            writer.name("emoji")
            writer.value(it.emoji.id)
            writer.name("guild")
            writer.value(it.emoji.guild!!.id)
            writer.name("id")
            writer.value(it.id)
        }
    }
}

object DiscordUserAdapter: JsonAdapter<DiscordUser>() {
    @FromJson
    override fun fromJson(reader: JsonReader): DiscordUser? {
        var readStringOrThrow = getReadStringOrThrow(reader, "DiscordUser")
        if (readStringOrThrow("type") != "DiscordUser")
            return null
        if (reader.nextName() != "chat")
            throw JsonDataException("The 2nd field should be chat in DiscordUser!")
        val chatJson = when (val json = reader.readJsonValue()) {
            is String -> _moshi.toJson(json)
            is Number -> _moshi.toJson(json)
            is Boolean -> _moshi.toJson(json)
            is List<*> -> _moshi.toJson(json)
            is Map<*, *> -> _moshi.toJson(json)
            else -> return null
        }
        val chat = _moshi.fromJson<DiscordChat>(chatJson)
        readStringOrThrow = getReadStringOrThrow(reader, "DiscordUser", 4)
        val name = readStringOrThrow("name")
        val id = readStringOrThrow("id")
        val authorId = readStringOrThrow("author")
        val author = jda.getUserById(authorId) ?: return null
        return DiscordUser(chat, name, id.toLong(), author)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DiscordUser?) {
        value?.let {
            writer.name("type")
            writer.value("DiscordUser")
            writer.name("chat")
            writer.value(it.chat.toJson())
            writer.name("name")
            writer.value(it.name)
            writer.name("id")
            writer.value(it.id)
            writer.name("author")
            writer.value(it.author.idLong)
        }
    }
}

object DiscordChatAdapter: JsonAdapter<DiscordChat>() {
    @FromJson
    override fun fromJson(reader: JsonReader): DiscordChat? {
        val readStringOrThrow = getReadStringOrThrow(reader, "DiscordChat")
        if (readStringOrThrow("type") != "DiscordChat")
            return null
        val name = readStringOrThrow("name")
        val id = readStringOrThrow("id")
        val channelId = readStringOrThrow("channel")
        val channel = jda.getTextChannelById(channelId) ?: return null
        return DiscordChat(name, id.toLong(), channel)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DiscordChat?) {
        value?.let {
            writer.name("type")
            writer.value("DiscordChat")
            writer.name("name")
            writer.value(it.name)
            writer.name("id")
            writer.value(it.id)
            writer.name("channel")
            writer.value(it.channel.idLong)
        }
    }
}
