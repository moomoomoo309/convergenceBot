package convergence.model

import java.io.InputStream
import java.net.URI
import java.time.OffsetDateTime

abstract class Image {
    abstract fun getURL(): URI
    abstract fun getStream(): InputStream
    fun getBytes(): ByteArray = getStream().readAllBytes()
}

abstract class Sticker(val name: String, val url: String?)

abstract class Availability(val description: String)

abstract class MessageHistory(var message: IncomingMessage, val timestamp: OffsetDateTime, val sender: User)

abstract class Role(val name: String)
