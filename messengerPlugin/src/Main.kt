package convergence.testPlugins.messengerPlugin

import co.aurasphere.botmill.core.annotation.BotEncryption
import co.aurasphere.botmill.core.internal.util.ConfigurationUtils
import convergence.*
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import java.nio.file.Paths
import java.time.OffsetDateTime

object MessengerProtocol: Protocol("Messenger")
object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IUserAvailability, IStickers {

    override val name: String
        get() = TODO()
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format>
        get() = TODO()

    override fun getUserNickname(chat: Chat, user: User): String? = TODO()
    override fun getBotNickname(chat: Chat): String? = TODO()
    override fun sendImage(chat: Chat, image: Image, name: String?) = TODO()
    override fun receivedImage(chat: Chat, image: Image, name: String) = TODO()
    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<BonusInterface.IMessageHistory.MessageHistory> = TODO()
    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<BonusInterface.IMessageHistory.MessageHistory> = TODO()
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

@BotEncryption
class DefaultEncryption {
    init {
        val enc = StandardPBEStringEncryptor()
        enc.setPassword(Paths.get(System.getProperty("user.home"), ".convergence", "password").toString())
        ConfigurationUtils.loadEncryptedConfigurationFile(enc, "config.properties")
    }
}


class Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    override fun init() {
        println("Messenger Plugin initialized.")

    }
}
