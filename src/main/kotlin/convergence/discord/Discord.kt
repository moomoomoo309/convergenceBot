package convergence.discord

import com.fasterxml.jackson.annotation.JsonIgnore
import convergence.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
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
class DiscordServer(name: String, val guild: Guild): Server(name, DiscordProtocol) {
    constructor(guild: Guild): this(guild.name, guild)
    override fun compareTo(other: Server): Int {
        if (other !is DiscordServer) {
            return name.compareTo(other.name)
        }
        return guild.idLong.compareTo(other.guild.idLong)
    }

    override fun toKey() = "DiscordServer(${guild.idLong})"
}
val serverCache = mutableMapOf<Long, DiscordServer>()

class DiscordChat(name: String, override val id: Long, @JsonIgnore val channel: GuildMessageChannel):
    Chat(DiscordProtocol, name), DiscordObject, HasServer {
    constructor(channel: GuildMessageChannel): this(channel.name, channel.idLong, channel)
    constructor(message: Message): this(message.channel.asGuildMessageChannel())
    constructor(msgEvent: MessageReceivedEvent): this(msgEvent.message)
    override val server = serverCache.getOrPut(id) { DiscordServer(channel.guild) }

    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?) =
        this === other || (javaClass == other?.javaClass && id == (other as DiscordChat).id)

    override fun toKey() = "DiscordChat($id)"

    override fun toString(): String =
        "${protocol.name}(${(channel as? GuildChannel)?.guild?.name ?: ""}#${channel.name})"
}

class DiscordMessageHistory(val msg: Message, override val id: Long):
    MessageHistory(msg.contentRaw, msg.timeCreated, DiscordUser(msg.author)), DiscordObject {
    constructor(msg: Message): this(msg, msg.idLong)
}

class DiscordUser(val name: String, override val id: Long, val author: DUser):
    User(DiscordProtocol), DiscordObject {

    constructor(msgEvent: MessageReceivedEvent): this(msgEvent.author)
    constructor(id: Long): this(jda.getUserById(id)!!)
    constructor(author: DUser): this(author.name, author.idLong, author)
    constructor(author: Member): this(author.user)

    override fun toKey() = "DiscordUser($id)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiscordUser) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String = "DiscordUser($name)"
}

data class DiscordEmoji(
    val url: String,
    override val name: String,
    val emoji: DCustomEmoji,
    override val id: Long
): CustomEmoji(url, name), DiscordObject {
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

class DiscordImage(val image: Message.Attachment): Image() {
    val url get() = image.url
}

object DiscordProtocol: Protocol("Discord"), CanFormatMessages, HasNicknames, HasImages, CanMentionUsers,
    HasMessageHistory, CanEditOtherMessages, HasUserAvailability, HasCustomEmoji, HasServers {
    override fun init() {
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
                        GatewayIntent.GUILD_EXPRESSIONS,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.SCHEDULED_EVENTS
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
        registerDiscordCommands()
        discordLogger.info("JDA Initialized.")
        jda.addEventListener(MessageListener)
    }

    @JsonIgnore
    override val supportedFormats = formatMap.keys

    override fun userFromKey(key: String): User? {
        if (key.startsWith("DiscordUser("))
            return DiscordUser(key.substringBetween("DiscordUser(", ")").toLong())
        return null
    }

    override fun commandScopeFromKey(key: String): CommandScope? {
        if (!key.startsWith(this.name))
            return null
        val scopeType = key.substringBetween(this.name, "(")
        return when(scopeType) {
            "Chat" -> {
                val id = key.substringBetween("(", ")").toLongOrNull() ?: return null
                return chatCache[id] ?: DiscordChat(
                    jda.getGuildChannelById(id) as? GuildMessageChannel ?: return null
                ).also { chat ->
                    chatCache[id] = chat
                }
            }
            "Server" -> {
                val id = key.substringBetween("(", ")").toLongOrNull() ?: return null
                return serverCache[id] ?: DiscordServer(
                    jda.getGuildById(id) ?: return null
                ).also { server ->
                    serverCache[id] = server
                }
            }
            else -> null
        }

    }

    override fun getUserNickname(chat: Chat, user: User): String? {
        if (user is DiscordUser && chat is DiscordChat && chat.channel is TextChannel)
            return chat.channel.guild.getMember(user.author)?.nickname
        return null
    }

    override fun getBotNickname(chat: Chat): String? = getUserNickname(chat, getBot(chat))


    override fun sendImages(chat: Chat, message: String, sender: User, vararg images: Image) {
        if (chat is DiscordChat && images.isArrayOf<DiscordImage>()) {
            @Suppress("UNCHECKED_CAST")
            val discordImages = images as Array<DiscordImage>
            try {
                chat.channel.sendMessage(
                    MessageCreateBuilder()
                        .addFiles(discordImages.map {
                            FileUpload.fromStreamSupplier(it.image.fileName) {
                                it.image.proxy.download().join()
                            }
                        })
                        .setContent(message)
                        .build()
                ).queue()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String) {
        if (message is DiscordMessageHistory)
            message.msg.editMessage(newMessage).queue()
    }

    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<DiscordMessageHistory> {
        if (chat !is DiscordChat || (since != null && (since.isAfter(OffsetDateTime.now()) || since.isBefore(until))))
            return emptyList()
        val history = chat.channel.history

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
    }

    override fun getUserMessages(
        chat: Chat,
        user: User,
        since: OffsetDateTime?,
        until: OffsetDateTime?
    ): List<DiscordMessageHistory> {
        if (user !is DiscordUser)
            return emptyList()
        return getMessages(chat, since).filter { it.sender == user }
    }

    override fun setBotAvailability(chat: Chat, availability: Availability) {
        if (availability is DiscordAvailability)
            jda.presence.setPresence(availability.status, true)
    }

    override fun getUserAvailability(chat: Chat, user: User): DiscordAvailability {
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
            chat.channel.sendMessage(message.take(2000)).complete()
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private val botUser: DiscordUser by lazy { DiscordUser(jda.selfUser) }
    override fun getBot(chat: Chat): User = botUser
    override fun getName(chat: Chat, user: User): String = if (user is DiscordUser) user.name else ""
    private val chatCache = mutableMapOf<Long, DiscordChat>()
    private val serverCache = mutableMapOf<Long, DiscordServer>()

    override fun getServers(): List<Server> = jda.guilds.map {
        serverCache[it.idLong] ?: DiscordServer(it).also { server ->
            serverCache[it.idLong] = server
        }
    }

    override fun getChats(): List<Chat> = jda.textChannels.map {
        chatCache[it.idLong] ?: DiscordChat(it).also { chat ->
            chatCache[it.idLong] = chat
        }
    }

    private val userCache = mutableMapOf<Long, DiscordUser>()
    override fun getUsers(): List<User> = jda.users.map {
        userCache[it.idLong] ?: DiscordUser(it).also { user ->
            userCache[it.idLong] = user
        }
    }

    override fun getUsers(chat: Chat): List<User> {
        val channel = (chat as DiscordChat).channel
        return jda.users.filter {
            channel.canTalk(channel.guild.getMember(it) ?: return@filter false)
        }.map { userCache[it.idLong] ?: DiscordUser(it).also { user -> userCache[it.idLong] = user } }
    }

    override fun getChatName(chat: Chat): String = if (chat is DiscordChat) chat.name else ""
    override fun mention(chat: Chat, user: User, message: String?) {
        sendMessage(chat, jda.retrieveUserById((user as DiscordUser).id).complete().asMention + message?.let { " $it" })
    }
}

object MessageListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val chat = DiscordChat(event)
        val messageText = event.message.contentDisplay
        val sender = DiscordUser(event)
        val mentionedMembers = event.message.mentions.members
        if (mentionedMembers.isNotEmpty())
            DiscordProtocol.mentionedUsers(
                chat,
                event.message.contentDisplay,
                mentionedMembers.map { DiscordUser(it) }.toSet(),
                sender
            )
        val images = event.message.attachments
            .filter { it.isImage }
            .map { DiscordImage(it) }
            .toTypedArray()
        if (images.isNotEmpty())
            DiscordProtocol.receivedImages(chat, messageText, sender, *images)
        else
            DiscordProtocol.receivedMessage(chat, messageText, sender)
    }
}
