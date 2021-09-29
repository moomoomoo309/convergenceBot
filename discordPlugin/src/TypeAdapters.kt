package convergence.testPlugins.discordPlugin

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import convergence.fromJson
import convergence.moshi
import convergence.toJson

fun numToOrdinal(num: Int): String {
    val suffixes = arrayOf("th", "st", "nd", "rd")
    return "$num${suffixes.getOrNull(num % 10) ?: "th"}"
}

fun getReadStringOrThrow(reader: JsonReader, className: String, startCountAt: Int = 1): (String) -> String {
    var count = startCountAt
    fun readStringOrThrow(fieldName: String): String {
        if (reader.nextName() != fieldName)
            throw JsonDataException("$fieldName should be the ${numToOrdinal(count++)} field for a $className")
        return reader.nextString()
    }
    return ::readStringOrThrow
}

object DiscordMessageHistoryAdapter: JsonAdapter<DiscordMessageHistory>() {
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

    override fun toJson(writer: JsonWriter, value: DiscordMessageHistory?) {
        value?.let {
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
    override fun fromJson(reader: JsonReader): DiscordEmoji? {
        val readStringOrThrow = getReadStringOrThrow(reader, "DiscordEmoji")
        if (readStringOrThrow("type") != "DiscordEmoji")
            return null
        val url = readStringOrThrow("url")
        val name = readStringOrThrow("name")
        val emoji = readStringOrThrow("emoji")
        val guild = readStringOrThrow("guild")
        val id = readStringOrThrow("id")
        val emote = jda.getGuildById(guild)?.getEmoteById(emoji) ?: return null
        return DiscordEmoji(url, name, emote, id.toLong())
    }

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
    override fun fromJson(reader: JsonReader): DiscordUser? {
        var readStringOrThrow = getReadStringOrThrow(reader, "DiscordUser")
        if (readStringOrThrow("type") != "DiscordUser")
            return null
        if (reader.nextName() != "chat")
            throw JsonDataException("The 2nd field should be chat in DiscordUser!")
        val chatJson = when (val json = reader.readJsonValue()) {
            is String -> moshi.toJson(json)
            is Number -> moshi.toJson(json)
            is Boolean -> moshi.toJson(json)
            is List<*> -> moshi.toJson(json)
            is Map<*, *> -> moshi.toJson(json)
            else -> return null
        }
        val chat = moshi.fromJson<DiscordChat>(chatJson)
        readStringOrThrow = getReadStringOrThrow(reader, "DiscordUser", 3)
        val name = readStringOrThrow("name")
        val id = readStringOrThrow("id")
        val authorId = readStringOrThrow("author")
        val author = jda.getUserById(authorId) ?: return null
        return DiscordUser(chat, name, id.toLong(), author)
    }

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
