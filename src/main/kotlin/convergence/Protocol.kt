package convergence

import convergence.command.Alias
import convergence.model.*

/**
 * The non-generic view of a protocol that [Chat], [Server], [User] and
 * [CommandScope] can point at. This exists to break the circular type-parameter
 * bound between Protocol and its model types (the JVM forbids bounds that
 * expand infinitely), and it carries only the base-typed operations that
 * polymorphic callers need.
 */
interface ProtocolRef {
    val name: String
    fun aliasCreated(alias: Alias)

    fun sendMessageTo(chat: Chat<*>, message: OutgoingMessage): Boolean
    fun getUsersOf(chat: Chat<*>): List<User<*>>
    fun getUserNameFor(chat: Chat<*>, user: User<*>): String
    fun getChatNameOf(chat: Chat<*>): String
    fun getBotOf(chat: Chat<*>): User<*>
}

abstract class Protocol<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>, OM : OutgoingMessage>(override val name: String): ProtocolRef, Comparable<Protocol<*, *, *, *>> {
    abstract fun init()
    abstract fun configLoaded()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol<*, *, *, *>) return false

        if (name != other.name) return false

        return true
    }

    override fun compareTo(other: Protocol<*, *, *, *>) = this.name.compareTo(other.name)
    override fun toString(): String = this::class.java.simpleName
    override fun hashCode(): Int = name.hashCode()

    /**
     * Casts a chat that is guaranteed (by an identity check against this protocol)
     * to be one of this protocol's own chats.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun own(chat: Chat<*>): C {
        if (chat.protocol !== this)
            throw IllegalArgumentException("Chat ${chat.name} belongs to protocol ${chat.protocol.name}, not $name")
        return chat as C
    }

    @Suppress("UNCHECKED_CAST")
    protected fun ownUser(user: User<*>): U {
        if (user.protocol !== this)
            throw IllegalArgumentException("User ${user.toKey()} belongs to protocol ${user.protocol.name}, not $name")
        return user as U
    }

    fun receivedMessage(chat: C, sender: U, message: IM, images: Array<out Image> = emptyArray()) {
        bot.messageCallbacks.forEach {
            it(chat, sender, message, images)
        }
    }

    abstract fun sendMessage(chat: C, message: OM): Boolean
    fun sendMessage(chat: C, message: String): Boolean = sendMessage(chat, outgoingFromSimple(message))
    protected abstract fun outgoingFromSimple(text: String): OM

    abstract fun getChats(): List<C>
    abstract fun getBot(chat: C): U

    abstract fun getUsers(): List<U>
    abstract fun getUsers(chat: C): List<U>

    abstract fun getUserName(chat: C, user: U): String
    abstract fun getChatName(chat: C): String

    abstract fun commandScopeFromKey(key: String): CommandScope?
    abstract fun userFromKey(key: String): U?

    // Base-typed bridges for polymorphic callers holding a Chat<*>/User<*>.
    // Protocols may override these to preserve rich message data.

    override fun sendMessageTo(chat: Chat<*>, message: OutgoingMessage): Boolean {
        if (chat.protocol !== this) return false
        return sendMessage(own(chat), outgoingFromSimple(message.toSimple().text))
    }

    override fun getUsersOf(chat: Chat<*>): List<User<*>> =
        if (chat.protocol !== this) emptyList() else getUsers(own(chat))

    override fun getUserNameFor(chat: Chat<*>, user: User<*>): String =
        if (chat.protocol === this && user.protocol === this) getUserName(own(chat), ownUser(user))
        else user.displayName()

    override fun getChatNameOf(chat: Chat<*>): String =
        if (chat.protocol === this) getChatName(own(chat)) else chat.protocol.getChatNameOf(chat)

    override fun getBotOf(chat: Chat<*>): User<*> =
        if (chat.protocol === this) getBot(own(chat)) else chat.protocol.getBotOf(chat)
}
