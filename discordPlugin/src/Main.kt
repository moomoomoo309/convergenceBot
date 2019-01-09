package convergence.testPlugins.discordPlugin

import convergence.*
import java.time.LocalDateTime

object DiscordInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IOtherMessageEditable, IUserAvailability, ICustomEmoji {
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

    override fun editedMessage(oldMessage: String, sender: User, newMessage: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String) {
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

    override fun getEmojis(chat: Chat): List<Emoji> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEmojiURL(emoji: Emoji) {
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

}

class Main: Plugin {
    override val name = "DiscordPlugin"
    override val baseInterface: BaseInterface = DiscordInterface
    override fun init() {
        println("Discord Plugin initialized.")
    }
}
