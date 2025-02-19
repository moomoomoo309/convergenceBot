package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import convergence.discord.jda
import java.nio.file.Path
import java.time.OffsetDateTime

const val defaultCommandDelimiter = "!"

data class SyncedCalendar(val guildId: Long, val calURL: String) {
    override fun toString(): String {
        return "SyncedCalendar(guild=${jda.getGuildById(guildId)?.name ?: guildId}, calURL=$calURL)"
    }
}

data class AliasDTO(val scopeName: String, val name: String, val commandName: String, val protocolName: String, val args: List<String>) {
    fun toAlias(): Alias {
        val protocol = protocols.first { protocolName == it.name }
        val scope = protocol.commandScopeFromKey(scopeName) as Chat
        val command = getCommand(commandName, scope) as Command
        return Alias(scope, name, command, args)
    }
}

data class ScheduledCommandDTO(
    val time: OffsetDateTime,
    val chatKey: String,
    val senderKey: String,
    val protocolName: String,
    val commandName: String,
    val args: List<String>,
    val id: Int,
) {
    fun toScheduledCommand() = strToProtocol(protocolName)?.let { protocol ->
        ScheduledCommand(
            time,
            protocol.commandScopeFromKey(chatKey) as Chat,
            protocol.userFromKey(senderKey)!!,
            protocolName,
            commandName,
            args,
            id
        )
    }
}

private fun strToProtocol(s: String) = protocols.firstOrNull { it.name == s }
private fun scopeStrToProtocol(s: String) = protocols.firstOrNull { it.name == s.substringBefore("(") }
private fun strToChat(s: String): CommandScope? = scopeStrToProtocol(s)?.commandScopeFromKey(s)

data class SettingsDTO(
    var aliases: MutableMap<String, MutableMap<String, AliasDTO>> = mutableMapOf(),
    var commandDelimiters: MutableMap<String, String> = mutableMapOf(),
    var linkedChats: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var serializedCommands: MutableMap<Int, ScheduledCommandDTO> = mutableMapOf(),
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf()
)

object Settings {
    var aliases: MutableMap<CommandScope, MutableMap<String, Alias>> = mutableMapOf()
    var commandDelimiters: MutableMap<CommandScope, String> = mutableMapOf()
    var linkedChats: MutableMap<Chat, MutableSet<Chat>> = mutableMapOf()
    var serializedCommands: MutableMap<Int, ScheduledCommand> = mutableMapOf()
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf()

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

    private fun <T> mapFromDTO(map: Map<String, T>): Map<out CommandScope, T> {
        return map.mapNotNull { (k, v) ->
            strToChat(k)?.to(v)
        }.toMap()
    }

    private fun linkedChatsFromDTO(linkedChats: MutableMap<String, MutableSet<String>>): MutableMap<Chat, MutableSet<Chat>> {
        return mutableMapOf(*linkedChats.mapNotNull { (k, v) ->
            (strToChat(k) as? Chat)?.to(v.mapNotNull { strToChat(it) as? Chat }.toMutableSet())
        }.toTypedArray())
    }

    private fun <T> mapToDTO(map: Map<out CommandScope, T>): MutableMap<String, T> {
        return map.mapKeys { (k, _) ->
            k.toKey()
        } as MutableMap<String, T>
    }

    private fun mapAliasesToDTO(aliases: MutableMap<CommandScope, MutableMap<String, Alias>>): MutableMap<String, MutableMap<String, AliasDTO>> {
        return aliases.map { (k, v) ->
            k.toKey() to v.mapValues { (_, alias) -> alias.toDTO() }.toMutableMap()
        }.toMap().toMutableMap()
    }

    private fun mapAliasesFromDTO(aliasesDTO: MutableMap<String, MutableMap<String, AliasDTO>>): MutableMap<CommandScope, MutableMap<String, Alias>> {
        return aliasesDTO.map { (k, v) ->
            val protocol = scopeStrToProtocol(k)!!
            protocol.commandScopeFromKey(k)!! to v.mapValues { (_, aliasDTO) -> aliasDTO.toAlias() }.toMutableMap()
        }.toMap().toMutableMap()
    }

    fun updateFrom(settingsDTO: SettingsDTO) {
        this.aliases.apply { clear() }.putAll(mapAliasesFromDTO(settingsDTO.aliases))
        this.commandDelimiters.apply { clear() }.putAll(mapFromDTO(settingsDTO.commandDelimiters))
        this.linkedChats.apply { clear() }.putAll(linkedChatsFromDTO(settingsDTO.linkedChats))
        this.serializedCommands.apply { clear() }
            .putAll(settingsDTO.serializedCommands.mapValues { (_, dto) -> dto.toScheduledCommand()!! }.toMutableMap())
        this.syncedCalendars.apply { clear() }.addAll(settingsDTO.syncedCalendars)
    }

    fun toDTO() = SettingsDTO(
        mapAliasesToDTO(aliases),
        mapToDTO(commandDelimiters),
        mapToDTO(linkedChats.mapValues { (_, v) -> v.map { it.toKey() }.toMutableSet() }),
        serializedCommands.mapValues { (_, serializedCommands) -> serializedCommands.toDTO() }.toMutableMap(),
        syncedCalendars
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
val syncedCalendars = Settings.syncedCalendars

lateinit var convergencePath: Path
val chatMap: MutableMap<Int, Chat> = mutableMapOf()
val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
val commands: MutableMap<Protocol, MutableMap<String, Command>> = mutableMapOf()
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
