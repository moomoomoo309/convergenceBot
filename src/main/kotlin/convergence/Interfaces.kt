@file:Suppress("unused", "UNCHECKED_CAST")
package convergence

import com.sigpwned.emoji4j.core.grapheme.Emoji
import java.io.InputStream
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

sealed interface CommandScope {
    val protocol: Protocol
    fun toKey(): String
}

abstract class Server(val name: String, override val protocol: Protocol): Comparable<Server>, CommandScope

typealias MessageCallback = (chat: Chat, message: IncomingMessage, sender: User) -> Unit
val messageCallbacks = mutableListOf<MessageCallback>(
    { chat, message, sender -> runCommand(chat, message, sender) },
)
abstract class Protocol(val name: String): Comparable<Protocol> {
    abstract fun init()
    abstract fun configLoaded()
    abstract fun aliasCreated(alias: Alias)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol) return false

        if (name != other.name) return false

        return true
    }

    override fun compareTo(other: Protocol) = this.name.compareTo(other.name)
    override fun toString(): String = this::class.java.simpleName
    override fun hashCode(): Int = name.hashCode()

    fun receivedMessage(chat: Chat, message: IncomingMessage, sender: User) = messageCallbacks.forEach {
        it(chat, message, sender)
    }
    abstract fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean
    fun sendMessage(chat: Chat, message: String) = sendMessage(chat, SimpleOutgoingMessage(message))

    abstract fun getChats(): List<Chat>
    abstract fun getBot(chat: Chat): User

    abstract fun getUsers(): List<User>
    abstract fun getUsers(chat: Chat): List<User>

    abstract fun getUserName(chat: Chat, user: User): String
    abstract fun getChatName(chat: Chat): String

    abstract fun commandScopeFromKey(key: String): CommandScope?
    abstract fun userFromKey(key: String): User?
}

abstract class User(val protocol: Protocol) {
    abstract fun toKey(): String
}

abstract class Chat(override val protocol: Protocol, val name: String): Comparable<Chat>, CommandScope {
    override fun compareTo(other: Chat) =
        "${protocol.name}-${this.name}".compareTo("${other.protocol.name}-${other.name}")

    override fun toString(): String {
        return "${this::class.java.simpleName}($name)"
    }
}

object UniversalUser: User(UniversalProtocol) {
    override fun toKey() = "UniversalUser()"
}

object UniversalProtocol: Protocol("Universal") {
    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getUserName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = listOf(UniversalChat)
    override fun getUsers(): List<User> = listOf(UniversalUser)
    override fun getUsers(chat: Chat): List<User> = listOf(UniversalUser)
    override fun getChatName(chat: Chat): String = ""
    override fun commandScopeFromKey(key: String): Chat? {
        if (key == "UniversalChat")
            return UniversalChat
        return null
    }

    override fun userFromKey(key: String): User? {
        if (key == "UniversalUser")
            return UniversalUser
        return null
    }

    override fun init() {
        // Do nothing
    }

    override fun configLoaded() {
        // Do nothing
    }

    override fun aliasCreated(alias: Alias) {
        // Do nothing
    }
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal") {
    override fun toKey() = "UniversalChat"
}

interface HasNicknames {
    fun getUserNickname(chat: Chat, user: User): String?
    fun getBotNickname(chat: Chat): String?

    fun changedNickname(chat: Chat, user: User, oldName: String) = runCallbacks<ChangedNickname>(chat, user, oldName)
}

abstract class Image {
    abstract fun getURL(): URI
    abstract fun getStream(): InputStream
    fun getBytes(): ByteArray = getStream().readAllBytes()
}

interface HasImages {
    fun sendImages(chat: Chat, message: OutgoingMessage, sender: User, vararg images: Image)
    fun sendImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        sendImages(chat, SimpleOutgoingMessage(message), sender, *images)

    fun receivedImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        receivedImages(chat, SimpleIncomingMessage(message), sender, *images)

    fun receivedImages(chat: Chat, message: IncomingMessage, sender: User, vararg images: Image) =
        runCallbacks<ReceivedImages>(chat, message, sender, images)
}

interface CanEditOtherMessages {
    fun editMessage(message: MessageHistory, oldMessage: IncomingMessage, sender: User, newMessage: OutgoingMessage)
    fun editedMessage(oldMessage: IncomingMessage, sender: User, newMessage: IncomingMessage) =
        runCallbacks<EditMessage>(oldMessage, sender, newMessage)
}

abstract class MessageHistory(var message: IncomingMessage, val timestamp: OffsetDateTime, val sender: User)
interface HasMessageHistory {
    fun getMessages(chat: Chat, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
    fun getUserMessages(
        chat: Chat,
        user: User,
        since: OffsetDateTime? = null,
        until: OffsetDateTime? = null
    ): List<MessageHistory>
}

interface CanMentionUsers {
    fun mention(chat: Chat, user: User, message: OutgoingMessage?)
    fun mention(chat: Chat, user: User) = mention(chat, user, null)
    fun mentionedUsers(chat: Chat, message: IncomingMessage, sender: User, users: Set<User>) =
        runCallbacks<MentionedUser>(chat, message, sender, users)
    fun getUserFromMentionText(chat: Chat, mention: String): User?
    fun getMentions(message: IncomingMessage): List<User>
}

interface HasTypingStatus {
    fun setBotTypingStatus(chat: Chat, status: Boolean)
    fun startedTyping(chat: Chat, user: User) = runCallbacks<StartedTyping>(chat, user)
    fun stoppedTyping(chat: Chat, user: User) = runCallbacks<StoppedTyping>(chat, user)
}

abstract class Sticker(val name: String, val url: String?)

interface HasStickers {
    fun sendSticker(chat: Chat, sticker: Sticker)
    fun receivedSticker(chat: Chat, sticker: Sticker, sender: User) =
        runCallbacks<ReceivedSticker>(chat, sticker, sender)
}

interface HasUserStatus { // Like your status on Skype.
    fun setBotStatus(chat: Chat, status: String)
    fun getUserStatus(chat: Chat, user: User): String
}

abstract class Availability(val description: String)

interface HasUserAvailability {
    fun setBotAvailability(chat: Chat, availability: Availability)
    fun getUserAvailability(chat: Chat, user: User): Availability
    fun changedAvailability(chat: Chat, user: User, availability: Availability) =
        runCallbacks<ChangedAvailability>(chat, user, availability)
}


interface HasReadStatus {
    fun getReadStatus(chat: Chat, message: MessageHistory): Set<User>
    fun setRead(chat: Chat, message: MessageHistory, user: User)
    fun readByUser(chat: Chat, message: MessageHistory, user: User) = runCallbacks<ReadByUser>(chat, message, user)
}

// If possible, the name would be an enum instead, but I want the ability for protocols to add extra formats
// beyond the defaults in the companion object, so it's an open class.
open class Format(name: String) {
    val name: String = name.lowercase(Locale.getDefault())

    companion object {
        val bold = Format("bold")
        val italics = Format("italics")
        val underline = Format("underline")
        val monospace = Format("monospace")
        val code = Format("code")
        val strikethrough = Format("strikethrough")
        val spoiler = Format("spoiler")
        val greentext = Format("greentext")
    }
}

interface CanFormatMessages {
    val supportedFormats: Set<Format>

    fun getDelimiters(format: Format): Pair<String, String>?

    companion object {
        val defaultFormats = mutableSetOf(
            Format.bold,
            Format.italics,
            Format.underline,
            Format.monospace,
            Format.code,
            Format.strikethrough,
            Format.spoiler,
            Format.greentext
        )
    }
}

interface IEmoji {
    fun asString(): String
}

abstract class CustomEmoji(open val name: String, open val url: String?): IEmoji
class UnicodeEmoji(val emoji: Emoji): IEmoji {
    constructor(s: String): this(s.toEmoji()!!)

    override fun asString(): String = emoji.toString()
}

interface HasCustomEmoji {
    fun getEmojis(chat: Chat): List<CustomEmoji>
}

interface HasReactions {
    fun react(message: IncomingMessage, emoji: IEmoji)
    fun unreact(message: IncomingMessage, emoji: IEmoji)
    fun getReactions(message: IncomingMessage): Map<IEmoji, Int>
    fun reactionChanged(
        sender: User,
        chat: Chat,
        message: IncomingMessage,
        emoji: IEmoji,
        oldAmount: Int,
        newAmount: Int
    ) = runCallbacks<ReactionChanged>(sender, chat, message, emoji, oldAmount, newAmount)
}

interface HasServer<T: Server> {
    val server: T
}

interface HasServers<T: Server> {
    fun getServers(): List<T>
}

abstract class IncomingMessage {
    abstract fun toSimple(): SimpleIncomingMessage
    abstract fun toOutgoing(): OutgoingMessage
}

class SimpleIncomingMessage(val text: String): IncomingMessage() {
    override fun toSimple() = this
    override fun toOutgoing() = SimpleOutgoingMessage(text)
}

abstract class OutgoingMessage {
    abstract fun toSimple(): SimpleOutgoingMessage
}

class SimpleOutgoingMessage(val text: String): OutgoingMessage() {
    override fun toSimple() = this
}
