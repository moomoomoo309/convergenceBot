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

