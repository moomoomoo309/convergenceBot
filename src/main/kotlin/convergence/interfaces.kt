package convergence

import java.time.LocalDateTime

open class Protocol(name: String)
open class User //Intentionally empty, because it might be represented as an int or a string or whatever.
open class Chat //Same as above.

abstract class BaseInterface {
    abstract fun receivedMessage(protocol: Protocol, chat: Chat, message: String, sender: User)
    abstract fun sendMessage(protocol: Protocol, chat: Chat, message: String, sender: User): Boolean
    abstract fun getBotName(protocol: Protocol, chat: Chat): String
    abstract fun listUsers(protocol: Protocol, chat: Chat): List<String>
}

interface INickname {
    fun getUserNickname(protocol: Protocol, chat: Chat, user: User): String?
    fun getBotNickname(protocol: Protocol, chat: Chat): String?
}

interface IImages {
    open class Image
    fun sendImage(protocol: Protocol, chat: Chat, image: Image)
    fun receivedImage(protocol: Protocol, chat: Chat, image: Image)
}

interface IOtherMessageEditable {
    fun receivedMessage(protocol: Protocol, chat: Chat, message: String, sender: User, newMessage: String)
    fun editedMessage(protocol: Protocol, oldMessage: String, sender: User, newMessage: String)
}

interface IMessageHistory {
    data class MessageHistory(var message: String, val sender: User)
    fun getMessages(protocol: Protocol, chat: Chat, since: LocalDateTime?): List<MessageHistory>
    fun getUserMessages(protocol: Protocol, chat: Chat, user: User, since: LocalDateTime?): List<MessageHistory>
}

interface IMention {
    fun getMentionText(protocol: Protocol, chat: Chat, user: User): String
    fun mentionedBot(protocol: Protocol, chat: Chat, message: String, user: User)
}

interface ITypingStatus {
    fun startedTyping(protocol: Protocol, chat: Chat, user: User)
    fun stoppedTyping(protocol: Protocol, chat: Chat, user: User)
    fun setBotTypingStatus(protocol: Protocol, chat: Chat, status: Boolean)
}

interface IStickers {
    open class Sticker
    fun sendSticker(protocol: Protocol, chat: Chat, sticker: Sticker)
    fun receivedSticker(protocol: Protocol, chat: Chat, sticker: Sticker)
}

interface IUserStatus { // Like your status on Skype.
    fun setBotStatus(protocol: Protocol, chat: Chat, status: String)
    fun getUserStatus(protocol: Protocol, chat: Chat, user: User): String
}

interface IUserAvailability {
    enum class Availability {
        Online, Offline, Away, DoNotDisturb
    }
    fun setBotAvailability(protocol: Protocol, chat: Chat, availability: Availability)
    fun getUserAvailability(protocol: Protocol, chat: Chat, user: User): Availability
}

interface IReadStatus {
    fun getReadStatus(protocol: Protocol, chat: Chat, message: IMessageHistory.MessageHistory): Boolean
    fun setReadStatus(protocol: Protocol, chat: Chat, message: IMessageHistory.MessageHistory, status: Boolean)
}

interface IFormatting {
    // If possible, the name would be an enum instead, but I cannot predict what will be supported, so it's a string.
    // This is also so if multiple protocols support the same thing, like bolding, they can share the same name.
    open class Format(name: String)

    fun getDelimiters(protocol: Protocol, format: Format)
    fun getSupportedFormats(protocol: Protocol): List<Format>
    fun supportsFormat(protocol: Protocol, format: Format): Boolean
}
