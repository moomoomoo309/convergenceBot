package convergence.model

import convergence.ProtocolRef

sealed interface CommandScope {
    val protocol: ProtocolRef
    fun toKey(): String
}
