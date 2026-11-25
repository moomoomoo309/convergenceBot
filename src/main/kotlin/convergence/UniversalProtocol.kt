package convergence

import convergence.command.Alias
import convergence.model.Chat
import convergence.model.SimpleIncomingMessage
import convergence.model.SimpleOutgoingMessage
import convergence.model.User

object UniversalUser: User<UniversalProtocol>(UniversalProtocol) {
    override fun toKey() = "UniversalUser()"
    override fun displayName() = "Universal User"
}

object UniversalProtocol: Protocol<UniversalChat, UniversalUser, SimpleIncomingMessage, SimpleOutgoingMessage>("Universal") {
    override fun sendMessage(chat: UniversalChat, message: SimpleOutgoingMessage): Boolean = false
    override fun getBot(chat: UniversalChat): UniversalUser = UniversalUser
    override fun getUserName(chat: UniversalChat, user: UniversalUser): String = ""
    override fun getChats(): List<UniversalChat> = listOf(UniversalChat)
    override fun getUsers(): List<UniversalUser> = listOf(UniversalUser)
    override fun getUsers(chat: UniversalChat): List<UniversalUser> = listOf(UniversalUser)
    override fun getChatName(chat: UniversalChat): String = ""
    override fun outgoingFromSimple(text: String) = SimpleOutgoingMessage(text)
    override fun commandScopeFromKey(key: String): Chat<*>? {
        if (key == "UniversalChat")
            return UniversalChat
        return null
    }

    override fun userFromKey(key: String): UniversalUser? {
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
object UniversalChat: Chat<UniversalProtocol>(UniversalProtocol, "Universal") {
    override fun toKey() = "UniversalChat"
}
