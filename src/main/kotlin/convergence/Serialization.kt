package convergence

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.util.StdConverter

// Domain objects (Chat/Server/User/CommandScope) can't be serialized directly: they wrap live protocol
// state (JDA channels, etc.). Each one exposes a stable string key via toKey(), and every protocol can
// rebuild the object from that key via commandScopeFromKey()/userFromKey(). The (de)serializers below encode
// that single contract once, so the whole Settings graph — including the nested maps keyed by domain
// objects — round-trips through Jackson without a hand-written DTO mirror of the settings structure.
//
// Keys are self-describing: every key starts with its protocol's name (e.g. "DiscordChat(…)" starts with
// "Discord"), so scopeStrToProtocol() recovers the owning protocol from the key string alone.

private fun resolveScope(key: String): CommandScope =
    scopeStrToProtocol(key)?.commandScopeFromKey(key)
        ?: throw IllegalArgumentException("No protocol could resolve a command scope from key: $key")

private fun resolveUser(key: String): User =
    scopeStrToProtocol(key)?.userFromKey(key)
        ?: throw IllegalArgumentException("No protocol could resolve a user from key: $key")

object ScopeKeySerializer: JsonSerializer<CommandScope>() {
    override fun serialize(value: CommandScope, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeFieldName(value.toKey())
}

object ScopeValueSerializer: JsonSerializer<CommandScope>() {
    override fun serialize(value: CommandScope, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeString(value.toKey())
}

object ScopeKeyDeserializer: KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any = resolveScope(key)
}

object ChatValueDeserializer: JsonDeserializer<Chat>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Chat =
        resolveScope(p.valueAsString) as? Chat
            ?: throw IllegalArgumentException("Key did not resolve to a Chat: ${p.valueAsString}")
}

object UserKeySerializer: JsonSerializer<User>() {
    override fun serialize(value: User, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeFieldName(value.toKey())
}

object UserKeyDeserializer: KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any = resolveUser(key)
}

// Alias/ScheduledCommand/ReactConfig hold live Command/Chat references that can't be reconstructed from a
// single key, so they round-trip through their existing DTO mirrors via Jackson converters. This reuses the
// same toDTO()/toAlias()/toConfig()/toScheduledCommand() logic the rest of the code already relies on.
class AliasToDTOConverter: StdConverter<Alias, AliasDTO>() {
    override fun convert(value: Alias): AliasDTO = value.toDTO()
}

class DTOToAliasConverter: StdConverter<AliasDTO, Alias>() {
    override fun convert(value: AliasDTO): Alias = value.toAlias()
}

class ScheduledCommandToDTOConverter: StdConverter<ScheduledCommand, ScheduledCommandDTO>() {
    override fun convert(value: ScheduledCommand): ScheduledCommandDTO = value.toDTO()
}

class DTOToScheduledCommandConverter: StdConverter<ScheduledCommandDTO, ScheduledCommand>() {
    override fun convert(value: ScheduledCommandDTO): ScheduledCommand = value.toScheduledCommand()
        ?: throw IllegalArgumentException("No protocol could resolve scheduled command: ${value.protocolName}")
}

class ReactConfigToDTOConverter: StdConverter<ReactConfig, ReactConfigDTO>() {
    override fun convert(value: ReactConfig): ReactConfigDTO = value.toDTO()
}

class DTOToReactConfigConverter: StdConverter<ReactConfigDTO, ReactConfig>() {
    override fun convert(value: ReactConfigDTO): ReactConfig = value.toConfig()
}

val convergenceModule: SimpleModule = SimpleModule("ConvergenceDomain").apply {
    // Key (de)serializers. Key deserializers match by exact type only, so Chat and Server are registered
    // alongside the CommandScope base type even though they share the same resolution logic.
    addKeySerializer(CommandScope::class.java, ScopeKeySerializer)
    addKeySerializer(Chat::class.java, ScopeKeySerializer)
    addKeySerializer(Server::class.java, ScopeKeySerializer)
    addKeyDeserializer(CommandScope::class.java, ScopeKeyDeserializer)
    addKeyDeserializer(Chat::class.java, ScopeKeyDeserializer)
    addKeyDeserializer(Server::class.java, ScopeKeyDeserializer)

    // Value (de)serializers, used where a Chat appears as a collection element (e.g. linkedChats' sets).
    addSerializer(Chat::class.java, ScopeValueSerializer)
    addDeserializer(Chat::class.java, ChatValueDeserializer)

    addKeySerializer(User::class.java, UserKeySerializer)
    addKeyDeserializer(User::class.java, UserKeyDeserializer)
}
