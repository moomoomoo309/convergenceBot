package convergence.testPlugins.discordPlugin

import convergence.*
import java.time.LocalDateTime

object DiscordInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IOtherMessageEditable, IUserAvailability, ICustomEmoji {
    override val name: String
        get() = TODO()
    override val protocol: Protocol
        get() = TODO()

    override fun getUserNickname(chat: Chat, user: User): String? {
        TODO()
    }

    override fun getBotNickname(chat: Chat): String? {
        TODO()
    }

    override fun nicknameChanged(chat: Chat, user: User, oldName: String): Boolean {
        TODO()
    }

    override fun sendImage(chat: Chat, image: Image) {
        TODO()
    }

    override fun receivedImage(chat: Chat, image: Image) {
        TODO()
    }

    override fun editedMessage(oldMessage: String, sender: User, newMessage: String) {
        TODO()
    }

    override fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String) {
        TODO()
    }

    override fun getMessages(chat: Chat, since: LocalDateTime?): List<MessageHistory> {
        TODO()
    }

    override fun getUserMessages(chat: Chat, user: User, since: LocalDateTime?): List<MessageHistory> {
        TODO()
    }

    override fun getMentionText(chat: Chat, user: User): String {
        TODO()
    }

    override fun mentionedBot(chat: Chat, message: String, user: User) {
        TODO()
    }

    override fun setBotAvailability(chat: Chat, availability: Availability) {
        TODO()
    }

    override fun getUserAvailability(chat: Chat, user: User): Availability {
        TODO()
    }

    override val supportedFormats: Set<Format>
        get() = TODO()

    override fun getDelimiters(format: Format): Pair<String, String> {
        TODO()
    }

    override fun getEmojis(chat: Chat): List<Emoji> {
        TODO()
    }

    override fun getEmojiURL(emoji: Emoji) {
        TODO()
    }

    override fun sendMessage(chat: Chat, message: String, sender: User): Boolean {
        TODO()
    }

    override fun getBot(chat: Chat): User {
        TODO()
    }

    override fun getName(chat: Chat, user: User): String {
        TODO()
    }

    override fun getChats(): List<Chat> {
        TODO()
    }

    override fun getUsers(chat: Chat): List<User> {
        TODO()
    }

    override fun getChatName(chat: Chat): String {
        TODO()
    }
}

class Main: Plugin {
    override val name = "DiscordPlugin"
    override val baseInterface: BaseInterface = DiscordInterface
    override fun init() {
        println("Discord Plugin initialized.")
    }
}
