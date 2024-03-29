package convergence.testPlugins.consolePlugin

import convergence.*
import org.pf4j.PluginWrapper
import java.util.*
import kotlin.system.exitProcess

class ConsoleUser: User(ConsoleChat)
object ConsoleChat: Chat(ConsoleProtocol, "Console")

val user = ConsoleUser()
val bot = ConsoleUser()

object ConsoleProtocol: Protocol("Console")
object ConsoleInterface: BaseInterface {
    override val name = "ConsoleInterface"
    override val protocols: List<Protocol> = listOf(ConsoleProtocol)
    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat is ConsoleChat) {
            println(message)
            return true
        }
        throw InputMismatchException("Invalid chat or user passed. Can only be ConsoleChat and ConsoleUser.")
    }

    override fun getBot(chat: Chat): User {
        return bot
    }

    override fun getName(chat: Chat, user: User): String {
        if (user is ConsoleUser && chat is ConsoleChat)
            return "ConsoleUser"
        throw InputMismatchException("Invalid chat or user passed. Can only be ConsoleChat and ConsoleUser.")
    }

    override fun getChats(): List<Chat> {
        return listOf(ConsoleChat)
    }

    override fun getUsers(chat: Chat): List<User> {
        return listOf(user)
    }

    override fun getChatName(chat: Chat): String {
        return "Console"
    }

}

class ConsolePlugin(wrapper: PluginWrapper): Plugin(wrapper) {
    override val name = "consolePlugin"
    override val baseInterface: BaseInterface = ConsoleInterface
    var currentChatID: Int by sharedVariables
    val chatMap: MutableMap<Int, Chat> by settings
    val reverseChatMap: MutableMap<Chat, Int> by settings
    var chatAdapterFactory: PolymorphicJsonAdapterFactory<Chat> by sharedVariables
    var userAdapterFactory: PolymorphicJsonAdapterFactory<User> by sharedVariables
    override fun init() {
        val id = currentChatID++ // Only one chat, so we can register it right away.
        chatMap[id] = ConsoleChat
        reverseChatMap[ConsoleChat] = id

        // The SingletonAdapterFactory should make these work. If not, it's because this plugin is on a different
        // ClassLoader.
        chatAdapterFactory = chatAdapterFactory.withSubtype(ConsoleChat::class.java, "ConsoleChat")
        userAdapterFactory = userAdapterFactory.withSubtype(ConsoleUser::class.java, "ConsoleUser")

        print("consolePlugin initialized.\n\n> ")

        System.out.flush() // Flush guarantees that the > shows up before stdin. IntelliJ still doesn't listen to it.
        try {
            val stdin = Scanner(System.`in`)
            val currentLine = stdin.nextLine()
            ConsoleInterface.receivedMessage(ConsoleChat, currentLine, user)
            while (true) {
                print("> ")
                System.out.flush()
                while (!stdin.hasNextLine()) stdin.next()
                ConsoleInterface.receivedMessage(ConsoleChat, stdin.nextLine(), user)
            }
        } catch (e: NoSuchElementException) {
            // Catch Ctrl-D (EOF). Normally, I wouldn't do this in a plugin, but it's the local console of the bot,
            // and if the user puts in a Ctrl-D, they probably want to close the bot, just like a SIGTERM.
            println() // The newline is just to make the output cleaner.
            exitProcess(0)
        }
    }
}
