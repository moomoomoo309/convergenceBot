@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import java.time.LocalDateTime

abstract class Protocol(val name: String)
//Intentionally empty, because it might be represented as an int or a string or whatever.
abstract class User(val chat: Chat)

abstract class Chat(val protocol: Protocol, val name: String)

private object UniversalUser: User(UniversalChat)

object UniversalProtocol: Protocol("Universal") // Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal")
object FakeBaseInterface: BaseInterface {
    override val protocol: Protocol = UniversalProtocol

    init {
        if (!registerProtocol(this.protocol, this))
            System.err.println("Protocol with name \"$this.protocol.name\" registered more than once!")
    }

    override fun sendMessage(chat: Chat, message: String, sender: User): Boolean {
        return false
    }

    override fun getBot(chat: Chat): User {
        return UniversalUser
    }

    override fun getName(chat: Chat, user: User): String {
        return ""
    }

    override fun getChats(): List<Chat> {
        return emptyList()
    }

    override fun getUsers(chat: Chat): List<User> {
        return emptyList()
    }

    override fun getChatName(chat: Chat): String {
        return ""
    }

    override val name: String = "FakeBaseInterface"
}

abstract class CommandLike(open val name: String,
                           open val helpText: String,
                           open val syntaxText: String)

data class Command(override val name: String,
                   val function: (List<String>, User) -> String?,
                   override val helpText: String,
                   override val syntaxText: String): CommandLike(name, helpText, syntaxText)

data class Alias(override val name: String,
                 val command: Command,
                 val args: List<String>,
                 override val helpText: String,
                 override val syntaxText: String): CommandLike(name, helpText, syntaxText)

interface BaseInterface {
    val name: String
    val protocol: Protocol
    fun receivedMessage(chat: Chat, message: String, sender: User) = runCommand(message, sender)
    fun sendMessage(chat: Chat, message: String, sender: User): Boolean
    fun getBot(chat: Chat): User
    fun getName(chat: Chat, user: User): String
    fun getChats(): List<Chat>
    fun getUsers(chat: Chat): List<User>
    fun getChatName(chat: Chat): String
}

private val callbacks = HashMap<Class<out BonusInterface>, ArrayList<(Chat, User, String) -> Boolean>>()
fun registerCallback(self: BonusInterface, fct: (Chat, User, String) -> Boolean) {
    if (callbacks[self.javaClass] == null)
        callbacks[self.javaClass] = ArrayList()
    callbacks[self.javaClass]?.add(fct)
}

fun registerCallbacks() {
    for (bonusInterface in BonusInterface::class.sealedSubclasses) {
        if (callbacks[bonusInterface.java] == null)
            callbacks[bonusInterface.java] = ArrayList()
    }
}


sealed class BonusInterface {
    interface INickname {
        fun getUserNickname(chat: Chat, user: User): String?
        fun getBotNickname(chat: Chat): String?
        fun nicknameChanged(chat: Chat, user: User, oldName: String): Boolean
    }

    interface IImages {
        abstract class Image

        fun sendImage(chat: Chat, image: Image)
        fun receivedImage(chat: Chat, image: Image)
    }

    interface IOtherMessageEditable {
        fun editedMessage(oldMessage: String, sender: User, newMessage: String)
        fun editMessage(message: IMessageHistory.MessageHistory, oldMessage: String, sender: User, newMessage: String)
    }

    interface IMessageHistory {
        data class MessageHistory(var message: String, val timestamp: LocalDateTime, val sender: User)

        fun getMessages(chat: Chat, since: LocalDateTime? = null): List<MessageHistory>
        fun getUserMessages(chat: Chat, user: User, since: LocalDateTime? = null): List<MessageHistory>
    }

    interface IMention {
        fun getMentionText(chat: Chat, user: User): String
        fun mentionedBot(chat: Chat, message: String, user: User)
    }

    interface ITypingStatus {
        fun startedTyping(chat: Chat, user: User)
        fun stoppedTyping(chat: Chat, user: User)
        fun setBotTypingStatus(chat: Chat, status: Boolean)
    }

    interface IStickers {
        abstract class Sticker

        fun sendSticker(chat: Chat, sticker: Sticker)
        fun receivedSticker(chat: Chat, sticker: Sticker)
    }

    interface IUserStatus { // Like your status on Skype.
        fun setBotStatus(chat: Chat, status: String)
        fun getUserStatus(chat: Chat, user: User): String
    }

    interface IUserAvailability {
        enum class Availability

        fun setBotAvailability(chat: Chat, availability: Availability)
        fun getUserAvailability(chat: Chat, user: User): Availability
    }

    interface IReadStatus {
        fun getReadStatus(chat: Chat, message: IMessageHistory.MessageHistory): Boolean
        fun setReadStatus(chat: Chat, message: IMessageHistory.MessageHistory, status: Boolean)
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
            }
        }

        val supportedFormats: Set<Format>

        fun getDelimiters(format: Format): Pair<String, String>

        companion object {
            val formats = mutableSetOf(
                    Format.bold,
                    Format.italics,
                    Format.underline,
                    Format.monospace,
                    Format.code
            )
        }
    }
}

// The sealed class is useful, but I'm not going to put BonusInterface in front of everything.

typealias INickname = BonusInterface.INickname
typealias IImages = BonusInterface.IImages
typealias IOtherMessageEditable = BonusInterface.IOtherMessageEditable
typealias IMessageHistory = BonusInterface.IMessageHistory
typealias IMention = BonusInterface.IMention
typealias ITypingStatus = BonusInterface.ITypingStatus
typealias IStickers = BonusInterface.IStickers
typealias IUserStatus = BonusInterface.IUserStatus
typealias IUserAvailability = BonusInterface.IUserAvailability
typealias IReadStatus = BonusInterface.IReadStatus
typealias IFormatting = BonusInterface.IFormatting
typealias Format = BonusInterface.IFormatting.Format
