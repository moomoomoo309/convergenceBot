package convergence.testPlugins.consolePlugin

import convergence.*
import java.util.*

object ConsoleUser: User(ConsoleChat)
object ConsoleChat: Chat(ConsoleProtocol, "Console")
object ConsoleProtocol: Protocol("Console")
object ConsoleInterface: BaseInterface {
    override val name: String = "ConsoleInterface"
    override val protocol: Protocol = ConsoleProtocol

    init {
        if (!registerProtocol(this.protocol, this))
            System.err.println("Protocol with name \"$this.protocol.name\" registered more than once!")
    }

    override fun sendMessage(chat: Chat, message: String, sender: User): Boolean {
        if (sender is ConsoleUser && chat is ConsoleChat) {
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

class Main: Plugin {
    override val name = "basicPlugin"
    override val baseInterface: BaseInterface = ConsoleInterface
    override fun init() {
        println("consolePlugin init")
        print("> ")
        System.out.flush()
        val stdin = Scanner(System.`in`)
        val currentLine = stdin.nextLine()
        if (currentLine == "commands")
            println(commands)
        else
            ConsoleInterface.receivedMessage(ConsoleChat, currentLine, ConsoleUser)
        while (true) {
            print("> ")
            System.out.flush()
            while (!stdin.hasNextLine()) stdin.next()
            ConsoleInterface.receivedMessage(ConsoleChat, stdin.nextLine(), ConsoleUser)
        }
    }
}