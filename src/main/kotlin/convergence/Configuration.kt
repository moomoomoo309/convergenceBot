package convergence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import convergence.ConfigOption.Companion.defaultSettings
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

const val defaultCommandDelimiter = "!"

@Suppress("EnumEntryName", "unused")
enum class ConfigOption(val defaultValueSupplier: () -> Any) {
    commandDelimiters({ HashMap<Chat, String>() }),
    linkedChats({ hashMapOf<Chat, MutableSet<Chat>>() }),
    commands({ mutableMapOf<Chat, MutableMap<String, Command>>() }),
    aliases({ mutableMapOf<Chat, MutableMap<String, Alias>>() }),
    serializedCommands({ mutableMapOf<Int, String>() })
    ;

    companion object {
        val values = entries
        val defaultSettings = values.associate { it.name to it.defaultValueSupplier }
    }
}

private val settingsMap = ConcurrentHashMap<String, Any>()

object Settings: MutableMap<String, Any> by settingsMap {
    val map = settingsMap
    var updateIsScheduled = true
    fun update() {
        // Don't update the settings multiple times per second, limit it to one per second
        if (!updateIsScheduled) {
            updateIsScheduled = true
            CommandScheduler.schedule(
                UniversalUser,
                CommandData(updateSettingsCommand, emptyList()),
                OffsetDateTime.now().plusSeconds(1L)
            )
            defaultLogger.info("Scheduled settings update.")
        }
    }

    override fun put(key: String, value: Any): Any? {
        val returnValue = settingsMap.put(key, value)
        update()
        return returnValue
    }
}

private fun writeLazySettingsToFile(settingsToWrite: Map<String, () -> Any>) =
    writeSettingsToFile(settingsToWrite.mapValues { it.value() })


private fun writeSettingsToFile(settingsToWrite: Map<String, Any>) {
    settingsLogger.info("Writing settings to ${settingsPath}:")
    // Converting to a [SortedMap] makes the keys go in alphabetical order, but Moshi can't serialize sorted maps,
    // so the SortedMap is converted back into the default map type (toMap() preserves the key ordering).
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    val settingsAsString = objectMapper.writeValueAsString(settingsToWrite.toSortedMap().toMap())
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false)
    settingsLogger.info(settingsAsString)
    settingsPath.toFile().writeText(settingsAsString)
    settingsLogger.info("Settings written.")
}

fun initSettings(): Map<String, Any> {
    with(settingsLogger) {
        if (Files.notExists(settingsPath)) {
            info("Creating new settings file since one was not found.")
            writeLazySettingsToFile(defaultSettings)
            return defaultSettings
        }
    }

    // Make sure to fill in any of the missing keys if needed
    return readSettings(defaultSettings, initialRun = true)
}

fun updateSettings(newSettings: ConcurrentHashMap<String, Any>) {
    settingsLogger.info("Seeing if settings need update...")
    // Make sure all the keys from the default settings exist in the current ones.
    for ((k, v) in defaultSettings)
        if (!newSettings.containsKey(k)) {
            val defaultValue = v()
            settingsLogger.error("$k was not in the config file, so its default value of $defaultValue is being written to the file")
            newSettings[k] = defaultValue
        }

    settingsLogger.info("Updating settings file...")
    writeSettingsToFile(newSettings)
    for ((k, _) in settingsMap) {
        if (!newSettings.containsKey(k)) {
            settingsMap.remove(k)
        }
    }
    settingsMap.putAll(newSettings)
    Settings.updateIsScheduled = false
}

/**
 * Warning: Editing this function can cause initialization errors that are a royal pain to debug. If you see
 * an [ExceptionInInitializerError], it's probably from this function.
 */
fun readSettings(fallbackSettings: Map<String, () -> Any>, initialRun: Boolean): ConcurrentHashMap<String, Any> = try {
    val finalSettings = objectMapper.readValue<MutableMap<String, Any>>(settingsPath.toFile())
    for ((k, v) in defaultSettings) {
        if (!finalSettings.containsKey(k)) {
            settingsLogger.error("$k was not in the config file initially, so its default value of $v will be written to the file")
            finalSettings[k] = v
        }
    }
    if (initialRun) {
        writeSettingsToFile(finalSettings)
    }
    ConcurrentHashMap(finalSettings)
} catch(e: Throwable) {
    settingsLogger.error("Error occurred while reading settings from $settingsPath. Returning fallback settings instead.\n\tError: $e")
    e.printStackTrace()
    ConcurrentHashMap(fallbackSettings.mapValues { it.value() })
}

val commandDelimiters: MutableMap<Chat, String> by Settings
val linkedChats: MutableMap<Chat, MutableList<Chat>> by Settings
val aliases: MutableMap<Chat, MutableMap<String, Alias>> by Settings
val serializedCommands: MutableMap<Int, String> by Settings

lateinit var convergencePath: Path
val chatMap: MutableMap<Int, Chat> = mutableMapOf()
val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
val commands: MutableMap<Chat, MutableMap<String, Command>> = mutableMapOf()
val protocols: MutableList<Protocol> = mutableListOf()
val sortedHelpText: MutableList<CommandLike> = mutableListOf()
var currentChatID: Int = 0
val aliasVars: MutableMap<String, (baseInterface: BaseInterface, chat: Chat, sender: User) -> String> = mutableMapOf(
    "sender" to { b: BaseInterface, c: Chat, s: User -> b.getName(c, s) },
    "botname" to { b: BaseInterface, c: Chat, _: User -> b.getName(c, b.getBot(c)) },
    "chatname" to { b: BaseInterface, c: Chat, _: User -> b.getChatName(c) }
)
val objectMapper = ObjectMapper()

val settingsPath: Path by lazy { convergencePath.resolve("settings.json") }
