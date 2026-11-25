package convergence.model

import convergence.Protocol

abstract class Server(val name: String, override val protocol: Protocol): Comparable<Server>, CommandScope {
    override fun hashCode() = toKey().hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Server) return false

        return toKey() == other.toKey()
    }
}
