@file:Suppress("EnumEntryName", "unused")

package convergence.testPlugins.messengerPlugin

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import convergence.*
import io.ktor.util.KtorExperimentalAPI
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.OffsetDateTime

object MessengerProtocol: Protocol("Messenger")

@Suppress("unused")
@KtorExperimentalAPI
object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IStickers, ITypingStatus {
    override val name: String = Main.name
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format> = setOf(Format.code, Format.monospace, Format.underline, Format.bold, Format.italics)
    val formatMap = mapOf(Format.code to Pair("```", "```"),
            Format.monospace to Pair("`", "`"),
            Format.underline to Pair("__", "__"),
            Format.bold to Pair("**", "**"),
            Format.italics to Pair("*", "*"),
            Format.greentext to Pair("> ", "\n"))

    @Suppress("UNCHECKED_CAST")
    override fun getUserNickname(chat: Chat, user: User): String? {
        val info = jsonParser.parse<Map<*, *>>(Api.getThreadInfo((chat as MessengerChat).id))
        return (info!!["nicknames"] as JsonArray<JsonObject>?)?.firstOrNull {
            (it["userid"] as String) == (user as MessengerUser).id
        }?.let { it["nickname"] as String }
    }

    override fun getBotNickname(chat: Chat): String? = getUserNickname(chat, getBot(chat))
    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) =
            Api.sendMessage(message + "\n${(image as MessengerImage).url}", (chat as MessengerChat).id).let { Unit }

    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun sendMessage(chat: Chat, message: String): Boolean = Api.sendMessage(message, (chat as MessengerChat).id).let { true }
    override fun getBot(chat: Chat): User = MessengerUser(Api.getCurrentUserId(), chat as MessengerChat)
    override fun getName(chat: Chat, user: User): String {
        val info = jsonParser.parse<Map<String, String>>(Api.getUserInfo((user as MessengerUser).id))
        return info!!["name"] ?: error("Name JSON note parsed correctly.")
    }

    override fun getChats(): List<Chat> = Api.getThreadList(20, null, arrayOf("INBOX")).let { threads ->
        jsonParser.parse<List<Map<String, *>>>(threads)?.map { MessengerChat(it["name"] as String) } ?: emptyList()
    }

    override fun getUsers(chat: Chat): List<User> = Api.getThreadInfo((chat as MessengerChat).id).let { info ->
        @Suppress("UNCHECKED_CAST")
        (jsonParser.parse<Map<String, JsonArray<String>>>(info)!!["participantIDs"])?.map { MessengerUser(it, chat) }
                ?: emptyList()
    }

    override fun getChatName(chat: Chat): String = Api.getThreadInfo((chat as MessengerChat).id).let {
        @Suppress("UNCHECKED_CAST")
        jsonParser.parse<Map<String, String>>(it)!!.getOrDefault("name", "unnamed")
    }

    override fun sendSticker(chat: Chat, sticker: Sticker) =
            Api.sendMessage((sticker as MessengerSticker).url, (chat as MessengerChat).id).let { Unit }

    override fun setBotTypingStatus(chat: Chat, status: Boolean) = TODO()
}


fun runCommand(command: String, workingDir: File = File(".")): Process? {
    return try {
        val parts = command.split("\\s".toRegex())
        ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

@Suppress("unused")
@KtorExperimentalAPI
object Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface = MessengerInterface
    override fun init() {
        deserializationFunctions["MessengerInterface"] = { MessengerInterface }
        deserializationFunctions["MessengerChat"] = { MessengerChat(it["id"] as String) }
        deserializationFunctions["MessengerUser"] = { MessengerUser(it["id"] as String, deserialize(it["chat"] as String)) }
        println("Messenger Plugin initialized.")
        val facebookCredentials = Paths.get(System.getProperty("user.home"), ".convergence", "facebookCredentials.json")
        if (!Files.exists(facebookCredentials)) {
            Files.write(facebookCredentials, """
                {
                    "email": "",
                    "password": ""
                }
            """.trimIndent().toByteArray())
            println("Put your credentials in facebookCredentials.json!")
            return
        }
        val messengerPath = Paths.get(System.getProperty("user.home"), ".convergence", "messenger")
        try {
            Files.createDirectory(messengerPath)
        } catch (ignored: FileAlreadyExistsException) {
        }

        val hash = (String(jcl.loadedResources["index.js"]!!) + String(jcl.loadedResources["package.json"]!!)).md5()
        val hashPath = messengerPath.resolve("hash")
        if (Files.notExists(hashPath) || String(Files.readAllBytes(hashPath)) != hash) {
            Files.write(hashPath, hash.toByteArray())
            Files.write(messengerPath.resolve("index.js"), jcl.loadedResources["index.js"]!!, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            Files.write(messengerPath.resolve("package.json"), jcl.loadedResources["package.json"]!!, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            runCommand("npm i --production", messengerPath.toFile())?.waitFor()
        }
        runCommand("node ${messengerPath.toFile().absolutePath}/index.js")
        Thread.sleep(5000)
        Api.listen()
    }
}

