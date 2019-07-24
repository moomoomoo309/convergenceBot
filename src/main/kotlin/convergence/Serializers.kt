package convergence

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.time.OffsetDateTime

@Serializer(OffsetDateTime::class)
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("OffsetDateTime")

    override fun serialize(encoder: Encoder, obj: OffsetDateTime) = encoder.encodeString(obj.toString())
    override fun deserialize(decoder: Decoder): OffsetDateTime = OffsetDateTime.parse(decoder.decodeString())
}

@Serializer(Command::class)
object CommandSerializer: KSerializer<Command> {
    override val descriptor = StringDescriptor.withName("Command")

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, obj: Command) {
        encoder.encode(obj.chat)
        encoder.encodeString(obj.name)
    }

    @ImplicitReflectionSerializer
    override fun deserialize(decoder: Decoder): Command {
        val chat = decoder.decode(Chat::class.serializer())
        return commands[chat]!![decoder.decodeString()]!!
    }
}

