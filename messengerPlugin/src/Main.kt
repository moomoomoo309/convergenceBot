package convergence.testPlugins.messengerPlugin

import convergence.*
import java.time.LocalDateTime

object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IUserAvailability, IStickers {
    override val name: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val protocol: Protocol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getUserNickname(chat: Chat, user: User): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBotNickname(chat: Chat): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nicknameChanged(chat: Chat, user: User, oldName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendImage(chat: Chat, image: Image) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receivedImage(chat: Chat, image: Image) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessages(chat: Chat, since: LocalDateTime?): List<MessageHistory> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserMessages(chat: Chat, user: User, since: LocalDateTime?): List<MessageHistory> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMentionText(chat: Chat, user: User): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mentionedBot(chat: Chat, message: String, user: User) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setBotAvailability(chat: Chat, availability: Availability) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserAvailability(chat: Chat, user: User): Availability {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val supportedFormats: Set<Format>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getDelimiters(format: Format): Pair<String, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendMessage(chat: Chat, message: String, sender: User): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBot(chat: Chat): User {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(chat: Chat, user: User): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getChats(): List<Chat> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUsers(chat: Chat): List<User> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getChatName(chat: Chat): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendSticker(chat: Chat, sticker: Sticker) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receivedSticker(chat: Chat, sticker: Sticker) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    override fun init() {
        println("Messenger Plugin initialized.")
    }
}
