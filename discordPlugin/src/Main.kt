package convergence.testPlugins.discordPlugin

import convergence.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import javax.security.auth.login.LoginException

var jda: JDA? = null

interface DiscordObject {
    val id: Long
}

class DiscordAvailability(val status: OnlineStatus): Availability()

fun localDateTimeToOffsetDateTime(ldt: LocalDateTime): OffsetDateTime = OffsetDateTime.of(ldt, TimeZone.getDefault().toZoneId().rules.getOffset(ldt))
fun offsetDateTimeToLocalDateTime(odt: OffsetDateTime): LocalDateTime = odt.toLocalDateTime()

class DiscordChat(name: String, override val id: Long, val channel: MessageChannel): Chat(DiscordProtocol, name), DiscordObject {
    constructor(channel: MessageChannel): this(channel.name, channel.idLong, channel)
    constructor(message: Message): this(message.channel)
    constructor(msgEvent: MessageReceivedEvent): this(msgEvent.message)
}

class DiscordMessageHistory(val msg: Message, override val id: Long): MessageHistory(msg.contentRaw, offsetDateTimeToLocalDateTime(msg.timeCreated), DiscordUser(DiscordChat(msg), msg.author)), DiscordObject {
    constructor(msg: Message): this(msg, msg.idLong)
}

class DiscordUser(chat: Chat, val name: String, override val id: Long, val author: net.dv8tion.jda.api.entities.User): User(chat), DiscordObject {
    constructor(msgEvent: MessageReceivedEvent): this(DiscordChat(msgEvent), msgEvent)
    constructor(chat: Chat, msgEvent: MessageReceivedEvent): this(chat, msgEvent.author)
    constructor(chat: Chat, author: net.dv8tion.jda.api.entities.User): this(chat, author.name, author.idLong, author)
}

class DiscordEmoji(url: String, name: String, val emoji: Emote, override val id: Long): Emoji(url, name), DiscordObject {
    constructor(emoji: Emote): this(emoji.imageUrl, emoji.name, emoji, emoji.idLong)
}

val formatMap = mapOf(Format.code to Pair("```", "```"),
        Format.monospace to Pair("`", "`"),
        Format.underline to Pair("__", "__"),
        Format.bold to Pair("**", "**"),
        Format.italics to Pair("*", "*"),
        Format.strikethrough to Pair("~~", "~~"))

class DiscordImage(var URL: String? = null, var data: ByteArray? = null): Image()

object DiscordProtocol: Protocol("Discord")
object DiscordInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IOtherMessageEditable, IUserAvailability, ICustomEmoji {
    override val name: String = "DiscordInterface"
    override val protocol: Protocol = DiscordProtocol
    override val supportedFormats: Set<Format> = formatMap.keys

    override fun getUserNickname(chat: Chat, user: User): String? {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel)
            return chat.channel.guild.getMember(user.author).nickname
        return null
    }

    override fun getBotNickname(chat: Chat): String? = getUserNickname(chat, getBot(chat))

    override fun nicknameChanged(chat: Chat, user: User, oldName: String): Boolean = TODO()

    override fun sendImage(chat: Chat, image: Image, name: String?) {
        if (chat is DiscordChat && image is DiscordImage)
            if (image.URL != null)
                chat.channel.sendFile(URL(image.URL).openStream(), name ?: "untitled")
            else if (image.data != null)
                chat.channel.sendFile(image.data, name ?: "untitled")
    }

    override fun receivedImage(chat: Chat, image: Image, name: String) = TODO()

    override fun editedMessage(oldMessage: String, sender: User, newMessage: String) = TODO()

    override fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String) {
        if (message is DiscordMessageHistory)
            message.msg.editMessage(newMessage)
    }

    override fun getMessages(chat: Chat, since: LocalDateTime?): List<MessageHistory> {
        if (chat !is DiscordChat || (since != null && since.isAfter(LocalDateTime.now())))
            return emptyList()
        val history = chat.channel.history
        if (history != null) {
            val sinceOffset = if (since != null) localDateTimeToOffsetDateTime(since) else null
            for (i in 0..9) {
                history.retrievePast(10)
                if (sinceOffset != null && history.retrievedHistory.last().timeCreated.isBefore(sinceOffset)) {
                    for ((i2, msg) in history.retrievedHistory.withIndex())
                        if (msg.timeCreated.isAfter(sinceOffset))
                            return history.retrievedHistory.subList(i2, history.size() - 1).map { DiscordMessageHistory(it) }
                }
            }
            return history.retrievedHistory.map { DiscordMessageHistory(it) }
        } else
            return emptyList()
    }

    override fun getUserMessages(chat: Chat, user: User, since: LocalDateTime?): List<MessageHistory> {
        if (user !is DiscordUser)
            return emptyList()
        return getMessages(chat, since).filter { it.sender == user }
    }

    override fun getMentionText(chat: Chat, user: User): String = if (user is DiscordUser) jda!!.retrieveUserById(user.id).complete().asMention else ""

    override fun mentionedBot(chat: Chat, message: String, user: User) {

    }

    override fun setBotAvailability(chat: Chat, availability: Availability) {
        if (availability is DiscordAvailability)
            jda?.presence?.setPresence(availability.status, true)
    }

    override fun getUserAvailability(chat: Chat, user: User): Availability {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel)
            return DiscordAvailability(chat.channel.guild.getMember(user.author).onlineStatus)
        return DiscordAvailability(OnlineStatus.UNKNOWN)
    }

    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun getEmojis(chat: Chat): List<Emoji> = jda!!.emotes.map { DiscordEmoji(it) }
    fun getCachedEmojis(chat: Chat) = jda!!.emoteCache.map { DiscordEmoji(it) }
    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat !is DiscordChat)
            return false
        chat.channel.sendMessage(message)
        return true
    }

    override fun getBot(chat: Chat): User = DiscordUser(chat, jda!!.selfUser)
    override fun getName(chat: Chat, user: User): String = if (user is DiscordUser) user.name else ""
    override fun getChats(): List<Chat> = jda?.textChannels?.map { DiscordChat(it) } ?: emptyList()
    fun getCachedChats(): List<Chat> = jda?.textChannelCache?.map { DiscordChat(it) } ?: emptyList()
    override fun getUsers(chat: Chat): List<User> = jda?.users?.map { DiscordUser(chat, it) } ?: emptyList()
    fun getCachedUsers(chat: Chat): List<User> = jda?.userCache?.map { DiscordUser(chat, it) } ?: emptyList()
    override fun getChatName(chat: Chat): String = if (chat is DiscordChat) chat.name else ""
}

object MessageListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val chat = DiscordChat(event)
        DiscordInterface.receivedMessage(chat, event.message.contentDisplay, DiscordUser(event))
    }
}

class Main: Plugin {
    override val name = "DiscordPlugin"
    override val baseInterface: BaseInterface = DiscordInterface
    override fun init() {
        println("Discord Plugin initialized.")
        jda = try {
            JDABuilder(String(Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".convergence", "discordToken")))).build()
        } catch (e: LoginException) {
            e.printStackTrace()
            return
        }
    }
}
