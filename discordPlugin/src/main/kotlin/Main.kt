package convergence.testPlugins.discordPlugin

import com.squareup.moshi.Moshi
import convergence.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.time.OffsetDateTime
import javax.security.auth.login.LoginException

lateinit var jda: JDA

val discordLogger: Logger = LoggerFactory.getLogger("convergence.discord")

interface DiscordObject {
    val id: Long
}
typealias DUser = net.dv8tion.jda.api.entities.User
typealias DCustomEmoji = net.dv8tion.jda.api.entities.emoji.RichCustomEmoji

class DiscordAvailability(val status: OnlineStatus): Availability(status.name)

class DiscordChat(name: String, override val id: Long, val channel: MessageChannel): Chat(DiscordProtocol, name),
    DiscordObject, JsonConvertible {
    constructor(channel: MessageChannel): this(channel.name, channel.idLong, channel)
    constructor(message: Message): this(message.channel)
    constructor(msgEvent: MessageReceivedEvent): this(msgEvent.message)

    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) =
        this === other || (javaClass == other?.javaClass && id == (other as DiscordChat).id)
}

class DiscordMessageHistory(val msg: Message, override val id: Long):
    MessageHistory(msg.contentRaw, msg.timeCreated, DiscordUser(DiscordChat(msg), msg.author)), DiscordObject,
    JsonConvertible {
    constructor(msg: Message): this(msg, msg.idLong)
}

class DiscordUser(chat: Chat, val name: String, override val id: Long, val author: DUser):
    User(chat), DiscordObject, JsonConvertible {

    constructor(msgEvent: MessageReceivedEvent): this(DiscordChat(msgEvent), msgEvent)
    constructor(chat: Chat, msgEvent: MessageReceivedEvent): this(chat, msgEvent.author)
    constructor(chat: Chat, author: DUser): this(chat, author.name, author.idLong, author)
    constructor(chat: Chat, author: Member): this(chat, author.user)

    override fun toJson() = mapOf(
        "type" to "DiscordUser",
        "chat" to chat.toJson(),
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

data class DiscordEmoji(
    val url: String,
    override val name: String,
    val emoji: DCustomEmoji,
    override val id: Long
): CustomEmoji(url, name), DiscordObject, JsonConvertible {
    constructor(emoji: DCustomEmoji): this(emoji.imageUrl, emoji.name, emoji, emoji.idLong)
}

val formatMap = mapOf(
    Format.code to Pair("```", "```"),
    Format.monospace to Pair("`", "`"),
    Format.underline to Pair("__", "__"),
    Format.bold to Pair("**", "**"),
    Format.italics to Pair("*", "*"),
    Format.strikethrough to Pair("~~", "~~"),
    Format.spoiler to Pair("||", "||")
)

/** TODO: Use this, then add a [Json Adapter][com.squareup.moshi.JsonAdapter] for it. */
class DiscordImage(var url: String? = null, var data: ByteArray? = null): Image()

object DiscordProtocol: Protocol("Discord")
object DiscordInterface: BaseInterface, CanFormatMessages, HasNicknames, HasImages, CanMentionUsers, HasMessageHistory,
    CanEditOtherMessages, HasUserAvailability, HasCustomEmoji {
    override val name: String = "DiscordInterface"
    override val protocols = listOf(DiscordProtocol)
    override val supportedFormats = formatMap.keys

    override fun getUserNickname(chat: Chat, user: User): String? {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel)
            return chat.channel.guild.getMember(user.author)?.nickname
        return null
    }

    override fun getBotNickname(chat: Chat): String? = getUserNickname(chat, getBot(chat))


    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) {
        if (chat is DiscordChat && image is DiscordImage)
            if (image.url != null)
                chat.channel.sendFiles(FileUpload.fromData(URL(image.url).openStream(), name))
            else {
                val data = image.data
                if (data != null)
                    chat.channel.sendFiles(FileUpload.fromData(data, name))
            }
    }

    override fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String) {
        if (message is DiscordMessageHistory)
            message.msg.editMessage(newMessage)
    }

    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> {
        if (chat !is DiscordChat || (since != null && (since.isAfter(OffsetDateTime.now()) || since.isBefore(until))))
            return emptyList()
        val history = chat.channel.history
        if (history != null) {
            for (i in 0..9) { // Get the last 1000 messages, 100 at a time.
                history.retrievePast(100)
                if (since != null && history.retrievedHistory.last().timeCreated.isBefore(since)) {
                    if (until == null) {
                        for ((i2, msg) in history.retrievedHistory.withIndex())
                            if (msg.timeCreated.isAfter(since))
                                return history.retrievedHistory.subList(i2, history.size() - 1)
                                    .map { DiscordMessageHistory(it) }
                    } else {
                        var startIndex = 0
                        for ((i2, msg) in history.retrievedHistory.withIndex()) {
                            if (msg.timeCreated.isAfter(since))
                                startIndex = i2
                            if (msg.timeCreated.isAfter(until))
                                return history.retrievedHistory.subList(startIndex, i2)
                                    .map { DiscordMessageHistory(it) }
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

    override fun getUserMessages(
        chat: Chat,
        user: User,
        since: OffsetDateTime?,
        until: OffsetDateTime?
    ): List<MessageHistory> {
        if (user !is DiscordUser)
            return emptyList()
        return getMessages(chat, since).filter { it.sender == user }
    }

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
    override fun getEmojis(chat: Chat): List<CustomEmoji> = jda.emojis.map { DiscordEmoji(it) }
    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat !is DiscordChat)
            return false
        try {
            chat.channel.sendMessage(message).complete()
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun getBot(chat: Chat): User = DiscordUser(chat, jda.selfUser)
    override fun getName(chat: Chat, user: User): String = if (user is DiscordUser) user.name else ""
    override fun getChats(): List<Chat> = jda.textChannels.map { DiscordChat(it) }
    override fun getUsers(chat: Chat): List<User> = jda.users.map { DiscordUser(chat, it) }
    override fun getChatName(chat: Chat): String = if (chat is DiscordChat) chat.name else ""
    override fun mention(chat: Chat, user: User, message: String?) {
        sendMessage(chat, jda.retrieveUserById((user as DiscordUser).id).complete().asMention + message?.let { " $it" })
    }
}

object MessageListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val chat = DiscordChat(event)
        DiscordInterface.receivedMessage(chat, event.message.contentDisplay, DiscordUser(event))
        val mentionedMembers = event.message.mentions.members
        if (mentionedMembers.isNotEmpty())
            DiscordInterface.mentionedUsers(
                chat,
                event.message.contentDisplay,
                mentionedMembers.map { DiscordUser(chat, it) }.toSet()
            )
    }
}

lateinit var discordMoshi: Moshi

class DiscordPlugin(wrapper: PluginWrapper): Plugin(wrapper) {
    override val name = "DiscordPlugin"
    override val baseInterface = DiscordInterface
    val moshi: Moshi by sharedVariables
    override fun preinit() {
        val moshiBuilder: Moshi.Builder by sharedVariables
        moshiBuilder.add(DiscordMessageHistoryAdapter)
            .add(DiscordEmojiAdapter)
            .add(DiscordUserAdapter)
            .add(DiscordChatAdapter)
    }

    override fun init() {
        discordMoshi = this.moshi
        println("Discord Plugin initialized.")
        jda = try {
            val it = JDABuilder
                .create(
                    setOf(
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.GUILD_MESSAGE_TYPING,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.DIRECT_MESSAGES
                    )
                )
                .setToken(Files.readString(convergencePath.resolve("discordToken")).trim())
                .disableCache(CacheFlag.VOICE_STATE)
            it.build()
        } catch(e: FileNotFoundException) {
            discordLogger.error("You need to put your discord token in $convergencePath/discordToken!")
            return
        } catch(e: LoginException) {
            e.printStackTrace()
            return
        }
        jda.addEventListener(MessageListener)
    }
}
