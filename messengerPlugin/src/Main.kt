package convergence.testPlugins.messengerPlugin

import convergence.*
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import javax.script.Invocable
import javax.script.ScriptEngineManager
import kotlin.reflect.jvm.jvmName

object MessengerProtocol: Protocol("Messenger")

object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IReadStatus, IStickers {
    override fun getReadStatus(chat: Chat, message: MessageHistory): Set<User> {
        TODO()
    }

    override fun setRead(chat: Chat, message: MessageHistory, user: User) {
        TODO()
    }

    override val name: String = Main.name
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format> = setOf(Format.code, Format.monospace, Format.underline, Format.bold, Format.italics)
    val formatMap = mapOf(Format.code to Pair("```", "```"),
            Format.monospace to Pair("`", "`"),
            Format.underline to Pair("__", "__"),
            Format.bold to Pair("**", "**"),
            Format.italics to Pair("*", "*"))

    override fun getUserNickname(chat: Chat, user: User): String? = this.getName(chat, user)

    override fun getBotNickname(chat: Chat): String? = TODO()
    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun receivedImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun sendMessage(chat: Chat, message: String): Boolean = TODO()
    override fun getBot(chat: Chat): User = TODO()
    override fun getName(chat: Chat, user: User): String = TODO()

    override fun getChats(): List<Chat> = TODO()
    override fun getUsers(chat: Chat): List<User> = TODO()

    override fun getChatName(chat: Chat): String = TODO()

    override fun sendSticker(chat: Chat, sticker: Sticker) = TODO()
    override fun receivedSticker(chat: Chat, sticker: Sticker) = TODO()
}


object Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    val engine = ScriptEngineManager().getEngineByName("nashorn")
    override fun init() {
        println("Messenger Plugin initialized.")

        val context = engine.context
        println(context.scopes)
        println(jcl.loadedResources)
        engine.eval(String(jcl.loadedResources["facebookApi.js"]!!))
        context.setAttribute("username", "", 200)
        context.setAttribute("password", "", 200)
        engine.eval(String(jcl.loadedResources["index.js"]!!))
        val invocable = engine as Invocable

    }
}
