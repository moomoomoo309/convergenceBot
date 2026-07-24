import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import convergence.*
import org.junit.After
import org.junit.Before
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

// A state-free protocol whose scopes/users are fully reconstructable from their keys, so we can exercise the
// real (de)serialization path (convergenceModule + the per-type converters) without any live protocol state.
class SerChat(val cid: Int): Chat(SerProtocol, "chat$cid") {
    override fun toKey() = "SerTestChat($cid)"
    override fun equals(other: Any?) = other is SerChat && other.cid == cid
    override fun hashCode() = cid
}

class SerServer(val sid: Int): Server("srv$sid", SerProtocol) {
    override fun toKey() = "SerTestServer($sid)"
    override fun compareTo(other: Server) =
        if (other is SerServer) sid.compareTo(other.sid) else name.compareTo(other.name)

    override fun equals(other: Any?) = other is SerServer && other.sid == sid
    override fun hashCode() = sid
}

class SerUser(val uid: Int): User(SerProtocol) {
    override fun toKey() = "SerTestUser($uid)"
    override fun equals(other: Any?) = other is SerUser && other.uid == uid
    override fun hashCode() = uid
}

@Suppress("EmptyFunctionBlock")
object SerProtocol: Protocol("SerTest") {
    override fun init() {}
    override fun configLoaded() {}
    override fun aliasCreated(alias: Alias) {}
    override fun sendMessage(chat: Chat, message: OutgoingMessage) = false
    override fun getBot(chat: Chat): User = SerUser(0)
    override fun getUserName(chat: Chat, user: User) = "user"
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(): List<User> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat) = "chat"
    override fun commandScopeFromKey(key: String): CommandScope? = when {
        key.startsWith("SerTestChat(") -> SerChat(key.substringBetween("SerTestChat(", ")").toInt())
        key.startsWith("SerTestServer(") -> SerServer(key.substringBetween("SerTestServer(", ")").toInt())
        else -> null
    }

    override fun userFromKey(key: String): User? =
        if (key.startsWith("SerTestUser(")) SerUser(key.substringBetween("SerTestUser(", ")").toInt()) else null
}

// Mirrors ReactConfig: a data class whose field is declared as a *concrete* Chat subtype rather than Chat.
data class ReactLike(val destination: SerChat, val emojis: MutableMap<String, Int>)

class SerializationTest {
    private val time: OffsetDateTime = OffsetDateTime.of(2026, 6, 13, 10, 0, 0, 0, ZoneOffset.UTC)
    private lateinit var testCommand: Command

    @Before
    fun setup() {
        resetGlobalState()
        if (SerProtocol !in bot.protocols) bot.protocols.add(SerProtocol)
        testCommand = Command.of(SerProtocol, "testcmd", listOf(), { _: List<String> -> null }, "help", "syntax")
        bot.commands[SerProtocol] = mutableMapOf("testcmd" to testCommand)
    }

    @After
    fun teardown() {
        bot.protocols.remove(SerProtocol)
        resetGlobalState()
    }

    private fun sampleData(): Settings {
        val s = Settings()
        s.aliases = mutableMapOf(
            SerChat(1) to mutableMapOf(
                "myalias" to Alias(SerChat(1), "myalias", testCommand, listOf("a", "b"))
            )
        )
        s.commandDelimiters = mutableMapOf(SerChat(1) to "?")
        s.linkedChats = mutableMapOf(SerChat(1) to mutableSetOf(SerChat(2), SerChat(3)))
        s.serializedCommands = mutableMapOf(
            7 to ScheduledCommand(time, SerChat(1), SerUser(5), "SerTest", "testcmd", listOf("x"), 7)
        )
        s.syncedCalendars = mutableListOf(SyncedCalendar(123L, "https://example.com/cal"))
        s.timers = mutableMapOf("t1" to time)
        s.imageUploadChannels = mutableMapOf(SerChat(1) to URI("https://example.com/img"))
        s.mentionChats = mutableMapOf(SerChat(1) to mutableMapOf(SerUser(2) to mutableMapOf(SerUser(3) to 4)))
        s.debugMode = true
        return s
    }

    private fun assertMatches(data: Settings) {
        assertEquals(sampleData().aliases, data.aliases, "aliases did not round-trip")
        assertEquals(sampleData().commandDelimiters, data.commandDelimiters, "commandDelimiters did not round-trip")
        assertEquals(sampleData().linkedChats, data.linkedChats, "linkedChats did not round-trip")
        assertEquals(sampleData().serializedCommands, data.serializedCommands, "serializedCommands did not round-trip")
        assertEquals(sampleData().syncedCalendars, data.syncedCalendars, "syncedCalendars did not round-trip")
        assertEquals(sampleData().timers, data.timers, "timers did not round-trip")
        assertEquals(
            sampleData().imageUploadChannels,
            data.imageUploadChannels,
            "imageUploadChannels did not round-trip"
        )
        assertEquals(sampleData().mentionChats, data.mentionChats, "mentionChats did not round-trip")
        assertEquals(sampleData().debugMode, data.debugMode, "debugMode did not round-trip")
    }

    @Test
    fun roundTripsSettingsData() {
        val json = objectMapper.writeValueAsString(sampleData())
        assertMatches(objectMapper.readValue<Settings>(json))
    }

    @Test
    fun concreteChatSubtypeFieldRoundTrips() {
        // This is the exact mechanism ReactConfig.destination relies on now that ReactConfigDTO is gone: the
        // base-type Chat value serializer must apply to a concrete-subtype-declared field, and a subtype-exact
        // deserializer must rebuild it from the key. (ReactConfig itself can't be exercised offline — a real
        // DiscordChat needs a live JDA channel — so we validate the Jackson behavior with the test protocol.)
        val mapper = ObjectMapper().registerKotlinModule().registerModule(
            SimpleModule().apply {
                addSerializer(Chat::class.java, ScopeValueSerializer)
                addDeserializer(SerChat::class.java, object: JsonDeserializer<SerChat>() {
                    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
                        SerProtocol.commandScopeFromKey(p.valueAsString) as SerChat
                })
            }
        )
        val original = ReactLike(SerChat(9), mutableMapOf("x" to 1))
        val json = mapper.writeValueAsString(original)
        assert(json.contains("\"destination\":\"SerTestChat(9)\"")) { "destination not serialized as key:\n$json" }
        assertEquals(original, mapper.readValue<ReactLike>(json), "concrete-subtype field did not round-trip")
    }

    @Test
    fun scheduledCommandKeepsLegacyFieldNames() {
        // ScheduledCommand serializes directly (no DTO) but must keep the chatKey/senderKey field names so
        // settings files written by the old DTO-based format still load.
        val json = objectMapper.writeValueAsString(sampleData())
        assert(json.contains("\"chatKey\"")) { "expected chatKey in serialized output:\n$json" }
        assert(json.contains("\"senderKey\"")) { "expected senderKey in serialized output:\n$json" }
    }

    @Test
    fun roundTripsThroughSettingsSingleton() {
        // Exercises the exact production path: write the settings object, read back into a Settings instance.
        val data = sampleData()
        settings.aliases.clear()
        settings.commandDelimiters.clear()
        settings.linkedChats.clear()
        settings.serializedCommands.clear()
        settings.syncedCalendars.clear()
        settings.timers.clear()
        settings.imageUploadChannels.clear()
        settings.mentionChats.clear()

        settings.aliases.putAll(data.aliases)
        settings.commandDelimiters.putAll(data.commandDelimiters)
        settings.linkedChats.putAll(data.linkedChats)
        settings.serializedCommands.putAll(data.serializedCommands)
        settings.syncedCalendars.addAll(data.syncedCalendars)
        settings.timers.putAll(data.timers)
        settings.imageUploadChannels.putAll(data.imageUploadChannels)
        settings.mentionChats.putAll(data.mentionChats)
        settings.debugMode = data.debugMode
        val json = objectMapper.writeValueAsString(settings)
        assertMatches(objectMapper.readValue<Settings>(json))
    }
}
