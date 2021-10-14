package convergence.testPlugins.matterbridgePlugin

import com.squareup.moshi.Moshi
import convergence.*
import org.pf4j.PluginWrapper
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path

object MatterBridgeProtocol: Protocol("MatterBridge")

val inUrl = URL("127.1:4242/api/stream")
val outUrl = URL("127.1:4242/api/message")

val botUsers = mutableMapOf<Chat, User>()
val users = mutableMapOf<Chat, MutableSet<User>>()
val chats = mutableSetOf<Chat>()

class MatterBridgeChat(name: String): Chat(MatterBridgeProtocol, name)
open class MatterBridgeUser(chat: Chat, val name: String): User(chat)

class MatterBridgeBotUser(chat: Chat): MatterBridgeUser(chat, "ConvergenceBot")

object MatterBridgeInterface: BaseInterface {
    override val name = "MatterBridgeInterface"
    override val protocols: List<Protocol> = listOf(MatterBridgeProtocol)

    override fun sendMessage(chat: Chat, message: String): Boolean {
        val connection = outUrl.openConnection() as HttpURLConnection
        val stream = connection.outputStream
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.doInput = true
        connection.addRequestProperty("Content-Type", "application/json")
        stream.write(mapOf(
                "text" to message,
                "username" to getName(chat, getBot(chat)),
                "gateway" to "gateway1"
        ).json().encodeToByteArray())
        connection.connect()
        return connection.responseCode == 200
    }

    override fun getBot(chat: Chat): User = botUsers.getOrPut(chat) { MatterBridgeBotUser(chat) }

    override fun getName(chat: Chat, user: User): String = (user as MatterBridgeUser).name

    override fun getChats(): List<Chat> = chats.toList()

    override fun getUsers(chat: Chat): List<User> = mutableListOf<User>().also {
        users.values.forEach { userSet -> it.addAll(userSet) }
    }.toList()

    override fun getChatName(chat: Chat): String = (chat as MatterBridgeChat).name

}

class MatterbridgePlugin(wrapper: PluginWrapper): Plugin(wrapper) {
    override val name = "MatterBridgePlugin"
    override val baseInterface: BaseInterface = MatterBridgeInterface
    val convergencePath: Path by configuration
    val moshi: Moshi by configuration

    override fun init() {
        val outLog = convergencePath.resolve("matterbridge.log").toFile()
        val errLog = convergencePath.resolve("matterbridgeErr.log").toFile()

        ProcessBuilder()
                .command(convergencePath.resolve("matterbridge").toAbsolutePath().toString())
                .directory(convergencePath.toFile())
                .redirectOutput(outLog)
                .redirectError(errLog)
                .start()

        val messageThread = Thread({
            var connection: HttpURLConnection
            while (true) {
                do {
                    log("Connecting to matterbridge...")
                    connection = inUrl.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.doInput = true
                    connection.addRequestProperty("Content-Type", "application/json")
                    connection.connect()
                } while (connection.responseCode != 200)

                val stream = connection.inputStream
                stream.bufferedReader().lineSequence().forEach {
                    val msg = moshi.fromJson<Map<String, String>>(it)
                    println(msg)
                }
                stream.close()
            }
        }, "Matterbridge Message Thread")
        messageThread.start()
    }

}
