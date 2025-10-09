package convergence.console

import convergence.*
import java.util.*
import kotlin.system.exitProcess

class ConsoleUser(val name: String): User(ConsoleProtocol) {
    override fun toKey() = "ConsoleUser($name)"
}
object ConsoleChat: Chat(ConsoleProtocol, "Console") {
    override fun toKey() = "ConsoleChat(Console)"
}

val user = ConsoleUser("user")
val bot = ConsoleUser("bot")

object ConsoleProtocol: Protocol("Console") {
    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean {
        if (chat is ConsoleChat) {
            println(message.toSimple().text)
            return true
        }
        throw InputMismatchException("Invalid chat or user passed. Can only be ConsoleChat and ConsoleUser.")
    }

    override fun userFromKey(key: String): User? {
        if (key.startsWith("ConsoleUser"))
            return ConsoleUser(key.substringBetween("ConsoleUser(", ")"))
        return null
    }

    override fun commandScopeFromKey(key: String) = if (key == "ConsoleChat(Console)") ConsoleChat else null
    override fun getBot(chat: Chat): User {
        return bot
    }

    override fun getUserName(chat: Chat, user: User): String {
        if (user is ConsoleUser && chat is ConsoleChat)
            return "ConsoleUser"
        throw InputMismatchException("Invalid chat or user passed. Can only be ConsoleChat and ConsoleUser.")
    }

    override fun getChats(): List<Chat> {
        return listOf(ConsoleChat)
    }

    override fun getUsers(): List<User> {
        return listOf(user)
    }

    override fun getUsers(chat: Chat): List<User> {
        return listOf(user)
    }

    override fun getChatName(chat: Chat): String {
        return "Console"
    }

    override fun init() {
        if (System.console() != null) {
            Thread {
                print("consolePlugin initialized.\n\n> ")

                System.out.flush() // Flush guarantees that the > shows up before stdin. IntelliJ doesn't listen to it.
                try {
                    val stdin = Scanner(System.`in`)
                    val currentLine = stdin.nextLine()
                    receivedMessage(ConsoleChat, SimpleIncomingMessage(currentLine), user)
                    while (true) {
                        print("> ")
                        System.out.flush()
                        while (!stdin.hasNextLine()) stdin.next()
                        receivedMessage(ConsoleChat, SimpleIncomingMessage(stdin.nextLine()), user)
                    }
                } catch(_: NoSuchElementException) {
                    // Catch Ctrl-D (EOF). Normally, I wouldn't do this in a plugin, but it's the local console of the
                    // bot, and if the user puts in a Ctrl-D, they probably want to close the bot, just like a SIGTERM.
                    println() // The newline is just to make the output cleaner.
                    exitProcess(0)
                }
            }.start()
        }
    }

    override fun configLoaded() {
        // Do nothing
    }

    override fun aliasCreated(alias: Alias) {
        // Do nothing
    }
}
