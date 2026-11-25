package convergence.console

import convergence.Protocol
import convergence.command.Alias
import convergence.model.Chat
import convergence.model.SimpleIncomingMessage
import convergence.model.SimpleOutgoingMessage
import convergence.model.User
import convergence.substringBetween
import java.util.*
import kotlin.system.exitProcess

class ConsoleUser(val name: String): User<ConsoleProtocol>(ConsoleProtocol) {
    override fun toKey() = "ConsoleUser($name)"
    override fun displayName() = name
}
object ConsoleChat: Chat<ConsoleProtocol>(ConsoleProtocol, "Console") {
    override fun toKey() = "ConsoleChat(Console)"
}

val user = ConsoleUser("user")
val bot = ConsoleUser("bot")

object ConsoleProtocol: Protocol<ConsoleChat, ConsoleUser, SimpleIncomingMessage, SimpleOutgoingMessage>("Console") {
    override fun sendMessage(chat: ConsoleChat, message: SimpleOutgoingMessage): Boolean {
        println(message.toSimple().text)
        return true
    }

    override fun userFromKey(key: String): ConsoleUser? {
        if (key.startsWith("ConsoleUser"))
            return ConsoleUser(key.substringBetween("ConsoleUser(", ")"))
        return null
    }

    override fun commandScopeFromKey(key: String) = if (key == "ConsoleChat(Console)") ConsoleChat else null
    override fun getBot(chat: ConsoleChat): ConsoleUser {
        return bot
    }

    override fun getUserName(chat: ConsoleChat, user: ConsoleUser): String {
        return user.name
    }

    override fun getChats(): List<ConsoleChat> {
        return listOf(ConsoleChat)
    }

    override fun getUsers(): List<ConsoleUser> {
        return listOf(user)
    }

    override fun getUsers(chat: ConsoleChat): List<ConsoleUser> {
        return listOf(user)
    }

    override fun getChatName(chat: ConsoleChat): String {
        return "Console"
    }

    override fun outgoingFromSimple(text: String) = SimpleOutgoingMessage(text)

    override fun init() {
        if (System.console() != null) {
            val thread = Thread({
                print("consolePlugin initialized.\n\n> ")

                System.out.flush() // Flush guarantees that the > shows up before stdin. IntelliJ doesn't listen to it.
                try {
                    val stdin = Scanner(System.`in`)
                    val currentLine = stdin.nextLine()
                    receivedMessage(ConsoleChat, user, SimpleIncomingMessage(currentLine))
                    while (true) {
                        print("> ")
                        System.out.flush()
                        while (!stdin.hasNextLine()) stdin.next()
                        receivedMessage(ConsoleChat, user, SimpleIncomingMessage(stdin.nextLine()))
                    }
                } catch(_: NoSuchElementException) {
                    // Catch Ctrl-D (EOF). Normally, I wouldn't do this in a plugin, but it's the local console of the
                    // bot, and if the user puts in a Ctrl-D, they probably want to close the bot, just like a SIGTERM.
                    println() // The newline is just to make the output cleaner.
                    exitProcess(0)
                }
            }, "console-input")
            thread.isDaemon = true
            thread.start()
        }
    }

    override fun configLoaded() {
        // Do nothing
    }

    override fun aliasCreated(alias: Alias) {
        // Do nothing
    }
}
