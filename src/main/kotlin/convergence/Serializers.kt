package convergence

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.time.OffsetDateTime

@Serializer(OffsetDateTime::class)
object OffsetDateTimeSerializer: KSerializer<OffsetDateTime> {
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

@Serializer(Alias::class)
object AliasSerializer: KSerializer<Alias> {
    override val descriptor = StringDescriptor.withName("Alias")

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, obj: Alias) {
        encoder.encode(obj.chat)
        encoder.encode(obj.name)
    }

    @ImplicitReflectionSerializer
    override fun deserialize(decoder: Decoder): Alias {
        val chat = decoder.decode(Chat::class.serializer())
        return aliases[chat]!![decoder.decodeString()]!!
    }
}

object UniversalUserSerializer: KSerializer<UniversalUser> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("UniversalUser")
    override fun deserialize(decoder: Decoder): UniversalUser = UniversalUser
    override fun serialize(encoder: Encoder, obj: UniversalUser) = encoder.encodeString("UniversalUser")
}

object UniversalChatSerializer: KSerializer<UniversalChat> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("UniversalChat")
    override fun deserialize(decoder: Decoder): UniversalChat = UniversalChat
    override fun serialize(encoder: Encoder, obj: UniversalChat) = encoder.encodeString("UniversalChat")
}
