@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.mutableSetOf
import kotlin.collections.set

@Polymorphic
abstract class Protocol(val name: String)

//Intentionally empty, because it might be represented as an int or a string or whatever.
@Polymorphic
abstract class User(val chat: Chat)

@Polymorphic
abstract class Chat(val protocol: Protocol, val name: String)

private object UniversalUser: User(UniversalChat)

object UniversalProtocol: Protocol("Universal") // Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal")
object FakeBaseInterface: BaseInterface {
    override val protocol: Protocol = UniversalProtocol

    init {
        if (!registerProtocol(this.protocol, this))
            logErr("Protocol with name \"$this.protocol.name\" registered more than once!")
    }

    override fun sendMessage(chat: Chat, message: String): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat): String = ""
    override val name: String = "FakeBaseInterface"
}

@Polymorphic
abstract class CommandLike(open val name: String,
                           open val helpText: String,
                           open val syntaxText: String) {
    constructor() : this("default", "default", "default")
}

@Serializable
data class Command(override val name: String,
                   val function: (List<String>, User) -> String?,
                   override val helpText: String,
                   override val syntaxText: String): CommandLike(name, helpText, syntaxText)

@Serializable
data class Alias(override val name: String,
                 val command: Command,
                 val args: List<String>,
                 override val helpText: String,
                 override val syntaxText: String): CommandLike(name, helpText, syntaxText)

interface BaseInterface {
    val name: String
    val protocol: Protocol
    fun receivedMessage(chat: Chat, message: String, sender: User) = runCommand(message, sender)
    fun sendMessage(chat: Chat, message: String): Boolean
    fun getBot(chat: Chat): User
    fun getName(chat: Chat, user: User): String
    fun getChats(): List<Chat>
    fun getUsers(chat: Chat): List<User>
    fun getChatName(chat: Chat): String
}

abstract class Callback<T>(fct: Any) {
    abstract operator fun invoke(vararg args: Any): Boolean
}

private val callbacks = HashMap<Class<out Callback<out BonusInterface>>, ArrayList<Callback<out BonusInterface>>>()

fun registerCallback(fct: Callback<out BonusInterface>) {
    if (callbacks[fct.javaClass] == null)
        callbacks[fct.javaClass] = ArrayList()
    callbacks[fct.javaClass]?.add(fct)
}

fun registerCallbacks() {
    for (bonusInterface in BonusInterface::class.sealedSubclasses)
        for (callbackClass in bonusInterface::class.nestedClasses.filter { it is Callback<*> })
            if (callbacks[callbackClass.java] == null)
                callbacks[callbackClass.java as Class<out Callback<out BonusInterface>>] = ArrayList()
}

fun runCallbacks(callbackClass: Class<*>, vararg args: Any): Boolean {
    var success = false
    if (callbackClass as Class<out Callback<out BonusInterface>> in callbacks)
        for (callback in callbacks[callbackClass]!!)
            if (callback(args))
                success = true
    return success
}


sealed class BonusInterface {
    interface INickname {
        fun getUserNickname(chat: Chat, user: User): String?
        fun getBotNickname(chat: Chat): String?

        class ChangedNickname(private val fct: (Chat, User, String) -> Boolean): Callback<INickname>(fct) {
            override fun invoke(vararg args: Any) = fct(args[0] as Chat, args[1] as User, args[2] as String)
        }

        fun changedNickname(chat: Chat, user: User, oldName: String): Boolean = runCallbacks(ChangedNickname::class.java, chat, user, oldName)
    }

    interface IImages {
        open class Image

        fun sendImage(chat: Chat, image: Image, name: String?)

        class ReceivedImage(private val fct: (Chat, Image, String) -> Boolean): Callback<IImages>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as Image, args[2] as String)
        }

        fun receivedImage(chat: Chat, image: Image, name: String): Boolean = runCallbacks(ReceivedImage::class.java, chat, image, name)
    }

    interface IOtherMessageEditable {
        fun editMessage(message: IMessageHistory.MessageHistory, oldMessage: String, sender: User, newMessage: String)

        class EditMessage(private val fct: (String, User, String) -> Boolean): Callback<IOtherMessageEditable>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as String, args[1] as User, args[2] as String)
        }

        fun editedMessage(oldMessage: String, sender: User, newMessage: String) = runCallbacks(EditMessage::class.java, oldMessage, sender, newMessage)
    }

    interface IMessageHistory {
        open class MessageHistory(var message: String, val timestamp: OffsetDateTime, val sender: User)

        fun getMessages(chat: Chat, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
        fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
    }

    interface IMention {
        fun getMentionText(chat: Chat, user: User): String

        class MentionedUser(private val fct: (Chat, String, Set<User>) -> Boolean): Callback<IMention>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as String, args[2] as Set<User>)
        }

        fun mentionedUsers(chat: Chat, message: String, users: Set<User>) = runCallbacks(MentionedUser::class.java, chat, message, users)
    }

    interface ITypingStatus {
        fun setBotTypingStatus(chat: Chat, status: Boolean)

        class StartedTyping(val fct: (Chat, User) -> Boolean): Callback<ITypingStatus>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User)
        }

        fun startedTyping(chat: Chat, user: User) = runCallbacks(StartedTyping::class.java, chat, user)

        class StoppedTyping(val fct: (Chat, User) -> Boolean): Callback<ITypingStatus>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User)
        }

        fun stoppedTyping(chat: Chat, user: User) = runCallbacks(StoppedTyping::class.java, chat, user)
    }

    interface IStickers {
        open class Sticker(val name: String, val URL: String?)

        fun sendSticker(chat: Chat, sticker: Sticker)

        class ReceivedSticker(val fct: (Chat, Sticker) -> Boolean): Callback<IStickers>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as Sticker)
        }

        fun receivedSticker(chat: Chat, sticker: Sticker) = runCallbacks(ReceivedSticker::class.java, chat, sticker)
    }

    interface IUserStatus { // Like your status on Skype.
        fun setBotStatus(chat: Chat, status: String)
        fun getUserStatus(chat: Chat, user: User): String
    }

    interface IUserAvailability {
        open class Availability

        fun setBotAvailability(chat: Chat, availability: Availability)
        fun getUserAvailability(chat: Chat, user: User): Availability

        class ChangedAvailability(val fct: (Chat, User, Availability) -> Boolean): Callback<IUserAvailability>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as User, args[2] as Availability)
        }

        fun changedAvailability(chat: Chat, user: User, availability: Availability) = runCallbacks(ChangedAvailability::class.java, chat, user, availability)
    }

    interface IReadStatus {
        fun getReadStatus(chat: Chat, message: MessageHistory): Set<User>
        fun setRead(chat: Chat, message: MessageHistory, user: User)

        class ReadByUser(val fct: (Chat, MessageHistory, User) -> Boolean): Callback<IReadStatus>(fct) {
            override fun invoke(vararg args: Any): Boolean = fct(args[0] as Chat, args[1] as MessageHistory, args[2] as User)
        }

        fun readByUser(chat: Chat, message: MessageHistory, user: User) = runCallbacks(ReadByUser::class.java, chat, message, user)
    }

    interface IFormatting {
        // If possible, the name would be an enum instead, but I want the ability for protocols to add extra formats
        // beyond the defaults in the companion object, so it's an open class.
        open class Format(name: String) {
            val name: String = name.toUpperCase()

            companion object {
                val bold = Format("BOLD")
                val italics = Format("ITALICS")
                val underline = Format("UNDERLINE")
                val monospace = Format("MONOSPACE")
                val code = Format("CODE")
                val strikethrough = Format("STRIKETHROUGH")
                val spoiler = Format("SPOILER")
            }
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
                    Format.spoiler
            )
        }
    }

    interface ICustomEmoji {
        open class Emoji(val name: String, val URL: String?)

        fun getEmojis(chat: Chat): List<Emoji>
    }
}

// The sealed class is useful, but I'm not going to put BonusInterface in front of everything.

typealias INickname = BonusInterface.INickname
typealias IImages = BonusInterface.IImages
typealias Image = BonusInterface.IImages.Image
typealias IOtherMessageEditable = BonusInterface.IOtherMessageEditable
typealias IMessageHistory = BonusInterface.IMessageHistory
typealias MessageHistory = BonusInterface.IMessageHistory.MessageHistory
typealias IMention = BonusInterface.IMention
typealias ITypingStatus = BonusInterface.ITypingStatus
typealias IStickers = BonusInterface.IStickers
typealias Sticker = BonusInterface.IStickers.Sticker
typealias IUserStatus = BonusInterface.IUserStatus
typealias IUserAvailability = BonusInterface.IUserAvailability
typealias Availability = BonusInterface.IUserAvailability.Availability
typealias IReadStatus = BonusInterface.IReadStatus
typealias IFormatting = BonusInterface.IFormatting
typealias Format = BonusInterface.IFormatting.Format
typealias ICustomEmoji = BonusInterface.ICustomEmoji
typealias Emoji = BonusInterface.ICustomEmoji.Emoji
