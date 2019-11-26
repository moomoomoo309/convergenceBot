package convergence.testPlugins.consolePlugin

import convergence.*
import java.util.*
import kotlin.system.exitProcess

object ConsoleUser: User(ConsoleChat)
object ConsoleChat: Chat(ConsoleProtocol, "Console")
object ConsoleProtocol: Protocol("Console")
object ConsoleInterface: BaseInterface {
    override val name = "ConsoleInterface"
    override val protocol: Protocol = ConsoleProtocol

    init {
        val id = currentChatID++ // Only one chat, so we can register it right away.
        chatMap[id] = ConsoleChat
        reverseChatMap[ConsoleChat] = id
    }

    override fun sendMessage(chat: Chat, message: String): Boolean {
        if (chat is ConsoleChat) {
            println(message)
            return true
        }
        throw InputMismatchException("Invalid chat or user passed. Can only be ConsoleChat and ConsoleUser.")
    }

    override fun getBot(chat: Chat): User {
        return ConsoleUser
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
        return listOf(ConsoleUser)
    }

    override fun getChatName(chat: Chat): String {
        return "Console"
    }

}

object Main: Plugin {
    override val name = "consolePlugin"
    override val baseInterface: BaseInterface = ConsoleInterface
    override fun init() {
        print("consolePlugin initialized.\n\n> ")
        System.out.flush() // Flush guarantees that the > shows up before stdin. IntelliJ still doesn't listen to it.
        try {
            val stdin = Scanner(System.`in`)
            val currentLine = stdin.nextLine()
            ConsoleInterface.receivedMessage(ConsoleChat, currentLine, ConsoleUser)
            while (true) {
                print("> ")
                System.out.flush()
                while (!stdin.hasNextLine()) stdin.next()
                ConsoleInterface.receivedMessage(ConsoleChat, stdin.nextLine(), ConsoleUser)
            }
        } catch (e: NoSuchElementException) {
            // Catch Ctrl-D (EOF). Normally, I wouldn't do this in a plugin, but it's the local console of the bot,
            // and if the user puts in a Ctrl-D, they probably want to close the bot, just like a SIGTERM.
            println() // The newline is just to make the output cleaner.
            exitProcess(0)
        }
    }
}