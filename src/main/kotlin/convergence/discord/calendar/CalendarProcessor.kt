package convergence.discord.calendar

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.util.GenerateQuery
import convergence.*
import convergence.discord.DiscordChat
import convergence.discord.DiscordProtocol
import convergence.discord.discordLogger
import convergence.discord.frat.fratConfig
import convergence.discord.jda
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ScheduledEvent
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.component.VEvent
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val base64Encoder: Base64.Encoder = Base64.getEncoder()
val calendarCache = hashMapOf<String, CalDAVCollection>()
val httpClient: CloseableHttpClient = HttpClients.custom().setDefaultHeaders(
    listOf(
        BasicHeader(
            "Authorization", "Basic ${base64Encoder.encodeToString(("bot:" + fratConfig.botPassword).toByteArray())}"
        )
    )
).build()

const val UID_LENGTH = 36
const val DAYS = 30L
private var lastCalendarUpdateTime = Instant.now().plusSeconds(10)
private val calendarUpdateFrequency = Duration.ofMinutes(15L)


object CalendarProcessor {
    fun getAndCacheCalendar(url: String): CalDAVCollection? {
        calendarCache[url]?.let {
            return it
        }
        val cal = CalDAVCollection(url)
        try {
            cal.testConnection(httpClient)
        } catch(e: CalDAV4JException) {
            discordLogger.error("Could not connect to calendar! Exception: ", e)
            return null
        }
        calendarCache[url] = cal
        return cal
    }

    fun getCalDAVEventsNextDays(calCollection: CalDAVCollection): List<VEvent> {
        val cals = calCollection.queryCalendars(httpClient, GenerateQuery().generate())
        val eventList = mutableSetOf<VEvent>()
        for (cal in cals) {
            eventList.addAll(cal.getComponents("VEVENT"))
        }
        val now = Instant.now()
        return eventList.filter {
            val duration = it.getConsumedTime(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate())
            duration.isNotEmpty() && duration.first().start.toInstant().isAfter(now)
        }.sortedBy { it.startDate.date }
    }

    private fun getDiscordEventsNextDays(jda: JDA, guildId: Long): List<ScheduledEvent> {
        return jda.scheduledEvents.filter {
            it.startTime.isAfter(OffsetDateTime.now())
                    && it.startTime.isBefore(OffsetDateTime.now().plus(DAYS, ChronoUnit.DAYS))
                    && it.guild.idLong == guildId
        }.sortedBy { it.startTime }
    }

    @Suppress("UNUSED_PARAMETER")
    fun syncCommand(args: List<String>, chat: Chat, sender: User): String {
        if (args.size != 1)
            return "Expected 1 argument, got ${args.size}."
        if (chat !is DiscordChat)
            return "Can only be ran on a Discord chat. How did you run this on a different chat?"
        if (getAndCacheCalendar(args[0]) == null)
            return "No calendar found at URL \"${args[0]}\"."
        val syncedCalendar = SyncedCalendar(chat.channel.guild.idLong, args[0])
        if (syncedCalendar in syncedCalendars)
            return "That calendar is already synced!"
        syncedCalendars.add(syncedCalendar)
        Settings.update()
        return syncToDiscord(syncedCalendars.filter { it.guildId == chat.channel.guild.idLong })
    }

    fun syncToDiscord(cals: List<SyncedCalendar>): String {
        val calendarEvents = mutableListOf<VEvent>()
        val guildId = cals.first().guildId
        for (cal in cals) {
            val calCollection = getAndCacheCalendar(cal.calURL) ?: return "No calendar found at URL \"${cal.calURL}\"."
            calendarEvents.addAll(getCalDAVEventsNextDays(calCollection))
            if (cal.guildId != guildId)
                return "The guild IDs of each calendar don't match!"
        }
        return syncToDiscord(
            jda.getGuildById(guildId) ?: return "No guild found with ID $guildId.",
            calendarEvents.sortedBy { it.getNextOccurrence().start },
            getDiscordEventsNextDays(jda, guildId)
        )
    }

    private val uidRegex = Regex("[a-z0-9-]{36}")
    private fun syncToDiscord(guild: Guild, calEvents: List<VEvent>, discordEvents: List<ScheduledEvent>): String {
        val uidMap = mutableMapOf<String, Long>()
        val calEventsById = mutableMapOf<String, VEvent>()
        val futures = mutableListOf<CompletableFuture<ScheduledEvent>>()
        outer@ for (event in calEvents) {
            calEventsById[event.uid.value] = event
            for (currentDiscordEvent in discordEvents) {
                if (event.equalEnough(currentDiscordEvent)) {
                    uidMap[event.uid.value] = currentDiscordEvent.idLong
                    continue@outer
                }
            }
            futures.add(addCalendarEventToDiscord(uidMap, guild, event))
        }

        // Wait for all the events to be added, so we know uidMap is populated
        CompletableFuture.allOf(*futures.toTypedArray()).join()

        for (discordEvent in discordEvents) {
            val uid = discordEvent.description?.takeLast(UID_LENGTH)?.trim()
            if (uid == null || !uidRegex.matches(uid))
                continue
            if (uidMap[uid] == null || calEventsById[uid]?.equalEnough(discordEvent) != true) {
                discordEvent.delete().queue()
            }
        }
        return "Calendar synced successfully."
    }

    private fun addCalendarEventToDiscord(
        uidMap: MutableMap<String, Long>,
        guild: Guild,
        event: VEvent
    ): CompletableFuture<ScheduledEvent> {
        val nextOccurrence = event.getNextOccurrence()
        return guild.createScheduledEvent(
            event.summary?.value ?: "Unnamed event",
            if (event.location?.value.isNullOrBlank()) "No Location" else event.location.value,
            nextOccurrence.start.toOffsetDateTime(),
            nextOccurrence.end.toOffsetDateTime()
        ).setDescription(addUIDToDescription(event.description?.value ?: "", event.uid.value))
            .submit()
            .whenComplete { discordEvent, _ ->
                uidMap[event.uid.value] = discordEvent.idLong
            }
    }

    private fun addUIDToDescription(description: String?, uid: String): String {
        if (description.isNullOrEmpty()) {
            return uid
        } else if (description.endsWith(uid)) {
            return description
        }
        return description + "\n" + uid
    }

    fun onUpdate() {
        if ((lastCalendarUpdateTime + calendarUpdateFrequency).isBefore(Instant.now())) {
            syncCalendars()
        }
    }

    fun syncCalendars() {
        defaultLogger.info("Syncing calendars...")
        Thread {
            val serverIDs = syncedCalendars.map { it.guildId }.toSet()
            for (serverId in serverIDs)
                syncToDiscord(syncedCalendars.filter { it.guildId == serverId })
        }.start()
        lastCalendarUpdateTime = Instant.now()
    }
}

fun Date.toOffsetDateTime(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)
val defaultZoneOffset: ZoneOffset = OffsetDateTime.now().offset

fun Instant.toIDate(): Date {
    return Date(Date.from(this))
}

@OptIn(ExperimentalContracts::class)
fun VEvent.equalEnough(dEvent: ScheduledEvent?): Boolean {
    contract {
        returns(true) implies (dEvent != null)
    }
    val period = this.getNextOccurrence()
    // More fields can be added here if needed, but this should be alright
    return dEvent != null
            && period.start.time == dEvent.startTime.toInstant().toEpochMilli()
            && period.end.time == dEvent.endTime?.toInstant()?.toEpochMilli()
            && (this.summary?.value ?: "Unnamed event") == dEvent.name
}

fun VEvent.getNextOccurrence(): Period {
    val now = Instant.now()
    return this.getConsumedTime(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate(), true).first()
}

@SuppressWarnings("LongMethod")
fun registerCalendarCommands() {
    registerCommand(
        Command.of(
            DiscordProtocol,
            "syncCalendar",
            listOf(ArgumentSpec("URL", ArgumentType.STRING)),
            CalendarProcessor::syncCommand,
            "Sync a CalDAV calendar to discord events.",
            "syncCalendar (URL)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "resyncCalendar",
            listOf(),
            { -> CalendarProcessor.syncCalendars(); "Resyncing calendars..." },
            "Resyncs all calendars in all servers.",
            "resyncCalendar (Takes no parameters)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "unsyncCalendar",
            listOf(ArgumentSpec("URL", ArgumentType.STRING)),
            { args, chat ->
                val removed = syncedCalendars.remove(syncedCalendars.firstOrNull {
                    it.guildId == (chat as DiscordChat).server.guild.idLong && it.calURL == args[0]
                }
                )
                if (removed) "Calendar removed." else "No calendar with URL ${args[0]} found."
            },
            "Removes a synced calendar.",
            "unsyncCalendar (URL)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "syncedCalendars",
            listOf(),
            { _, chat ->
                syncedCalendars.filter {
                    it.guildId == (chat as DiscordChat).server.guild.idLong
                }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
            },
            "Prints out all the synced calendars in this chat.",
            "syncedCalendars (Takes no parameters)"
        )
    )

    registerCommand(
        Command.of(
            DiscordProtocol,
            "syncCalendar",
            listOf(ArgumentSpec("URL", ArgumentType.STRING)),
            CalendarProcessor::syncCommand,
            "Sync a CalDAV calendar to discord events.",
            "syncCalendar (URL)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "resyncCalendar",
            listOf(),
            { -> CalendarProcessor.syncCalendars(); "Resyncing calendars..." },
            "Resyncs all calendars in all servers.",
            "resyncCalendar (Takes no parameters)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "unsyncCalendar",
            listOf(ArgumentSpec("URL", ArgumentType.STRING)),
            { args, chat ->
                val removed = syncedCalendars.remove(syncedCalendars.firstOrNull {
                    it.guildId == (chat as DiscordChat).server.guild.idLong && it.calURL == args[0]
                })
                if (removed) "Calendar removed." else "No calendar with URL ${args[0]} found."
            },
            "Removes a synced calendar.",
            "unsyncCalendar (URL)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "syncedCalendars",
            listOf(),
            { _, chat ->
                syncedCalendars.filter {
                    it.guildId == (chat as DiscordChat).server.guild.idLong
                }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
            },
            "Prints out all the synced calendars in this chat.",
            "syncedCalendars (Takes no parameters)"
        )
    )
}
