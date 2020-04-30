package convergence.testPlugins.discordPlugin

import convergence.*
import convergence.User
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import javax.security.auth.login.LoginException

lateinit var jda: JDA

interface DiscordObject {
    val id: Long
}

class DiscordAvailability(val status: OnlineStatus): Availability(status.name)

class DiscordChat(name: String, override val id: Long, val channel: MessageChannel): Chat(DiscordProtocol, name), DiscordObject, ISerializable {
    constructor(channel: MessageChannel): this(channel.name, channel.idLong, channel)
    constructor(message: Message): this(message.channel)
    constructor(msgEvent: MessageReceivedEvent): this(msgEvent.message)

    override fun hashCode() = id.hashCode()
    override fun serialize() = mapOf("type" to "DiscordChat",
            "name" to name,
            "id" to id,
            "channel" to channel.id
    ).json()

    override fun equals(other: Any?) = this === other || (javaClass == other?.javaClass && id == (other as DiscordChat).id)
}

class DiscordMessageHistory(val msg: Message, override val id: Long): MessageHistory(msg.contentRaw, msg.timeCreated, DiscordUser(DiscordChat(msg), msg.author)), DiscordObject, ISerializable {
    constructor(msg: Message): this(msg, msg.idLong)

    override fun serialize() = mapOf(
            "msg" to msg.id,
            "msgChannel" to msg.channel.id,
            "id" to id
    ).json()
}

class DiscordUser(chat: Chat, val name: String, override val id: Long, val author: net.dv8tion.jda.api.entities.User): User(chat), DiscordObject, ISerializable {
    constructor(msgEvent: MessageReceivedEvent): this(DiscordChat(msgEvent), msgEvent)
    constructor(chat: Chat, msgEvent: MessageReceivedEvent): this(chat, msgEvent.author)
    constructor(chat: Chat, author: net.dv8tion.jda.api.entities.User): this(chat, author.name, author.idLong, author)
    constructor(chat: Chat, author: Member): this(chat, author.user)

    override fun serialize() = mapOf(
            "type" to "DiscordUser",
            "chat" to chat.serialize(),
            "name" to name,
            "id" to id,
            "author" to author.idLong
    ).json()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiscordUser) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class DiscordEmoji(val url: String, name: String, val emoji: Emote, override val id: Long): Emoji(url, name), DiscordObject, ISerializable {
    constructor(emoji: Emote): this(emoji.imageUrl, emoji.name, emoji, emoji.idLong)

    override fun serialize() = mapOf(
            "type" to "DiscordEmoji",
            "url" to url,
            "name" to name,
            "emoji" to emoji.id,
            "guild" to emoji.guild!!.id,
            "id" to id
    ).json()
}

val formatMap = mapOf(Format.code to Pair("```", "```"),
        Format.monospace to Pair("`", "`"),
        Format.underline to Pair("__", "__"),
        Format.bold to Pair("**", "**"),
        Format.italics to Pair("*", "*"),
        Format.strikethrough to Pair("~~", "~~"),
        Format.spoiler to Pair("||", "||"))

class DiscordImage(var URL: String? = null, var data: ByteArray? = null): Image()

object DiscordProtocol: Protocol("Discord")
object DiscordInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IOtherMessageEditable, IUserAvailability, ICustomEmoji {
    override val name: String = Main.name
    override val protocol: Protocol = DiscordProtocol
    override val supportedFormats: Set<Format> = formatMap.keys

    override fun getUserNickname(chat: Chat, user: User): String? {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel)
            return chat.channel.guild.getMember(user.author)?.nickname
        return null
    }

    override fun getBotNickname(chat: Chat): String? = getUserNickname(chat, getBot(chat))


    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) {
        if (chat is DiscordChat && image is DiscordImage)
            if (image.URL != null)
                chat.channel.sendFile(URL(image.URL).openStream(), name)
            else {
                val data = image.data
                if (data != null)
                    chat.channel.sendFile(data, name)
            }
    }

    override fun editMessage(message: OptionalFunctionality.IMessageHistory.MessageHistory, oldMessage: String, sender: User, newMessage: String) {
        if (message is DiscordMessageHistory)
            message.msg.editMessage(newMessage)
    }

    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> {
        if (chat !is DiscordChat || (since != null && (since.isAfter(OffsetDateTime.now()) || since.isBefore(until))))
            return emptyList()
        val history = chat.channel.history
        if (history != null) {
            for (i in 0..9) {
                history.retrievePast(100)
                if (since != null && history.retrievedHistory.last().timeCreated.isBefore(since)) {
                    if (until == null) {
                        for ((i2, msg) in history.retrievedHistory.withIndex())
                            if (msg.timeCreated.isAfter(since))
                                return history.retrievedHistory.subList(i2, history.size() - 1).map { DiscordMessageHistory(it) }
                    } else {
                        var startIndex = 0
                        for ((i2, msg) in history.retrievedHistory.withIndex()) {
                            if (msg.timeCreated.isAfter(since))
                                startIndex = i2
                            if (msg.timeCreated.isAfter(until))
                                return history.retrievedHistory.subList(startIndex, i2).map { DiscordMessageHistory(it) }
                        }
                        if (history.retrievedHistory.first().timeCreated.isAfter(since))
                            return history.retrievedHistory.map { DiscordMessageHistory(it) }
                        return emptyList()
                    }
                }
            }
            return history.retrievedHistory.map { DiscordMessageHistory(it) }
        } else
            return emptyList()
    }

    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> {
        if (user !is DiscordUser)
            return emptyList()
        return getMessages(chat, since).filter { it.sender == user }
    }

    override fun getMentionText(chat: Chat, user: User): String = if (user is DiscordUser) jda.retrieveUserById(user.id).complete().asMention else ""

    override fun setBotAvailability(chat: Chat, availability: Availability) {
        if (availability is DiscordAvailability)
            jda.presence.setPresence(availability.status, true)
    }

    override fun getUserAvailability(chat: Chat, user: User): Availability {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel) {
            val member = chat.channel.guild.getMember(user.author) ?: return DiscordAvailability(OnlineStatus.UNKNOWN)
            return DiscordAvailability(member.onlineStatus)
        }
        return DiscordAvailability(OnlineStatus.UNKNOWN)
    }

    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun getEmojis(chat: Chat): List<Emoji> = jda.emotes.map { DiscordEmoji(it) }
    fun getCachedEmojis() = jda.emoteCache.map { DiscordEmoji(it) }
    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat !is DiscordChat)
            return false
        try {
            chat.channel.sendMessage(message).complete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun getBot(chat: Chat): User = DiscordUser(chat, jda.selfUser)
    override fun getName(chat: Chat, user: User): String = if (user is DiscordUser) user.name else ""
    override fun getChats(): List<Chat> = jda.textChannels.map { DiscordChat(it) }
    fun getCachedChats(): List<Chat> = jda.textChannelCache.map { DiscordChat(it) }
    override fun getUsers(chat: Chat): List<User> = jda.users.map { DiscordUser(chat, it) }
    fun getCachedUsers(chat: Chat): List<User> = jda.userCache.map { DiscordUser(chat, it) }
    override fun getChatName(chat: Chat): String = if (chat is DiscordChat) chat.name else ""
}

object MessageListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val chat = DiscordChat(event)
        DiscordInterface.receivedMessage(chat, event.message.contentDisplay, DiscordUser(event))
        val mentionedMembers = event.message.mentionedMembers
        if (mentionedMembers.isNotEmpty())
            DiscordInterface.mentionedUsers(chat, event.message.contentDisplay, mentionedMembers.map { DiscordUser(chat, it) }.toSet())
    }
}

object Main: Plugin {
    override val name = "DiscordPlugin"
    override val baseInterface = DiscordInterface
    override fun init() {
        deserializationFunctions["DiscordUser"] = {
            DiscordUser(
                    deserialize<DiscordChat>(it["chat"] as String),
                    it["name"] as String,
                    (it["name"] as String).toLong(),
                    jda.retrieveUserById(it["author"] as String).complete(true)
            )
        }
        deserializationFunctions["DiscordMessageHistory"] = {
            val channel = jda.getTextChannelById(it["msgChannel"] as String)!!
            DiscordMessageHistory(
                    channel.retrieveMessageById(it["msg"] as String).complete(true),
                    (it["id"] as String).toLong()
            )
        }
        deserializationFunctions["DiscordEmoji"] = {
            val guild = jda.getGuildById(it["guild"] as String)!!
            DiscordEmoji(it["url"] as String,
                    it["name"] as String,
                    guild.getEmoteById(it["emoji"] as String)!!,
                    (it["id"] as String).toLong()
            )
        }
        deserializationFunctions["DiscordInterface"] = { DiscordInterface }
        registerProtocol(DiscordProtocol, DiscordInterface)
        println("Discord Plugin initialized.")
        jda = try {
            val it = JDABuilder(String(
                    Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".convergence", "discordToken"))).trim())
            it.enableIntents(
                    setOf(
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_MESSAGE_TYPING,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.DIRECT_MESSAGE_TYPING,
                            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                            GatewayIntent.DIRECT_MESSAGES
                    )
            )
            it.disableCache(CacheFlag.VOICE_STATE)
            it.build()
        } catch (e: FileNotFoundException) {
            logErr("You need to put your discord token in .convergence/discordToken!")
            return
        } catch (e: LoginException) {
            e.printStackTrace()
            return
        }
        jda.addEventListener(MessageListener)
    }
}
