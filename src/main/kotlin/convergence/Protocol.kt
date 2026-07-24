package convergence

import convergence.command.Alias
import convergence.model.*

abstract class Protocol(val name: String): Comparable<Protocol> {
    abstract fun init()
    abstract fun configLoaded()
    abstract fun aliasCreated(alias: Alias)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol) return false

        if (name != other.name) return false

        return true
    }

    override fun compareTo(other: Protocol) = this.name.compareTo(other.name)
    override fun toString(): String = this::class.java.simpleName
    override fun hashCode(): Int = name.hashCode()

    fun receivedMessage(chat: Chat, message: IncomingMessage, sender: User) = bot.messageCallbacks.forEach {
        it(chat, message, sender)
    }
    abstract fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean
    fun sendMessage(chat: Chat, message: String) = sendMessage(chat, SimpleOutgoingMessage(message))

    abstract fun getChats(): List<Chat>
    abstract fun getBot(chat: Chat): User

    abstract fun getUsers(): List<User>
    abstract fun getUsers(chat: Chat): List<User>

    abstract fun getUserName(chat: Chat, user: User): String
    abstract fun getChatName(chat: Chat): String

    abstract fun commandScopeFromKey(key: String): CommandScope?
    abstract fun userFromKey(key: String): User?
}

typealias MessageCallback = (chat: Chat, message: IncomingMessage, sender: User) -> Unit
