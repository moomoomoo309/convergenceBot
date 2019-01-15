package convergence.testPlugins.messengerPlugin

import convergence.*
import java.time.LocalDateTime

object MessengerProtocol: Protocol("Messenger")
object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IUserAvailability, IStickers {
    override val name: String
        get() = TODO()
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format>
        get() = TODO()

    override fun getUserNickname(chat: Chat, user: User): String? = TODO()
    override fun getBotNickname(chat: Chat): String? = TODO()
    override fun nicknameChanged(chat: Chat, user: User, oldName: String): Boolean = TODO()
    override fun sendImage(chat: Chat, image: Image, name: String?) = TODO()
    override fun receivedImage(chat: Chat, image: Image, name: String) = TODO()
    override fun getMessages(chat: Chat, since: LocalDateTime?): List<MessageHistory> = TODO()
    override fun getUserMessages(chat: Chat, user: User, since: LocalDateTime?): List<MessageHistory> = TODO()
    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun mentionedBot(chat: Chat, message: String, user: User) = TODO()
    override fun setBotAvailability(chat: Chat, availability: Availability) = TODO()
    override fun getUserAvailability(chat: Chat, user: User): Availability = TODO()
    override fun getDelimiters(format: Format): Pair<String, String> = TODO()
    override fun sendMessage(chat: Chat, message: String): Boolean = TODO()
    override fun getBot(chat: Chat): User = TODO()
    override fun getName(chat: Chat, user: User): String = TODO()
    override fun getChats(): List<Chat> = TODO()
    override fun getUsers(chat: Chat): List<User> = TODO()
    override fun getChatName(chat: Chat): String = TODO()
    override fun sendSticker(chat: Chat, sticker: Sticker) = TODO()
    override fun receivedSticker(chat: Chat, sticker: Sticker) = TODO()
}

class Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    override fun init() {
        println("Messenger Plugin initialized.")
    }
}
