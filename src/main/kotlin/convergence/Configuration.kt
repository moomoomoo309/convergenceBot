package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import convergence.discord.DiscordChat
import convergence.discord.jda
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

const val DEFAULT_COMMAND_DELIMITER = "!"

data class SyncedCalendar(val guildId: Long, val calURL: String) {
    override fun toString(): String {
        return "SyncedCalendar(guild=${jda.getGuildById(guildId)?.name ?: guildId}, calURL=$calURL)"
    }
}

data class AliasDTO(
    val scopeName: String,
    val name: String,
    val commandName: String,
    val protocolName: String,
    val args: List<String>
) {
    fun toAlias(): Alias {
        val protocol = protocols.first { protocolName == it.name }
        val scope = protocol.commandScopeFromKey(scopeName) as Chat
        val command = getCommand(commandName.lowercase(), scope) as Command
        return Alias(scope, name, command, args)
    }
}

// destination persists as its key string (DiscordChat is serialized by the Chat value serializer and rebuilt
// by the DiscordChat value deserializer in convergenceModule); emojis is a plain map Jackson handles natively.
data class ReactConfig(val destination: DiscordChat, val emojis: MutableMap<String, Int>)

internal fun scopeStrToProtocol(s: String) = protocols.sortedBy { -it.name.length }
    .firstOrNull { s.substringBefore("(").startsWith(it.name) }

// Mirror of Settings used purely as a (de)serialization target — Settings itself is a singleton object,
// which Jackson can't instantiate. The domain-object keys/values round-trip via convergenceModule and the
// per-type converters, so this holder needs no string-keyed mirror or hand-written conversion logic.
data class SettingsData(
    var aliases: MutableMap<CommandScope, MutableMap<String, Alias>> = mutableMapOf(),
    var commandDelimiters: MutableMap<CommandScope, String> = mutableMapOf(),
    var linkedChats: MutableMap<Chat, MutableSet<Chat>> = mutableMapOf(),
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf(),
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf(),
    var timers: MutableMap<String, OffsetDateTime> = mutableMapOf(),
    var imageUploadChannels: MutableMap<Chat, URI> = mutableMapOf(),
    var reactServers: MutableMap<Server, MutableList<ReactConfig>> = mutableMapOf(),
    var mentionChats: MutableMap<Chat, MutableMap<User, MutableMap<User, Int>>> = mutableMapOf(),
    var debugMode: Boolean = false
)

object Settings {
    var aliases: MutableMap<CommandScope, MutableMap<String, Alias>> = mutableMapOf()
    var commandDelimiters: MutableMap<CommandScope, String> = mutableMapOf()
    var linkedChats: MutableMap<Chat, MutableSet<Chat>> = mutableMapOf()
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf()
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf()
    var timers: MutableMap<String, OffsetDateTime> = mutableMapOf()
    var imageUploadChannels: MutableMap<Chat, URI> = mutableMapOf()
    var reactServers: MutableMap<Server, MutableList<ReactConfig>> = TreeMap()
    var mentionChats: MutableMap<Chat, MutableMap<User, MutableMap<User, Int>>> = mutableMapOf()
    var debugMode: Boolean = false

    @JsonIgnore
    private val updateIsScheduled = AtomicBoolean(false)

    @JsonIgnore
    private val updateExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "settings-writer").apply { isDaemon = true }
    }

    fun update() {
        // Don't update the settings multiple times per second, limit it to one per second.
        // compareAndSet ensures only one write is ever scheduled even under concurrent callers, and
        // the shared single-thread executor avoids spawning a fresh thread per write.
        if (updateIsScheduled.compareAndSet(false, true)) {
            updateExecutor.schedule({
                settingsLogger.info("Updating settings file...")
                writeSettingsToFile()
                updateIsScheduled.set(false)
            }, 1000L, TimeUnit.MILLISECONDS)
        }
    }

    // Replace the live settings in place from a freshly-deserialized holder. The element conversions
    // (domain object <-> key string, and the Alias <-> AliasDTO converter) are all handled by Jackson via
    // convergenceModule, so this is a straight field-by-field copy.
    fun updateFrom(data: SettingsData) {
        aliases.clearThen().putAll(data.aliases)
        commandDelimiters.clearThen().putAll(data.commandDelimiters)
        linkedChats.clearThen().putAll(data.linkedChats)
        serializedCommands.clearThen().putAll(data.serializedCommands)
        syncedCalendars.clearThen().addAll(data.syncedCalendars)
        timers.clearThen().putAll(data.timers)
        imageUploadChannels.clearThen().putAll(data.imageUploadChannels)
        reactServers.clearThen().putAll(data.reactServers)
        mentionChats.clearThen().putAll(data.mentionChats)
        debugMode = data.debugMode
    }
}


private fun writeSettingsToFile() {
    settingsLogger.info("Writing settings to ${settingsPath}:")
    val settingsAsString = objectMapper.writeValueAsString(Settings)
    settingsPath.toFile().writeText(settingsAsString)
    settingsLogger.info("Settings written.")
}

fun readSettings() {
    try {
        val settings = objectMapper.readValue<SettingsData>(settingsPath.toFile())
        Settings.updateFrom(settings)
        writeSettingsToFile()
    } catch(_: java.io.FileNotFoundException) {
        writeSettingsToFile()
    } catch(e: Throwable) {
        settingsLogger.error("Error occurred while reading settings from $settingsPath.\n\tError: ", e)
        exitProcess(1)
    }
}

val commandDelimiters = Settings.commandDelimiters
val linkedChats = Settings.linkedChats
val aliases = Settings.aliases
val serializedCommands = Settings.serializedCommands
val syncedCalendars = Settings.syncedCalendars
val timers = Settings.timers
val imageUploadChannels: MutableMap<Chat, URI> = Settings.imageUploadChannels
val reactServers: MutableMap<Server, MutableList<ReactConfig>> = Settings.reactServers
val mentionChats: MutableMap<Chat, MutableMap<User, MutableMap<User, Int>>> = Settings.mentionChats
val debugMode get() = Settings.debugMode

lateinit var convergencePath: Path
val chatMap: MutableMap<Int, Chat> = mutableMapOf()
val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
val commands: MutableMap<Protocol, MutableMap<String, Command>> = mutableMapOf()
val protocols: MutableList<Protocol> = mutableListOf()
var currentChatID: Int = 0
val aliasVars: MutableMap<String, (chat: Chat, sender: User) -> String?> = mutableMapOf(
    "%sender" to { c: Chat, s: User -> c.protocol.getUserName(c, s) },
    "%nick" to { c, s -> (c.protocol as? HasNicknames)?.getUserNickname(c, s) },
    "%botname" to { c: Chat, _: User -> c.protocol.getUserName(c, c.protocol.getBot(c)) },
    "%chatname" to { c: Chat, _: User -> c.protocol.getChatName(c) }
)
val objectMapper: ObjectMapper = ObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)
    .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true)
    .registerKotlinModule()
    .registerModule(convergenceModule)
    .findAndRegisterModules()
val settingsPath: Path by lazy { convergencePath.resolve("settings.json") }
