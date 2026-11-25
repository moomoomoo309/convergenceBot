package convergence.model

import convergence.ProtocolRef


abstract class Chat<P : ProtocolRef>(override val protocol: P, val name: String): Comparable<Chat<*>>, CommandScope {
    override fun compareTo(other: Chat<*>) =
        "${protocol.name}-${this.name}".compareTo("${other.protocol.name}-${other.name}")

    fun sendMessage(message: OutgoingMessage): Boolean = protocol.sendMessageTo(this, message)
    fun sendMessage(message: String): Boolean = protocol.sendMessageTo(this, SimpleOutgoingMessage(message))
    fun getUsers(): List<User<*>> = protocol.getUsersOf(this)
    fun getUserName(user: User<*>): String = protocol.getUserNameFor(this, user)
    fun getChatName(): String = protocol.getChatNameOf(this)
    fun getBot(): User<*> = protocol.getBotOf(this)

    override fun toString(): String {
        return "${this::class.java.simpleName}($name)"
    }

    override fun equals(other: Any?) = other is Chat<*> && toKey() == other.toKey()
    override fun hashCode() = toKey().hashCode()
}
