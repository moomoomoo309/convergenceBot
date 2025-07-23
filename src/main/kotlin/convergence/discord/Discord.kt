package convergence.discord

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.sardine.Sardine
import com.github.sardine.SardineFactory
import convergence.*
import convergence.discord.MessageListener.forwardedMessages
import convergence.discord.calendar.registerCalendarCommands
import convergence.discord.frat.fratConfig
import convergence.discord.frat.registerFratCommands
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max

lateinit var jda: JDA

val discordLogger: Logger = LoggerFactory.getLogger("convergence.discord")

interface DiscordObject {
    val id: Long
}
typealias DUser = net.dv8tion.jda.api.entities.User
typealias DCustomEmoji = net.dv8tion.jda.api.entities.emoji.CustomEmoji

class DiscordAvailability(val status: OnlineStatus): Availability(status.name)
class DiscordServer(name: String, val guild: Guild): Server(name, DiscordProtocol) {
    constructor(guild: Guild): this(guild.name, guild)
    constructor(id: Long): this(jda.getGuildById(id)!!)

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
    Chat(DiscordProtocol, name), DiscordObject, HasServer<DiscordServer> {
    constructor(channel: GuildMessageChannel): this(channel.name, channel.idLong, channel)
    constructor(message: Message): this(message.channel.asGuildMessageChannel())
    constructor(id: Long): this(jda.getGuildChannelById(id) as GuildMessageChannel)
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
    MessageHistory(DiscordIncomingMessage(msg), msg.timeCreated, DiscordUser(msg.author)), DiscordObject {
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
    override val url: String,
    override val name: String,
    val emoji: DCustomEmoji,
    override val id: Long
): CustomEmoji(url, name), DiscordObject {
    constructor(emoji: DCustomEmoji): this(emoji.imageUrl, emoji.name, emoji, emoji.idLong)

    override fun asString() = emoji.asReactionCode
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
    override fun getURL() = URI(image.url)
    override fun getStream(): InputStream = image.proxy.download().get()
}

val sardine: Sardine by lazy { SardineFactory.begin("bot", fratConfig.botPassword) }
fun uploadImage(discordURL: URI, uploadURL: URI?, filename: String) {
    if (uploadURL == null)
        return
    val connection = discordURL.toURL().openConnection()
    val encoding = connection.contentEncoding
    val content = connection.getInputStream().readAllBytes()
    sardine.put(uploadURL.toString() + "/" + URLEncoder.encode(filename, "UTF8"), content, encoding)
}

class DiscordOutgoingMessage(val data: MessageCreateData): OutgoingMessage() {
    constructor(msg: String): this(
        MessageCreateBuilder()
            .setContent(msg)
            .build()
    )

    override fun toSimple(): SimpleOutgoingMessage {
        return SimpleOutgoingMessage(data.content)
    }
}

class DiscordIncomingMessage(val data: Message): IncomingMessage() {
    override fun toSimple(): SimpleIncomingMessage {
        return SimpleIncomingMessage(data.contentDisplay)
    }

    override fun toOutgoing(): OutgoingMessage {
        return DiscordOutgoingMessage(
            try {
                MessageCreateBuilder.fromMessage(data)
                    .build()
            } catch(_: IllegalStateException) {
                return SimpleOutgoingMessage("")
            }
        )
    }
}

fun ArgumentType.toDiscord() = when(this) {
    ArgumentType.NUMBER -> OptionType.NUMBER
    ArgumentType.STRING -> OptionType.STRING
    ArgumentType.INTEGER -> OptionType.INTEGER
    ArgumentType.BOOLEAN -> OptionType.BOOLEAN
}

object DiscordProtocol: Protocol("Discord"), CanFormatMessages, HasNicknames, HasImages, CanMentionUsers,
    HasMessageHistory, CanEditOtherMessages, HasUserAvailability, HasCustomEmoji, HasServers<DiscordServer>, HasReactions {
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
        } catch(_: FileNotFoundException) {
            discordLogger.error("You need to put your discord token in $convergencePath/discordToken!")
            return
        } catch(e: Exception) {
            e.printStackTrace()
            return
        }
        registerDiscordCommands()
        registerCalendarCommands()
        registerFratCommands()
        discordLogger.info("JDA Initialized.")
        jda.addEventListener(MessageListener)
        callbacks[ReceivedImages::class]!!.add(
            ReceivedImages { chat: Chat, _: IncomingMessage?, _: User, images: Array<Image> ->
                for (image in images) {
                    if (image is DiscordImage && chat in imageUploadChannels) {
                        val uploadURL = imageUploadChannels[chat]
                        val timeCreated = image.image.timeCreated.format(DateTimeFormatter.ISO_INSTANT)
                        val filename = image.image.fileName.substringBeforeLast(".")
                        uploadImage(image.getURL(), uploadURL, "$filename-$timeCreated${image.image.fileExtension ?: ""}")
                    }
                }
                true
            }
        )
        callbacks.getOrPut(ReactionChanged::class) { mutableListOf() }.add(
            ReactionChanged { sender: User, chat: Chat, message: IncomingMessage, emoji: IEmoji, oldAmount: Int, newAmount: Int ->
                if (chat !is DiscordChat) return@ReactionChanged false
                if (message !is DiscordIncomingMessage) return@ReactionChanged false
                if (emoji !is DiscordEmoji && emoji !is UnicodeEmoji) return@ReactionChanged false
                val server = chat.server
                val configs = reactServers[server]?.filter { it.destination.channel.guild == chat.channel.guild } ?: return@ReactionChanged false
                if (configs.isEmpty())
                    return@ReactionChanged false

                for (config in configs) {
                    val neededScore = config.emojis[emoji.asString()] ?: continue
                    if (newAmount == neededScore) {
                        if (message.data.idLong !in forwardedMessages.getOrDefault(server.guild.idLong, mutableSetOf())) {
                            config.destination.channel.sendMessage(MessageCreateBuilder()
                                .addContent(message.data.member?.asMention ?: continue)
                                .build()
                            ).queue()
                            message.data.forwardTo(config.destination.channel).queue { fwd ->
                                forwardedMessages.getOrPut(server.guild.idLong) { mutableSetOf() }.add(fwd.idLong)
                                val discordEmoji = if (emoji is DiscordEmoji) emoji.emoji else Emoji.fromUnicode(emoji.asString())
                                fwd.addReaction(discordEmoji).queue()
                            }
                        }
                        forwardedMessages.getOrPut(server.guild.idLong) { mutableSetOf() }.add(message.data.idLong)
                    }
                }
                return@ReactionChanged true
            }
        )
        jda.awaitReady()
    }

    override fun configLoaded() {
        jda.guilds.map { guild ->
            val slashCommands = guild.updateCommands()
            slashCommands.addCommands(
                listOf(commands[DiscordProtocol]!!, commands[UniversalProtocol]!!)
                    .flatMap { commandMap ->
                        commandMap.map { (name, command) ->
                            Commands.slash(name.lowercase(), command.helpText.take(100))
                                .setContexts(InteractionContextType.GUILD)
                                .addOptions(
                                    command.argSpecs.map {
                                        OptionData(it.type.toDiscord(), it.name.lowercase(), "ligma", !it.optional)
                                    }
                                )
                        }
                    }
            ).queue()
        }
    }

    override fun aliasCreated(alias: Alias) {
        val slashCommands = when(alias.scope) {
            is DiscordServer -> alias.scope
            is DiscordChat -> alias.scope.server
            else -> return
        }.guild.updateCommands()
        slashCommands.addCommands(
            Commands.slash(
                alias.name.lowercase(),
                "Alias that runs ${alias.command.name} with these arguments: ${alias.args}".take(100)
            )
                .setContexts(InteractionContextType.GUILD)
                .addOptions(alias.command.argSpecs.subList(max(alias.command.argSpecs.size, alias.args.size), alias.command.argSpecs.size).map {
                    OptionData(it.type.toDiscord(), it.name.lowercase(), "ligma", !it.optional)
                })
        ).queue()
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


    override fun sendImages(chat: Chat, message: OutgoingMessage, sender: User, vararg images: Image) {
        if (chat is DiscordChat && images.isArrayOf<DiscordImage>()) {
            @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
            val discordImages = images as Array<DiscordImage>
            try {
                when(message) {
                    is DiscordOutgoingMessage -> chat.channel.sendMessage(message.data).queue()
                    else -> chat.channel.sendMessage(
                        MessageCreateBuilder()
                            .addFiles(discordImages.map {
                                FileUpload.fromStreamSupplier(it.image.fileName) {
                                    it.image.proxy.download().join()
                                }
                            })
                            .setContent(message.toSimple().text)
                            .build()
                    ).queue()
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun editMessage(message: MessageHistory, oldMessage: IncomingMessage, sender: User, newMessage: OutgoingMessage) {
        if (message is DiscordMessageHistory)
            message.msg.editMessage(newMessage.toSimple().text).queue()
    }

    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<DiscordMessageHistory> {
        if (chat !is DiscordChat || (since != null && (since.isAfter(OffsetDateTime.now()) || since.isBefore(until))))
            return emptyList()
        val history = chat.channel.history

        repeat(10) { // Get the last 1000 messages, 100 at a time.
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
    override fun getEmojis(chat: Chat): List<DiscordEmoji> = jda.emojis.map { DiscordEmoji(it) }
    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean {
        if (chat !is DiscordChat)
            return false
        try {
            when(message) {
                is DiscordOutgoingMessage -> chat.channel.sendMessage(message.data).queue()
                else -> {
                    val text = message.toSimple().text
                    if (text.isNotEmpty())
                        chat.channel.sendMessage(text.take(2000)).complete()
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private val botUser: DiscordUser by lazy { DiscordUser(jda.selfUser) }
    override fun getBot(chat: Chat): User = botUser
    override fun getName(chat: Chat, user: User): String = if (user is DiscordUser) user.name else ""

    private val serverCache = mutableMapOf<Long, DiscordServer>()
    override fun getServers(): List<DiscordServer> = jda.guilds.map {
        getServer(it.idLong)!!
    }

    @OptIn(ExperimentalContracts::class)
    fun getServer(id: Long?): DiscordServer? {
        contract {
            this.returns(null) implies (id is Nothing?)
            this.returnsNotNull() implies (id is Long)
        }
        return serverCache[id] ?: DiscordServer(id ?: return null).also { server ->
            serverCache[id] = server
        }
    }

    private val chatCache = mutableMapOf<Long, DiscordChat>()
    override fun getChats(): List<DiscordChat> = jda.textChannels.map {
        getChat(it.idLong)!!
    }

    @OptIn(ExperimentalContracts::class)
    fun getChat(id: Long?): DiscordChat? {
        contract {
            this.returns(null) implies (id is Nothing?)
            this.returnsNotNull() implies (id is Long)
        }
        return chatCache[id] ?: DiscordChat(id ?: return null).also { chat ->
            chatCache[id] = chat
        }
    }

    private val userCache = mutableMapOf<Long, DiscordUser>()
    override fun getUsers(): List<DiscordUser> = jda.users.map {
        getUser(it.idLong)!!
    }

    @OptIn(ExperimentalContracts::class)
    fun getUser(id: Long?): DiscordUser? {
        contract {
            this.returns(null) implies (id is Nothing?) // Returns null
            this.returnsNotNull() implies (id is Long) // If the id is not null, it does not return null
        }
        return userCache[id] ?: DiscordUser(id ?: return null).also { user ->
            userCache[id] = user
        }
    }

    override fun getUsers(chat: Chat): List<User> {
        val channel = (chat as DiscordChat).channel
        return jda.users.filter {
            channel.canTalk(channel.guild.getMember(it) ?: return@filter false)
        }.map { userCache[it.idLong] ?: DiscordUser(it).also { user -> userCache[it.idLong] = user } }
    }

    override fun getChatName(chat: Chat): String = if (chat is DiscordChat) chat.name else ""
    override fun mention(chat: Chat, user: User, message: OutgoingMessage?) {
        sendMessage(chat, jda.retrieveUserById((user as DiscordUser).id).complete().asMention + message?.let { " $it" })
    }

    override fun react(message: IncomingMessage, emoji: IEmoji) {
        if (message !is DiscordIncomingMessage)
            return
        when(emoji) {
            is DiscordEmoji -> {
                message.data.addReaction(emoji.emoji)
            }
            is UnicodeEmoji -> {
                message.data.addReaction(Emoji.fromUnicode(emoji.emoji.name))
            }
            else -> return
        }.queue()
    }

    override fun unreact(message: IncomingMessage, emoji: IEmoji) {
        if (message !is DiscordIncomingMessage)
            return

        when(emoji) {
            is DiscordEmoji -> {
                message.data.removeReaction(emoji.emoji)
            }
            is UnicodeEmoji -> {
                message.data.removeReaction(Emoji.fromUnicode(emoji.emoji.name))
            }
            else -> return
        }.queue()
    }

    override fun getReactions(message: IncomingMessage): Map<IEmoji, Int> {
        if (message !is DiscordIncomingMessage)
            return emptyMap()
        return message.data.reactions.associate {
            if (it.emoji.type == Emoji.Type.CUSTOM)
                DiscordEmoji(it.emoji.asRich()) to it.count
            else
                UnicodeEmoji(it.emoji.name) to it.count
        }
    }
}

object MessageListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val chat = DiscordChat(event)
        val sender = DiscordUser(event)
        val message = DiscordIncomingMessage(event.message)
        val mentionedMembers = event.message.mentions.members
        if (mentionedMembers.isNotEmpty())
            DiscordProtocol.mentionedUsers(
                chat,
                DiscordIncomingMessage(event.message),
                mentionedMembers.map { DiscordUser(it) }.toSet(),
                sender
            )
        val images = event.message.attachments
            .filter { it.isImage }
            .map { DiscordImage(it) }
            .toTypedArray()
        if (images.isNotEmpty())
            DiscordProtocol.receivedImages(chat, message, sender, *images)
        else
            DiscordProtocol.receivedMessage(chat, message, sender)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val chat = DiscordProtocol.getChats().firstOrNull { it.channel == event.guildChannel } ?: return
        val sender = DiscordProtocol.getUsers().firstOrNull { it.id == event.member!!.user.idLong } ?: return
        val commandDelimiter = commandDelimiters[chat] ?: commandDelimiters[chat.server] ?: defaultCommandDelimiter
        val commandData =
            parseCommand(commandDelimiter + event.name + event.options.joinToString(" ", " ") { it.asString }, chat)
                ?: return
        val msg = replaceAliasVars(chat, commandData.command.function(commandData.args, chat, sender), sender)
        event.reply((msg as? DiscordOutgoingMessage ?: DiscordOutgoingMessage(msg!!.toSimple().text)).data).queue()
    }
    val forwardedMessages = mutableMapOf<Long, MutableSet<Long>>()

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        event.retrieveMessage().queue {
            val count = it.getReaction(event.reaction.emoji)?.count ?: return@queue
            DiscordProtocol.reactionChanged(
                DiscordProtocol.getUser(event.user?.idLong) ?: return@queue,
                DiscordChat(event.guildChannel),
                DiscordIncomingMessage(it),
                if (event.emoji.type == Emoji.Type.UNICODE)
                    UnicodeEmoji(event.emoji.name)
                else
                    DiscordEmoji(event.emoji as DCustomEmoji),
                count + 1,
                count
            )
        }

    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        event.retrieveMessage().queue {
            val count = it.getReaction(event.reaction.emoji)?.count ?: return@queue
            DiscordProtocol.reactionChanged(
                DiscordProtocol.getUser(event.user?.idLong) ?: return@queue,
                DiscordChat(event.guildChannel),
                DiscordIncomingMessage(it),
                if (event.emoji.type == Emoji.Type.UNICODE)
                    UnicodeEmoji(event.emoji.name)
                else
                        DiscordEmoji(event.emoji as DCustomEmoji),
                count - 1,
                count
            )
        }
    }
}
