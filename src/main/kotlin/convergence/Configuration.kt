package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import java.nio.file.Path

const val defaultCommandDelimiter = "!"

inline fun <reified T> ObjectMapper.readValue(value: String): T {
    return this.readValue(value, T::class.java)
}

inline fun <reified T> ObjectMapper.readValue(value: File): T {
    return this.readValue(value, T::class.java)
}

data class SettingsDTO(
    var aliases: MutableMap<String, MutableMap<String, Alias>> = mutableMapOf(),
    var commandDelimiters: MutableMap<String, String> = mutableMapOf(),
    var linkedChats: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf()
)

object Settings {
    var commandDelimiters: MutableMap<Chat, String> = mutableMapOf()
    var linkedChats: MutableMap<Chat, MutableSet<Chat>> = mutableMapOf()
    var aliases: MutableMap<Chat, MutableMap<String, Alias>> = mutableMapOf()
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf()

    @JsonIgnore
    var updateIsScheduled = false
    fun update() {
        // Don't update the settings multiple times per second, limit it to one per second
        if (!updateIsScheduled) {
            updateIsScheduled = true
            // Coroutines are _probably_ a better approach for this, but it's not a big deal.
            Thread {
                Thread.sleep(1000L)
                settingsLogger.info("Updating settings file...")
                writeSettingsToFile()
                updateIsScheduled = false
            }.start()
        }
    }

    private fun strToChat(s: String): Chat? =
        protocols.firstOrNull { it.name == s.substringBefore("(") }?.chatFromKey(s)

    private fun <T> mapFromDTO(map: Map<String, T>): Map<Chat, T> {
        return map.mapNotNull { (k, v) ->
            strToChat(k)?.to(v)
        }.toMap()
    }

    private fun linkedChatsFromDTO(linkedChats: MutableMap<String, MutableSet<String>>): MutableMap<Chat, MutableSet<Chat>> {
        return mutableMapOf(*linkedChats.mapNotNull { (k, v) ->
            strToChat(k)?.to(v.mapNotNull { strToChat(it) }.toMutableSet())
        }.toTypedArray())
    }

    private fun <T> mapToDTO(map: Map<Chat, T>): MutableMap<String, T> {
        return map.mapKeys { (k, _) ->
            k.toKey()
        } as MutableMap<String, T>
    }

    fun updateFrom(settingsDTO: SettingsDTO) {
        this.aliases.apply { clear() }.putAll(mapFromDTO(settingsDTO.aliases))
        this.commandDelimiters.apply { clear() }.putAll(mapFromDTO(settingsDTO.commandDelimiters))
        this.linkedChats.apply { clear() }.putAll(linkedChatsFromDTO(settingsDTO.linkedChats))
        this.serializedCommands.apply { clear() }.putAll(settingsDTO.serializedCommands)
    }

    fun toDTO() = SettingsDTO(
        mapToDTO(aliases),
        mapToDTO(commandDelimiters),
        mapToDTO(linkedChats.mapValues { (_, v) -> v.map { it.toKey() }.toMutableSet() }),
        serializedCommands
    )
}


private fun writeSettingsToFile() {
    settingsLogger.info("Writing settings to ${settingsPath}:")
    val settingsAsString = objectMapper.writeValueAsString(Settings.toDTO())
    settingsLogger.info(settingsAsString)
    settingsPath.toFile().writeText(settingsAsString)
    settingsLogger.info("Settings written.")
}

fun readSettings() {
    try {
        val settings = objectMapper.readValue<SettingsDTO>(settingsPath.toFile())
        Settings.updateFrom(settings)
        writeSettingsToFile()
    } catch(e: Throwable) {
        settingsLogger.error("Error occurred while reading settings from $settingsPath. Returning fallback settings instead.\n\tError: $e")
        writeSettingsToFile()
        e.printStackTrace()
    }
}

val commandDelimiters = Settings.commandDelimiters
val linkedChats = Settings.linkedChats
val aliases = Settings.aliases
val serializedCommands = Settings.serializedCommands

lateinit var convergencePath: Path
val chatMap: MutableMap<Int, Chat> = mutableMapOf()
val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
val commands: MutableMap<Chat, MutableMap<String, Command>> = mutableMapOf()
val protocols: MutableList<Protocol> = mutableListOf()
val sortedHelpText: MutableList<CommandLike> = mutableListOf()
var currentChatID: Int = 0
val aliasVars: MutableMap<String, (chat: Chat, sender: User) -> String> = mutableMapOf(
    "sender" to { c: Chat, s: User -> c.protocol.getName(c, s) },
    "botname" to { c: Chat, _: User -> c.protocol.getName(c, c.protocol.getBot(c)) },
    "chatname" to { c: Chat, _: User -> c.protocol.getChatName(c) }
)
val objectMapper: ObjectMapper = ObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)
    .findAndRegisterModules()

val settingsPath: Path by lazy { convergencePath.resolve("settings.json") }
