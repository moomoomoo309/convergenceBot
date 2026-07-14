package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
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

data class CalendarNotificationChannel(
    val guildId: Long,
    val channelId: Long,
    val calURL: String,
    val mentions: MutableMap<Long, String>
) {
    @get:JsonIgnore
    @delegate:JsonIgnore
    val regexes : MutableMap<String, Regex> by lazy { mutableMapOf() }
    override fun toString(): String {
        val guildName = jda.getGuildById(guildId)?.name ?: guildId
        val channelName = jda.getGuildChannelById(channelId)?.name ?: channelId
        val mention = if (mentions.isEmpty()) ""
            else " (mention IDs: ${mentions.toList().joinToString(", ") { 
                "${it.first}${if (it.second.isEmpty()) "" else ": ${it.second}" }"
            }}"
        return "CalendarNotificationChannel(guild=$guildName, channel=$channelName, calURL=$calURL$mention)"
    }
}

// destination persists as its key string (DiscordChat is serialized by the Chat value serializer and rebuilt
// by the DiscordChat value deserializer in convergenceModule); emojis is a plain map Jackson handles natively.
data class ReactConfig(val destination: DiscordChat, val emojis: MutableMap<String, Int>)

internal fun scopeStrToProtocol(s: String) = protocols.sortedBy { -it.name.length }
    .firstOrNull { s.substringBefore("(").startsWith(it.name) }

data class Settings(
    var aliases: MutableMap<CommandScope, MutableMap<String, Alias>> = mutableMapOf(),
    var commandDelimiters: MutableMap<CommandScope, String> = mutableMapOf(),
    var linkedChats: MutableMap<Chat, MutableSet<Chat>> = mutableMapOf(),
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf(),
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf(),
    var notificationChannels: MutableList<CalendarNotificationChannel> = mutableListOf(),
    var timers: MutableMap<String, OffsetDateTime> = mutableMapOf(),
    var imageUploadChannels: MutableMap<Chat, URI> = mutableMapOf(),
    var reactServers: MutableMap<Server, MutableList<ReactConfig>> = TreeMap(),
    var mentionChats: MutableMap<Chat, MutableMap<User, MutableMap<User, Int>>> = mutableMapOf(),
    var debugMode: Boolean = false
)

val settings = Settings()

@JsonIgnore
private val updateIsScheduled = AtomicBoolean(false)

@JsonIgnore
private val updateExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "settings-writer").apply { isDaemon = true }
}

fun updateSettings() {
    if (updateIsScheduled.compareAndSet(false, true)) {
        updateExecutor.schedule({
            settingsLogger.info("Updating settings file...")
            writeSettingsToFile()
            updateIsScheduled.set(false)
        }, 1000L, TimeUnit.MILLISECONDS)
    }
}

private fun writeSettingsToFile() {
    settingsLogger.info("Writing settings to ${settingsPath}:")
    val settingsAsString = objectMapper.writeValueAsString(settings)
    settingsPath.toFile().writeText(settingsAsString)
    settingsLogger.info("Settings written.")
}

fun readSettings() {
    try {
        objectMapper.readerForUpdating(settings).readValue<Settings>(settingsPath.toFile())
        writeSettingsToFile()
    } catch(_: java.io.FileNotFoundException) {
        writeSettingsToFile()
    } catch(e: Throwable) {
        settingsLogger.error("Error occurred while reading settings from $settingsPath.\n\tError: ", e)
        exitProcess(1)
    }
}

val commandDelimiters = settings.commandDelimiters
val linkedChats = settings.linkedChats
val aliases = settings.aliases
val serializedCommands = settings.serializedCommands
val syncedCalendars = settings.syncedCalendars
val notificationChannels = settings.notificationChannels
val timers = settings.timers
val imageUploadChannels: MutableMap<Chat, URI> = settings.imageUploadChannels
val reactServers: MutableMap<Server, MutableList<ReactConfig>> = settings.reactServers
val mentionChats: MutableMap<Chat, MutableMap<User, MutableMap<User, Int>>> = settings.mentionChats
val debugMode get() = settings.debugMode

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
