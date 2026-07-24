package convergence.model

import convergence.Protocol

abstract class User(val protocol: Protocol) {
    abstract fun toKey(): String
}
