package convergence.model

abstract class IncomingMessage {
    abstract fun toSimple(): SimpleIncomingMessage
    abstract fun toOutgoing(): OutgoingMessage
}

class SimpleIncomingMessage(val text: String): IncomingMessage() {
    override fun toSimple() = this
    override fun toOutgoing() = SimpleOutgoingMessage(text)
}

abstract class OutgoingMessage {
    abstract fun toSimple(): SimpleOutgoingMessage
}

class SimpleOutgoingMessage(val text: String): OutgoingMessage() {
    override fun toSimple() = this
}
