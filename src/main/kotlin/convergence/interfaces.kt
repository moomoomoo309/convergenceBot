@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import java.time.LocalDateTime
import java.util.function.Consumer

abstract class Protocol(name: String)
abstract class User(protocol: Protocol) //Intentionally empty, because it might be represented as an int or a string or whatever.
abstract class Chat(protocol: Protocol) //Same as above.
private object UniversalProtocol: Protocol("Universal") // Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol)


abstract class CommandLike(open val name: String, open val helpText: String, open val syntaxText: String)

data class Command(override val name: String, val function: Consumer<Array<String>>, override val helpText: String,
                   override val syntaxText: String): CommandLike(name, helpText, syntaxText)

data class Alias(override val name: String, val command: Command, val args: List<String>,
                 override val helpText: String, override val syntaxText: String): CommandLike(name, helpText, syntaxText)


abstract class BaseInterface {
    abstract val name: String
    abstract fun receivedMessage(chat: Chat, message: String, sender: User)
    abstract fun sendMessage(chat: Chat, message: String, sender: User): Boolean
    abstract fun getBotName(chat: Chat): String
    abstract fun listUsers(chat: Chat): List<String>
}

interface INickname {
    fun getUserNickname(chat: Chat, user: User): String?
    fun getBotNickname(chat: Chat): String?
}

interface IImages {
    abstract class Image

    fun sendImage(chat: Chat, image: Image)
    fun receivedImage(chat: Chat, image: Image)
}

interface IOtherMessageEditable {
    fun receivedMessage(chat: Chat, message: String, sender: User, newMessage: String)
    fun editedMessage(oldMessage: String, sender: User, newMessage: String)
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
    // If possible, the name would be an enum instead, but I cannot predict what will be supported, so it's a string.
    // This is also so if multiple protocols support the same thing, like bolding, they can share the same name.
    abstract class Format(name: String)

    fun getDelimiters(protocol: Protocol, format: Format)
    fun getSupportedFormats(protocol: Protocol): List<Format>
    fun supportsFormat(protocol: Protocol, format: Format): Boolean
}
