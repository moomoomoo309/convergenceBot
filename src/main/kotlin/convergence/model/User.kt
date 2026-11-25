package convergence.model

import convergence.ProtocolRef

abstract class User<P : ProtocolRef>(val protocol: P) {
    abstract fun toKey(): String
    open fun displayName(): String = toKey()
}
