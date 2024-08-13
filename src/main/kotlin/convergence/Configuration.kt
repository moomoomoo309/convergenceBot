package convergence

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import convergence.ConfigOption.Companion.defaultSettings
import net.sourceforge.argparse4j.inf.Namespace
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

const val defaultCommandDelimiter = "!"

@Suppress("EnumEntryName", "unused")
enum class ConfigOption(val defaultValue: Any) {
    protocols(mutableListOf<Protocol>()),
    baseInterfaceMap(mutableMapOf<Protocol, BaseInterface>()),
    chatMap(mutableMapOf<Int, Chat>()),
    reverseChatMap(mutableMapOf<Chat, Int>()),
    pluginPaths(mutableListOf<Path>()),
    commandDelimiters(DefaultMap<Chat, String>(defaultCommandDelimiter)),
    aliasVars(
        mutableMapOf(
            "sender" to { b: BaseInterface, c: Chat, s: User -> b.getName(c, s) },
            "botname" to { b: BaseInterface, c: Chat, _: User -> b.getName(c, b.getBot(c)) },
            "chatname" to { b: BaseInterface, c: Chat, _: User -> b.getChatName(c) })
    ),
    linkedChats(hashMapOf<Chat, MutableSet<Chat>>()),
    delimiters(hashMapOf<Chat, String>()),
    commands(mutableMapOf<Chat, MutableMap<String, Command>>()),
    aliases(mutableMapOf<Chat, MutableMap<String, Alias>>()),
    ;

    companion object {
        val values = values().toList()
        val defaultSettings = values.associate { it.name to it.defaultValue }
    }
}

object Settings: MutableMap<String, Any?> by ConcurrentHashMap(initSettings())

val initMoshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private fun writeSettingsToFile(settingsToWrite: Map<String, Any>) = Files.write(
    settingsPath,
    // Converting to a [SortedMap] makes the keys go in alphabetical order, but Moshi can't serialize sorted maps,
    // so the SortedMap is converted back into the default map type (toMap() preserves the key ordering).
    initMoshi.toJson(settingsToWrite.toSortedMap().toMap(), prettyPrint = true).toByteArray(),
)

fun initSettings(): Map<String, Any> {
    with(settingsLogger) {
        if (Files.notExists(settingsPath)) {
            info("Creating new settings file since one was not found.")
            writeSettingsToFile(defaultSettings)
            return defaultSettings
        }
    }

    // Make sure to fill in any of the missing keys if needed
    return readSettings(defaultSettings, initialRun = true)
}

private var settingsFromPreviousUpdate: ConcurrentHashMap<String, Any>? = null
fun updateSettings(newSettings: ConcurrentHashMap<String, Any>) {
    var settingsHaveChanged = false
    // Make sure all the keys from the default settings exist in the current ones.
    for ((k, v) in defaultSettings)
        if (!newSettings.containsKey(k)) {
            settingsLogger.error("$k was not in the config file, so its default value of $v is being written to the file")
            newSettings[k] = v
            settingsHaveChanged = true
        }

    // See if the settings have changed at all.
    if (!settingsHaveChanged)
        settingsHaveChanged = settingsFromPreviousUpdate == null || settingsFromPreviousUpdate != newSettings

    // If the settings have changed, write them out.
    if (settingsHaveChanged) {
        settingsLogger.info("Updating settings file...")
        writeSettingsToFile(newSettings)
        for ((k, _) in Settings) {
            if (!newSettings.containsKey(k)) {
                Settings.remove(k)
            }
        }
        Settings.putAll(newSettings)
        settingsFromPreviousUpdate = ConcurrentHashMap(newSettings)
    }
}

/**
 * Warning: Editing this function can cause initialization errors that are a royal pain to debug. If you see
 * an [ExceptionInInitializerError], it's probably from this function.
 */
fun readSettings(fallbackSettings: Map<String, Any>, initialRun: Boolean): ConcurrentHashMap<String, Any> = try {
    val finalSettings = initMoshi.fromJson<MutableMap<String, Any>>(settingsPath.toFile().readText())
    for ((k, v) in defaultSettings) {
        if (!finalSettings.containsKey(k)) {
            settingsLogger.error("$k was not in the config file initially, so its default value of $v will be written to the file")
            finalSettings[k] = v
        }
    }
    if (initialRun) {
        writeSettingsToFile(finalSettings)
        settingsFromPreviousUpdate = ConcurrentHashMap(finalSettings)
    }
    ConcurrentHashMap(finalSettings)
} catch (e: Throwable) {
    settingsLogger.error("Error occurred while reading settings from $settingsPath. Returning fallback settings instead.\n\tError: $e")
    e.printStackTrace()
    ConcurrentHashMap(fallbackSettings)
}

val protocols: MutableList<Protocol> by Settings
val baseInterfaceMap: MutableMap<Protocol, BaseInterface> by Settings
val chatMap: MutableMap<Int, Chat> by Settings
val reverseChatMap: MutableMap<Chat, Int> by Settings
val convergencePath: Path by Settings
var pluginPaths: MutableList<Path> by Settings
val commandDelimiters: MutableMap<Chat, String> by Settings
val aliasVars: MutableMap<String, (baseInterface: BaseInterface, chat: Chat, sender: User) -> String> by Settings
val linkedChats: MutableMap<Chat, MutableList<Chat>> by Settings
val delimiters: MutableMap<Chat, String> by Settings
val commands: MutableMap<Chat, MutableMap<String, Command>> by Settings
val aliases: MutableMap<Chat, MutableMap<String, Alias>> by Settings

object SharedVariables: MutableMap<String, Any?> by ConcurrentHashMap(
    SharedVariable.values().associate { it.name to it.defaultValue }
)

@Suppress("unused")
enum class SharedVariable(val defaultValue: Any?) {
    sortedHelpText(mutableListOf<CommandLike>()),
    commandLineArgs(null),
    pluginManager(DefaultPluginManager()),
    currentChatID(0),
    moshiBuilder(
        Moshi.Builder()
            .add(SingletonAdapterFactory)
            .add(OffsetDateTimeAdapter)
            .add(BaseInterfaceAdapterFactory)
            .add(KotlinJsonAdapterFactory())!!
    ),
    moshi(null),
    chatAdapterFactory(PolymorphicJsonAdapterFactory.of(Chat::class.java, "type")),
    userAdapterFactory(PolymorphicJsonAdapterFactory.of(User::class.java, "type")),
    imageAdapterFactory(PolymorphicJsonAdapterFactory.of(Image::class.java, "type")),
    messageHistoryAdapterFactory(PolymorphicJsonAdapterFactory.of(MessageHistory::class.java, "type")),
    stickerAdapterFactory(PolymorphicJsonAdapterFactory.of(Sticker::class.java, "type")),
    formatAdapterFactory(PolymorphicJsonAdapterFactory.of(Format::class.java, "type")),
    customEmojiAdapterFactory(PolymorphicJsonAdapterFactory.of(CustomEmoji::class.java, "type")),
}

val sortedHelpText: MutableList<CommandLike> by SharedVariables
var commandLineArgs: Namespace by SharedVariables
var pluginManager: PluginManager by SharedVariables
var currentChatID: Int by SharedVariables
val moshiBuilder: Moshi.Builder by SharedVariables
var moshi: Moshi by SharedVariables

val settingsPath: Path = convergencePath.resolve("settings.json")
