@file:Suppress("unused", "UNUSED_PARAMETER", "UNCHECKED_CAST")

package convergence

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import java.time.OffsetDateTime
import kotlin.reflect.KClass

abstract class Protocol(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()
}

//Intentionally empty, because it might be represented as an int or a string or whatever.
abstract class User(val chat: Chat): ISerializable

abstract class Chat(val protocol: Protocol, val name: String): ISerializable

object UniversalUser: User(UniversalChat), ISerializable {
    override fun serialize() = """{"type":"UniversalUser"}"""
}

object UniversalProtocol: Protocol("Universal"), ISerializable {
    override fun serialize() = """{"type":"UniversalProtocol"}"""
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal"), ISerializable {
    override fun serialize() = """{"type":"UniversalChat"}"""
}

object FakeBaseInterface: BaseInterface {
    override val protocol: Protocol = UniversalProtocol

    override fun sendMessage(chat: Chat, message: String): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat): String = ""

    override val name: String = "FakeBaseInterface"
}

sealed class CommandLike(open val chat: Chat,
                         open val name: String,
                         open val helpText: String,
                         open val syntaxText: String): ISerializable, Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = name.compareTo(other.name)

    data class Command(override val chat: Chat,
                       override val name: String,
                       val function: (List<String>, User) -> String?,
                       override val helpText: String,
                       override val syntaxText: String): CommandLike(chat, name, helpText, syntaxText) {
        override fun serialize() = mapOf(
                "type" to "Command",
                "name" to name,
                "chat" to chat.serialize()
        ).json()
    }

    data class Alias(
            override val chat: Chat,
            override val name: String,
            val command: Command,
            val args: List<String>,
            override val helpText: String,
            override val syntaxText: String): CommandLike(chat, name, helpText, syntaxText) {
        override fun serialize() = mapOf(
                "type" to "Alias",
                "name" to name,
                "args" to JsonArray(args).toJsonString(),
                "chat" to chat.serialize()
        ).json()
    }
}
typealias Command = CommandLike.Command
typealias Alias = CommandLike.Alias


interface ISerializable {
    fun serialize(): String
}

fun Map<String, Any>.json() = JsonObject(this).toJsonString()

val deserializationFunctions = mutableMapOf<String, (JsonObject) -> ISerializable>(
        "UniversalChat" to { UniversalChat },
        "UniversalUser" to { UniversalChat },
        "UniversalProtocol" to { UniversalProtocol },
        "SerializableOffsetDateTime" to { SerializableOffsetDateTime(OffsetDateTime.parse(it["payload"] as String)) },
        "ScheduledCommand" to {
            ScheduledCommand(deserialize<SerializableOffsetDateTime>(it["time"] as String).ODT,
                    deserialize(it["sender"] as String),
                    deserialize(it["commandData"] as String),
                    (it["id"] as String).toInt())
        },
        "Format" to { Format(it["name"] as String) }
)

fun <T: ISerializable> deserialize(str: String) = deserialize<T>(jsonParser.parse<JsonObject>(str)!!)
fun <T: ISerializable> deserialize(json: JsonObject): T =
        deserializationFunctions[json["type"] as String]!!(json) as T

interface BaseInterface: ISerializable {
    val name: String
    val protocol: Protocol
    fun receivedMessage(chat: Chat, message: String, sender: User) = runCommand(message, sender)
    fun sendMessage(chat: Chat, message: String): Boolean
    fun getBot(chat: Chat): User
    fun getName(chat: Chat, user: User): String
    fun getChats(): List<Chat>
    fun getUsers(chat: Chat): List<User>
    fun getChatName(chat: Chat): String
    override fun serialize() = mapOf("type" to name).json()
}

private val callbacks = HashMap<KClass<out OptionalFunctionality>, ArrayList<OptionalFunctionality>>()

fun registerCallback(fct: OptionalFunctionality) {
    callbacks.putIfAbsent(fct::class, ArrayList())
    callbacks[fct::class]?.add(fct)
            ?: throw IllegalArgumentException("Tried to register callback for unregistered class ${fct::class.simpleName}.")
}

fun runCallbacks(callbackClass: KClass<out OptionalFunctionality>, vararg args: Any) =
        callbacks[callbackClass]?.any { callback ->
            callback(args)
        } ?: false

inline fun <reified T: OptionalFunctionality> runCallbacks(vararg args: Any) = runCallbacks(T::class, *args)

/**
 * The set of classes that protocols can implement for additional functionality.
 * Each interface also may contain classes which extend this in order to act as callbacks.
 */
sealed class OptionalFunctionality {
    /**
     * Callback functionality
     */
    abstract operator fun invoke(vararg args: Any): Boolean

    interface INickname {
        fun getUserNickname(chat: Chat, user: User): String?
        fun getBotNickname(chat: Chat): String?

        class ChangedNickname(private val fct: (Chat, User, String) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any) = fct(args[0] as Chat, args[1] as User, args[2] as String)
        }

        fun changedNickname(chat: Chat, user: User, oldName: String) = runCallbacks<ChangedNickname>(chat, user, oldName)
    }

    interface IImages {
        open class Image

        fun sendImage(chat: Chat, image: Image, sender: User, message: String)

        class ReceivedImage(private val fct: (Chat, Image, User, String?) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as Image, args[2] as User, args[3] as String?
                    ?: "")
        }

        fun receivedImage(chat: Chat, image: Image, sender: User, message: String) = runCallbacks<ReceivedImage>(chat, image, sender, message)
    }

    interface IOtherMessageEditable {
        fun editMessage(message: IMessageHistory.MessageHistory, oldMessage: String, sender: User, newMessage: String)

        class EditMessage(private val fct: (String, User, String) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as String, args[1] as User, args[2] as String)
        }

        fun editedMessage(oldMessage: String, sender: User, newMessage: String) = runCallbacks<EditMessage>(oldMessage, sender, newMessage)
    }

    interface IMessageHistory {
        abstract class MessageHistory(var message: String, val timestamp: OffsetDateTime, val sender: User): ISerializable

        fun getMessages(chat: Chat, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
        fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
    }

    interface IMention {
        fun getMentionText(chat: Chat, user: User): String

        abstract class MentionedUser(private val fct: (Chat, String, Set<User>) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any) = fct(args[0] as Chat, args[1] as String, args[2] as Set<User>)
        }

        fun mentionedUsers(chat: Chat, message: String, users: Set<User>) = runCallbacks<MentionedUser>(chat, message, users)
    }

    interface ITypingStatus {
        fun setBotTypingStatus(chat: Chat, status: Boolean)

        class StartedTyping(val fct: (Chat, User) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User)
        }

        fun startedTyping(chat: Chat, user: User) = runCallbacks<StartedTyping>(chat, user)

        class StoppedTyping(val fct: (Chat, User) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User)
        }

        fun stoppedTyping(chat: Chat, user: User) = runCallbacks<StoppedTyping>(chat, user)
    }

    interface IStickers {
        open class Sticker(val name: String, val URL: String?)

        fun sendSticker(chat: Chat, sticker: Sticker)

        class ReceivedSticker(val fct: (Chat, Sticker, User) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as Sticker, args[2] as User)
        }

        fun receivedSticker(chat: Chat, sticker: Sticker, sender: User) = runCallbacks<ReceivedSticker>(chat, sticker, sender)
    }

    interface IUserStatus { // Like your status on Skype.
        fun setBotStatus(chat: Chat, status: String)
        fun getUserStatus(chat: Chat, user: User): String
    }

    interface IUserAvailability {
        open class Availability(val description: String)

        fun setBotAvailability(chat: Chat, availability: Availability)
        fun getUserAvailability(chat: Chat, user: User): Availability

        class ChangedAvailability(val fct: (Chat, User, Availability) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User, args[2] as Availability)
        }

        fun changedAvailability(chat: Chat, user: User, availability: Availability) = runCallbacks<ChangedAvailability>(chat, user, availability)
    }

    interface IReadStatus {
        fun getReadStatus(chat: Chat, message: MessageHistory): Set<User>
        fun setRead(chat: Chat, message: MessageHistory, user: User)

        class ReadByUser(val fct: (Chat, MessageHistory, User) -> Boolean): OptionalFunctionality() {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as MessageHistory, args[2] as User)
        }

        fun readByUser(chat: Chat, message: MessageHistory, user: User) = runCallbacks<ReadByUser>(chat, message, user)
    }

    interface IFormatting {
        // If possible, the name would be an enum instead, but I want the ability for protocols to add extra formats
        // beyond the defaults in the companion object, so it's an open class.
        open class Format(name: String): ISerializable {
            val name: String = name.toUpperCase()

            companion object {
                val bold = Format("BOLD")
                val italics = Format("ITALICS")
                val underline = Format("UNDERLINE")
                val monospace = Format("MONOSPACE")
                val code = Format("CODE")
                val strikethrough = Format("STRIKETHROUGH")
                val spoiler = Format("SPOILER")
                val greentext = Format("GREENTEXT")
            }

            override fun serialize() = mapOf(
                    "type" to "Format",
                    "name" to name
            ).json()
        }

        val supportedFormats: Set<Format>

        fun getDelimiters(format: Format): Pair<String, String>?

        companion object {
            val formats = mutableSetOf(
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

    interface ICustomEmoji {
        abstract class Emoji(val name: String, val URL: String?): ISerializable

        fun getEmojis(chat: Chat): List<Emoji>
    }
}

// The sealed class is useful, but I'm not going to put BonusInterface in front of everything.
typealias INickname = OptionalFunctionality.INickname

typealias IImages = OptionalFunctionality.IImages
typealias Image = OptionalFunctionality.IImages.Image
typealias IOtherMessageEditable = OptionalFunctionality.IOtherMessageEditable
typealias IMessageHistory = OptionalFunctionality.IMessageHistory
typealias MessageHistory = OptionalFunctionality.IMessageHistory.MessageHistory
typealias IMention = OptionalFunctionality.IMention
typealias ITypingStatus = OptionalFunctionality.ITypingStatus
typealias IStickers = OptionalFunctionality.IStickers
typealias Sticker = OptionalFunctionality.IStickers.Sticker
typealias IUserStatus = OptionalFunctionality.IUserStatus
typealias IUserAvailability = OptionalFunctionality.IUserAvailability
typealias Availability = OptionalFunctionality.IUserAvailability.Availability
typealias IReadStatus = OptionalFunctionality.IReadStatus
typealias IFormatting = OptionalFunctionality.IFormatting
typealias Format = OptionalFunctionality.IFormatting.Format
typealias ICustomEmoji = OptionalFunctionality.ICustomEmoji
typealias Emoji = OptionalFunctionality.ICustomEmoji.Emoji
