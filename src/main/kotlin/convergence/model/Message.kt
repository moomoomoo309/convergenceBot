package convergence.model

abstract class IncomingMessage<OM : OutgoingMessage> {
    abstract fun toSimple(): SimpleIncomingMessage
    abstract fun toOutgoing(): OM
}

class SimpleIncomingMessage(val text: String): IncomingMessage<SimpleOutgoingMessage>() {
    override fun toSimple() = this
    override fun toOutgoing() = SimpleOutgoingMessage(text)
}

abstract class OutgoingMessage {
    abstract fun toSimple(): SimpleOutgoingMessage
}

class SimpleOutgoingMessage(val text: String): OutgoingMessage() {
    override fun toSimple() = this
}
