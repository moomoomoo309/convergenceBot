package convergence

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
class OffsetDateTimeSerializer: KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("OffsetDateTime")

    override fun serialize(encoder: Encoder, obj: OffsetDateTime) {
        encoder.encodeLong(obj.toEpochSecond())
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(decoder.decodeLong()), ZoneId.systemDefault())
    }
}