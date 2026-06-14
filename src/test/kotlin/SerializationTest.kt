import convergence.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
    override fun compareTo(other: Server) = if (other is SerServer) sid.compareTo(other.sid) else name.compareTo(other.name)
    override fun equals(other: Any?) = other is SerServer && other.sid == sid
    override fun hashCode() = sid
}

class SerUser(val uid: Int): User(SerProtocol) {
    override fun toKey() = "SerTestUser($uid)"
    override fun equals(other: Any?) = other is SerUser && other.uid == uid
    override fun hashCode() = uid
}

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
        if (SerProtocol !in protocols) protocols.add(SerProtocol)
        testCommand = Command.of(SerProtocol, "testcmd", listOf(), { _: List<String> -> null }, "help", "syntax")
        commands[SerProtocol] = mutableMapOf("testcmd" to testCommand)
    }

    @After
    fun teardown() {
        protocols.remove(SerProtocol)
        commands.remove(SerProtocol)
        Settings.updateFrom(SettingsData()) // reset the global singleton so other tests see a clean slate
    }

    private fun sampleData() = SettingsData(
        aliases = mutableMapOf(
            SerChat(1) to mutableMapOf(
                "myalias" to Alias(SerChat(1), "myalias", testCommand, listOf("a", "b"))
            )
        ),
        commandDelimiters = mutableMapOf(SerChat(1) to "?"),
        linkedChats = mutableMapOf(SerChat(1) to mutableSetOf(SerChat(2), SerChat(3))),
        serializedCommands = mutableMapOf(
            7 to ScheduledCommand(time, SerChat(1), SerUser(5), "SerTest", "testcmd", listOf("x"), 7)
        ),
        syncedCalendars = mutableListOf(SyncedCalendar(123L, "http://example.com/cal")),
        timers = mutableMapOf("t1" to time),
        imageUploadChannels = mutableMapOf(SerChat(1) to URI("http://example.com/img")),
        mentionChats = mutableMapOf(SerChat(1) to mutableMapOf(SerUser(2) to mutableMapOf(SerUser(3) to 4))),
        debugMode = true
    )

    private fun assertMatches(data: SettingsData) {
        assertEquals(sampleData().aliases, data.aliases, "aliases did not round-trip")
        assertEquals(sampleData().commandDelimiters, data.commandDelimiters, "commandDelimiters did not round-trip")
        assertEquals(sampleData().linkedChats, data.linkedChats, "linkedChats did not round-trip")
        assertEquals(sampleData().serializedCommands, data.serializedCommands, "serializedCommands did not round-trip")
        assertEquals(sampleData().syncedCalendars, data.syncedCalendars, "syncedCalendars did not round-trip")
        assertEquals(sampleData().timers, data.timers, "timers did not round-trip")
        assertEquals(sampleData().imageUploadChannels, data.imageUploadChannels, "imageUploadChannels did not round-trip")
        assertEquals(sampleData().mentionChats, data.mentionChats, "mentionChats did not round-trip")
        assertEquals(sampleData().debugMode, data.debugMode, "debugMode did not round-trip")
    }

    @Test
    fun roundTripsSettingsData() {
        val json = objectMapper.writeValueAsString(sampleData())
        assertMatches(objectMapper.readValue<SettingsData>(json))
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
        // Exercises the exact production path: write the Settings object, read back into a SettingsData holder.
        Settings.updateFrom(sampleData())
        val json = objectMapper.writeValueAsString(Settings)
        assertMatches(objectMapper.readValue<SettingsData>(json))
    }
}
