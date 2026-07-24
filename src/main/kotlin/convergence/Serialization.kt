package convergence

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import convergence.discord.DiscordChat

// Domain objects (Chat/Server/User/CommandScope) can't be serialized directly: they wrap live protocol
// state (JDA channels, etc.). Each one exposes a stable string key via toKey(), and every protocol can
// rebuild the object from that key via commandScopeFromKey()/userFromKey(). The (de)serializers below encode
// that single contract once, so the whole Settings graph — including the nested maps keyed by domain
// objects — round-trips through Jackson.
//
// Keys are self-describing: every key starts with its protocol's name (e.g. "DiscordChat(…)" starts with
// "Discord"), so scopeStrToProtocol() recovers the owning protocol from the key string alone.

private fun resolveScope(key: String): CommandScope =
    bot.scopeStrToProtocol(key)?.commandScopeFromKey(key)
        ?: throw IllegalArgumentException("No protocol could resolve a command scope from key: $key")

private fun resolveUser(key: String): User =
    bot.scopeStrToProtocol(key)?.userFromKey(key)
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

// Value deserializers match by exact declared type, so ReactConfig's destination (a concrete DiscordChat)
// needs its own registration even though serialization is already covered by the Chat value serializer.
object DiscordChatValueDeserializer: JsonDeserializer<DiscordChat>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DiscordChat =
        resolveScope(p.valueAsString) as? DiscordChat
            ?: throw IllegalArgumentException("Key did not resolve to a DiscordChat: ${p.valueAsString}")
}

object UserKeySerializer: JsonSerializer<User>() {
    override fun serialize(value: User, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeFieldName(value.toKey())
}

object UserKeyDeserializer: KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any = resolveUser(key)
}

object UserValueSerializer: JsonSerializer<User>() {
    override fun serialize(value: User, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeString(value.toKey())
}

object UserValueDeserializer: JsonDeserializer<User>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): User = resolveUser(p.valueAsString)
}

// Alias holds a live Command whose resolution is contextual — getCommand(name, scope) needs the alias's
// scope — so it can't be rebuilt from a single self-contained key. Instead, it serializes as a small JSON
// object of its self-contained parts (the scope's key string, the command name, and the args), and the
// deserializer rebuilds the live Command via getCommand() against the resolved scope. The other persisted
// domain types serialize directly: their fields are either plain strings or Chat/User/CommandScope values
// handled by the (de)serializers above.
object AliasSerializer: JsonSerializer<Alias>() {
    override fun serialize(value: Alias, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("scope", value.scope.toKey())
        gen.writeStringField("name", value.name)
        gen.writeStringField("commandName", value.command.name)
        gen.writeArrayFieldStart("args")
        value.args.forEach { gen.writeString(it) }
        gen.writeEndArray()
        gen.writeEndObject()
    }
}

object AliasDeserializer: JsonDeserializer<Alias>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Alias {
        val node = p.readValueAsTree<JsonNode>()
        val scopeKey = node["scope"].asText()
        val scope = resolveScope(scopeKey)
        if (scope !is Chat)
            throw IllegalArgumentException("Alias scope is not a Chat: $scopeKey")
        val name = node["name"].asText()
        val command = getCommand(node["commandName"].asText().lowercase(), scope) as Command
        val args = node["args"].map { it.asText() }
        return Alias(scope, name, command, args)
    }
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

    // Value (de)serializers, used where a Chat/User appears as a field or collection element rather than a
    // map key (e.g. linkedChats' sets, ScheduledCommand.chat/sender).
    addSerializer(Chat::class.java, ScopeValueSerializer)
    addDeserializer(Chat::class.java, ChatValueDeserializer)
    addDeserializer(DiscordChat::class.java, DiscordChatValueDeserializer)

    addKeySerializer(User::class.java, UserKeySerializer)
    addKeyDeserializer(User::class.java, UserKeyDeserializer)
    addSerializer(User::class.java, UserValueSerializer)
    addDeserializer(User::class.java, UserValueDeserializer)
}
