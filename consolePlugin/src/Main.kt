package convergence.testPlugins.consolePlugin

import convergence.*
import java.util.*

object ConsoleUser: User(ConsoleChat), StringSerializable<ConsoleUser> {
    override fun serialize(): String = "ConsoleUser"
    override fun deserialize(serializedVal: String): ConsoleUser = deserializeSingleton(serializedVal)
}

object ConsoleChat: Chat(ConsoleProtocol, "Console"), StringSerializable<ConsoleChat> {
    override fun serialize(): String = "ConsoleChat"
    override fun deserialize(serializedVal: String): ConsoleChat = deserializeSingleton(serializedVal)
}

object ConsoleProtocol: Protocol("Console"), StringSerializable<ConsoleProtocol> {
    override fun serialize(): String = "ConsoleProtocol"
    override fun deserialize(serializedVal: String): ConsoleProtocol = deserializeSingleton(serializedVal)
}

object ConsoleInterface: BaseInterface {
    override val name: String = "ConsoleInterface"
    override val protocol: Protocol = ConsoleProtocol

    init {
        if (!registerProtocol(this.protocol, this))
            System.err.println("Protocol with name \"$this.protocol.name\" registered more than once!")
        val id = currentChatID++ // Only one chat, so we can register it right away.
        chatMap[id] = ConsoleChat
        reverseChatMap[ConsoleChat] = id
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
            System.exit(0)
        }
    }
}