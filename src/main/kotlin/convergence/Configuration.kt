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

data class ReactConfig(val destination: DiscordChat, val emojis: MutableMap<String, Int>) {
    fun toDTO() = ReactConfigDTO(destination, emojis)
}

data class ReactConfigDTO(val destination: String, val emojis: MutableMap<String, Int>) {
    constructor(chat: DiscordChat, emojis: MutableMap<String, Int>): this(chat.toKey(), emojis)

    fun toConfig() = ReactConfig(
        scopeStrToProtocol(destination)!!.commandScopeFromKey(destination) as DiscordChat,
        emojis
    )
}

private fun strToProtocol(s: String) = protocols.firstOrNull { it.name == s }
private fun scopeStrToProtocol(s: String) = protocols.sortedBy { -it.name.length }
    .firstOrNull { s.substringBefore("(").startsWith(it.name) }

private fun strToChat(s: String): CommandScope? = scopeStrToProtocol(s)?.commandScopeFromKey(s)

data class SettingsDTO(
    var aliases: MutableMap<String, MutableMap<String, AliasDTO>> = mutableMapOf(),
    var commandDelimiters: MutableMap<String, String> = mutableMapOf(),
    var linkedChats: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var serializedCommands: MutableMap<Int, ScheduledCommandDTO> = mutableMapOf(),
    var syncedCalendars: MutableList<SyncedCalendar> = mutableListOf(),
    var timers: MutableMap<String, OffsetDateTime> = mutableMapOf(),
    var imageUploadChannels: MutableMap<String, String> = mutableMapOf(),
    var reactServers: MutableMap<String, List<ReactConfigDTO>> = mutableMapOf(),
    val mentionChats: MutableMap<String, MutableMap<String, MutableMap<String, Int>>> = mutableMapOf()
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

    private fun linkedChatsFromDTO(linkedChats: MutableMap<String, MutableSet<String>>):
            MutableMap<Chat, MutableSet<Chat>> {
        return mutableMapOf(*linkedChats.mapNotNull { (k, v) ->
            (strToChat(k) as? Chat)?.to(v.mapNotNull { strToChat(it) as? Chat }.toMutableSet())
        }.toTypedArray())
    }

    private fun <T> mapToDTO(map: Map<out CommandScope, T>): MutableMap<String, T> {
        return map.mapKeys { (k, _) ->
            k.toKey()
        }.mutable()
    }

    private fun mapAliasesToDTO(aliases: MutableMap<CommandScope, MutableMap<String, Alias>>):
            MutableMap<String, MutableMap<String, AliasDTO>> {
        return aliases.mapEntries { (k, v) ->
            k.toKey() to v.mapValues { (_, alias) -> alias.toDTO() }.toMutableMap()
        }.mutable()
    }

    private fun mapAliasesFromDTO(aliasesDTO: MutableMap<String, MutableMap<String, AliasDTO>>):
            MutableMap<CommandScope, MutableMap<String, Alias>> {
        return aliasesDTO.mapEntries { (k, v) ->
            val protocol = scopeStrToProtocol(k)!!
            protocol.commandScopeFromKey(k)!! to v.mapValues { (_, aliasDTO) -> aliasDTO.toAlias() }.toMutableMap()
        }.mutable()
    }

    fun updateFrom(settingsDTO: SettingsDTO) {
        this.aliases.clearThen().putAll(mapAliasesFromDTO(settingsDTO.aliases))
        this.commandDelimiters.clearThen().putAll(mapFromDTO(settingsDTO.commandDelimiters))
        this.linkedChats.clearThen().putAll(linkedChatsFromDTO(settingsDTO.linkedChats))
        this.serializedCommands.clearThen().putAll(settingsDTO.serializedCommands.mapValues { (_, dto) ->
            dto.toScheduledCommand()!!
        } as MutableMap<Int, ScheduledCommand>)
        this.syncedCalendars.clearThen().addAll(settingsDTO.syncedCalendars)
        this.timers.clearThen().putAll(settingsDTO.timers)
        this.imageUploadChannels.clearThen().putAll(settingsDTO.imageUploadChannels.map { (k, v) ->
            val protocol = scopeStrToProtocol(k)!!
            protocol.commandScopeFromKey(k) as Chat to URI(v)
        })
        this.reactServers.clearThen().putAll(settingsDTO.reactServers.mapEntries { (k, v) ->
            val protocol = scopeStrToProtocol(k)!!
            protocol.commandScopeFromKey(k) as Server to v.map { it.toConfig() }.mutable()
        })
        this.mentionChats.clearThen().putAll(settingsDTO.mentionChats.mapEntries { (k, v) ->
            val protocol = scopeStrToProtocol(k)!!
            val chat = protocol.commandScopeFromKey(k) as Chat
            chat to v.mapEntries { (receiver, mentions) ->
                protocol.userFromKey(receiver)!! to mentions.mapKeys { (user, _) -> protocol.userFromKey(user)!! }
                    .mutable()
            }.mutable()
        })
    }

    fun toDTO() = SettingsDTO(
        mapAliasesToDTO(aliases),
        mapToDTO(commandDelimiters),
        mapToDTO(linkedChats.mapValues { (_, v) -> v.map { it.toKey() }.toMutableSet() }),
        serializedCommands.mapValues { (_, serializedCommands) ->
            serializedCommands.toDTO()
        } as MutableMap<Int, ScheduledCommandDTO>,
        syncedCalendars,
        timers,
        imageUploadChannels.mapEntries { (k, v) ->
            k.toKey() to v.toString()
        }.mutable(),
        reactServers.mapEntries { (k, v) ->
            k.toKey() to v.map { it.toDTO() }
        }.mutable(),
        mentionChats.mapEntries { (k, v) ->
            k.toKey() to v.mapEntries { (receiver, mentions) ->
                receiver.toKey() to mentions.mapKeys { (user, _) -> user.toKey() }
                    .mutable()
            }.mutable()
        }.mutable()
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

lateinit var convergencePath: Path
val chatMap: MutableMap<Int, Chat> = mutableMapOf()
val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
val commands: MutableMap<Protocol, MutableMap<String, Command>> = mutableMapOf()
val protocols: MutableList<Protocol> = mutableListOf()
val sortedHelpText: MutableList<CommandLike> = mutableListOf()
var currentChatID: Int = 0
val aliasVars: MutableMap<String, (chat: Chat, sender: User) -> String?> = mutableMapOf(
    "%sender" to { c: Chat, s: User -> c.protocol.getName(c, s) },
    "%nick" to { c, s -> (c.protocol as? HasNicknames)?.getUserNickname(c, s) },
    "%botname" to { c: Chat, _: User -> c.protocol.getName(c, c.protocol.getBot(c)) },
    "%chatname" to { c: Chat, _: User -> c.protocol.getChatName(c) }
)
val objectMapper: ObjectMapper = ObjectMapper()
    .configure(SerializationFeature.INDENT_OUTPUT, true)
    .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true)
    .registerKotlinModule()
    .findAndRegisterModules()
val settingsPath: Path by lazy { convergencePath.resolve("settings.json") }
