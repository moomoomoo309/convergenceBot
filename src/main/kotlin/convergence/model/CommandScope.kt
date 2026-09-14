package convergence.model

import convergence.Protocol

sealed interface CommandScope {
    val protocol: Protocol
    fun toKey(): String
}
