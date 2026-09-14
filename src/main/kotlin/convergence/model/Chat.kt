package convergence.model

import convergence.Protocol


abstract class Chat(override val protocol: Protocol, val name: String): Comparable<Chat>, CommandScope {
    override fun compareTo(other: Chat) =
        "${protocol.name}-${this.name}".compareTo("${other.protocol.name}-${other.name}")

    override fun toString(): String {
        return "${this::class.java.simpleName}($name)"
    }
}
