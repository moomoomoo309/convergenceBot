package convergence.testPlugins.messengerPlugin

import co.aurasphere.botmill.core.annotation.Bot
import co.aurasphere.botmill.core.annotation.BotEncryption
import co.aurasphere.botmill.core.internal.util.ConfigurationUtils
import co.aurasphere.botmill.fb.FbBot
import co.aurasphere.botmill.fb.event.FbBotMillEventType
import co.aurasphere.botmill.fb.internal.util.network.FbBotMillNetworkController.getUserProfile
import co.aurasphere.botmill.fb.model.annotation.FbBotMillController
import co.aurasphere.botmill.fb.model.api.userprofile.FacebookUserProfile
import co.aurasphere.botmill.fb.model.base.Attachment
import co.aurasphere.botmill.fb.model.incoming.MessageEnvelope
import co.aurasphere.botmill.fb.model.outcoming.factory.ReplyFactory
import convergence.*
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import kotlin.reflect.jvm.jvmName

object MessengerProtocol: Protocol("Messenger")

class MessengerChat(val envelope: MessageEnvelope): Chat(MessengerProtocol, envelope.recipient.id)
class MessengerUser(val envelope: MessageEnvelope): User(MessengerChat(envelope))

class MessengerAttachments(val attachments: List<Attachment>): Image()

@Bot
object MessengerBot: FbBot() {
    @FbBotMillController(eventType = FbBotMillEventType.MESSAGE)
    fun messageReceived(envelope: MessageEnvelope) = MessengerInterface.receivedMessage(MessengerChat(envelope), envelope.message.text, MessengerUser(envelope))

    @FbBotMillController(eventType = FbBotMillEventType.IMAGE)
    fun imageReceived(envelope: MessageEnvelope) {
        MessengerInterface.receivedImage(MessengerChat(envelope), MessengerAttachments(envelope.message.attachments), MessengerUser(envelope), envelope.message.text)
    }
}

object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IReadStatus, IStickers {
    override fun getReadStatus(chat: Chat, message: MessageHistory): Set<User> {
        TODO()
    }

    override fun setRead(chat: Chat, message: MessageHistory, user: User) {
        TODO()
    }

    val profileCache = HashMap<String, Pair<FacebookUserProfile, OffsetDateTime>>()
    override val name: String = Main.name
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format> = setOf(Format.code, Format.monospace, Format.underline, Format.bold, Format.italics)
    val formatMap = mapOf(Format.code to Pair("```", "```"),
            Format.monospace to Pair("`", "`"),
            Format.underline to Pair("__", "__"),
            Format.bold to Pair("**", "**"),
            Format.italics to Pair("*", "*"))

    override fun getUserNickname(chat: Chat, user: User): String? = this.getName(chat, user)

    override fun getBotNickname(chat: Chat): String? = this.getName((chat as MessengerChat).envelope.sender.id)
    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun receivedImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat is MessengerChat) {
            ReplyFactory.addTextMessageOnly(message).build(chat.envelope)
            return true
        }
        return false
    }

    override fun getBot(chat: Chat): User = MessengerUser(MessengerBot.envelope)
    override fun getName(chat: Chat, user: User): String =
            if (user is MessengerUser) getName(user.envelope.recipient.id)!!
            else throw IllegalArgumentException("Non-messenger user passed into messenger! User class: ${user::class.jvmName}")

    fun getName(userId: String): String? {
        if (profileCache[userId]?.let { it.second >= OffsetDateTime.now() } != false)
            profileCache[userId] = Pair(getUserProfile(userId), OffsetDateTime.now().plusDays(1))
        return profileCache[userId]!!.let { "${it.first.firstName} ${it.first.lastName}" }
    }

    override fun getChats(): List<Chat> = TODO("Needs to be done with serialization")
    override fun getUsers(chat: Chat): List<User> {
        //TODO: Need to implement this, since botmill doesn't implement POST/t_<thread_id>/participants
        // https://developers.facebook.com/docs/workplace/integrations/custom-integrations/bots#botsingroups
        TODO()
    }

    override fun getChatName(chat: Chat): String {
        //TODO: Need to implement this, since botmill doesn't implement POST/t_<thread_id>/threadname
        // https://developers.facebook.com/docs/workplace/integrations/custom-integrations/bots#botsingroups
        TODO()
    }

    override fun sendSticker(chat: Chat, sticker: Sticker) = TODO()
    override fun receivedSticker(chat: Chat, sticker: Sticker) = TODO()
}

@BotEncryption
class DefaultEncryption {
    init {
        val enc = StandardPBEStringEncryptor()
        enc.setPassword(Files.lines(Paths.get(System.getProperty("user.home"), ".convergence", "facebookPassword")).findFirst().get())
        ConfigurationUtils.loadEncryptedConfigurationFile(enc, "config.properties")
    }
}


object Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    override fun init() {
        println("Messenger Plugin initialized.")

    }
}
