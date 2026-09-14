package convergence

import convergence.command.Alias
import convergence.model.Chat
import convergence.model.OutgoingMessage
import convergence.model.User

object UniversalUser: User(UniversalProtocol) {
    override fun toKey() = "UniversalUser()"
}

object UniversalProtocol: Protocol("Universal") {
    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getUserName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = listOf(UniversalChat)
    override fun getUsers(): List<User> = listOf(UniversalUser)
    override fun getUsers(chat: Chat): List<User> = listOf(UniversalUser)
    override fun getChatName(chat: Chat): String = ""
    override fun commandScopeFromKey(key: String): Chat? {
        if (key == "UniversalChat")
            return UniversalChat
        return null
    }

    override fun userFromKey(key: String): User? {
        if (key == "UniversalUser")
            return UniversalUser
        return null
    }

    override fun init() {
        // Do nothing
    }

    override fun configLoaded() {
        // Do nothing
    }

    override fun aliasCreated(alias: Alias) {
        // Do nothing
    }
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal") {
    override fun toKey() = "UniversalChat"
}
