package convergence.model

import convergence.ProtocolRef

abstract class Server<P : ProtocolRef>(val name: String, override val protocol: P): Comparable<Server<*>>, CommandScope {
    override fun hashCode() = toKey().hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Server<*>) return false

        return toKey() == other.toKey()
    }
}
