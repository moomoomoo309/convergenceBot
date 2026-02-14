package convergence.discord.calendar

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.model.request.CalendarQuery
import com.github.caldav4j.model.request.CompFilter
import com.github.caldav4j.model.request.TimeRange
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
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Status
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

data class EventInstance(
    val vevent: VEvent,
    val start: Date,
    val end: Date
) {
    val uid: String? get() = vevent.uid?.value
    val summary: String? get() = vevent.summary?.value
}

object CalendarProcessor {
    init {
        System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            "net.fortuna.ical4j.util.MapTimeZoneCache"
        )
    }
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

    fun buildCalendarQuery(now: Instant): CalendarQuery {
        val calendarQuery = GenerateQuery().generate()
        val calFilter = CompFilter("VCALENDAR")
        val eventFilter = CompFilter("VEVENT")
        eventFilter.timeRange = TimeRange(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate())
        calFilter.addCompFilter(eventFilter)
        calendarQuery.compFilter = calFilter
        return calendarQuery
    }

    fun getCalDAVEventsNextDays(calendars: List<Calendar>): List<EventInstance> {
        val now = Instant.now()
        val periodWindow = Period(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate())

        val masterEvents = mutableMapOf<String, VEvent>()
        val exceptions = mutableMapOf<String, MutableList<VEvent>>()

        // Separate master events and exceptions
        for (cal in calendars) {
            val events = cal.getComponents<VEvent>("VEVENT")
            for (event in events) {
                val uid = event.uid.value
                if (event.recurrenceId == null) {
                    masterEvents[uid] = event
                } else {
                    exceptions.computeIfAbsent(uid) { mutableListOf() }.add(event)
                }
            }
        }

        val events = mutableListOf<EventInstance>()

        for ((uid, master) in masterEvents) {
            val occurrences = master.calculateRecurrenceSet(periodWindow)

            val exList = exceptions[uid] ?: emptyList()
            for (ex in exList) {
                val rid = DateTime(ex.recurrenceId.date)

                val status = ex.getProperty<Status>("STATUS")?.value
                if (status == "CANCELLED") {
                    // Remove cancelled occurrence
                    occurrences.removeIf { it.start == rid }
                } else {
                    // Override: replace original occurrence with this one
                    occurrences.removeIf { it.start == rid }
                    occurrences.add(Period(DateTime(ex.startDate.date), DateTime(ex.endDate.date)))
                }
            }

            for (period in occurrences) {
                events.add(
                    EventInstance(
                        vevent = master,
                        start = period.start,
                        end = period.end
                    )
                )
            }
        }

        return events.sortedBy { it.start }
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
        val calendarEvents = mutableListOf<EventInstance>()
        val guildId = cals.first().guildId
        for (cal in cals) {
            val calCollection = getAndCacheCalendar(cal.calURL) ?: return "No calendar found at URL \"${cal.calURL}\"."
            val cals = calCollection.queryCalendars(httpClient, buildCalendarQuery(Instant.now()))
            calendarEvents.addAll(getCalDAVEventsNextDays(cals))
            if (cal.guildId != guildId)
                return "The guild IDs of each calendar don't match!"
        }
        return syncToDiscord(
            jda.getGuildById(guildId) ?: return "No guild found with ID $guildId.",
            calendarEvents,
            getDiscordEventsNextDays(jda, guildId)
        )
    }

    private fun syncToDiscord(
        guild: Guild,
        calEvents: List<EventInstance>,
        discordEvents: List<ScheduledEvent>
    ): String {
        val futures = getFutures(calEvents, discordEvents, guild)

        // Remove duplicate events
        removeDuplicateEvents(futures, discordEvents)

        // Add all the events simultaneously, adding them to the uidMap after, then wait for all of them to complete.
        CompletableFuture.allOf(*futures.map {
            it.second.submit()
        }.toTypedArray()).join()

        // Remove invalid events
        removeInvalidEvents(discordEvents, calEvents)
        return "Calendar synced successfully."
    }

    private fun getFutures(
        calEvents: List<EventInstance>,
        discordEvents: List<ScheduledEvent>,
        guild: Guild
    ): MutableList<Pair<EventInstance, ScheduledEventAction>> {
        val futures = mutableListOf<Pair<EventInstance, ScheduledEventAction>>()
        outer@ for (eventInstance in calEvents) {
            val vevent = eventInstance.vevent
            for (currentDiscordEvent in discordEvents)
                if (vevent.equalEnough(currentDiscordEvent))
                    continue@outer
            addCalendarEventToDiscord(guild, eventInstance)?.let {
                futures.add(it)
            }
        }
        return futures
    }

    private val uidRegex = Regex("[a-z0-9-]{36}")
    private fun removeInvalidEvents(
        discordEvents: List<ScheduledEvent>,
        calEvents: List<EventInstance>,
    ) {
        outer@for (discordEvent in discordEvents) {
            val uid = discordEvent.description?.takeLast(UID_LENGTH)?.trim()
            if (uid == null || !uidRegex.matches(uid))
                continue
            val possibleEvents = calEvents.filter {
                it.uid == uid
            }
            for (possibleEvent in possibleEvents)
                if (possibleEvent.vevent.equalEnough(discordEvent))
                    continue@outer
            discordEvent.delete().queue()
        }
    }

    private fun removeDuplicateEvents(
        futures: MutableList<Pair<EventInstance, ScheduledEventAction>>,
        discordEvents: List<ScheduledEvent>
    ) {
        val futureIter = futures.iterator()
        while (futureIter.hasNext()) {
            val (event, _) = futureIter.next()
            for (discordEvent in discordEvents) {
                if (event.summary == discordEvent.name) {
                    if (event.start.toInstant().toEpochMilli() == discordEvent.startTime.toInstant().toEpochMilli()) {
                        futureIter.remove()
                        break
                    }
                }
            }
        }
    }

    private fun addCalendarEventToDiscord(
        guild: Guild,
        eventInstance: EventInstance
    ): Pair<EventInstance, ScheduledEventAction>? {
        val vevent = eventInstance.vevent
        return if (eventInstance.start.toInstant() < Instant.now())
            null
        else
            Pair(
                eventInstance, guild.createScheduledEvent(
                    vevent.summary?.value ?: "Unnamed event",
                    if (vevent.location?.value.isNullOrBlank()) "No Location" else vevent.location.value,
                    eventInstance.start.toOffsetDateTime(),
                    eventInstance.end.toOffsetDateTime()
                ).setDescription(addUIDToDescription(vevent.description.value ?: "", eventInstance.uid ?: ""))
            )
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

fun Instant.toIDate(): DateTime {
    return DateTime(Date.from(this))
}

@OptIn(ExperimentalContracts::class)
fun VEvent.equalEnough(dEvent: ScheduledEvent?): Boolean {
    contract {
        returns(true) implies (dEvent != null)
    }
    val periodList = this.getOccurrences()
    // More fields can be added here if needed, but this should be alright
    @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
    return dEvent != null
            && (this.summary?.value ?: "Unnamed event") == dEvent.name
            && periodList.any { dEvent.startTime == it.start && dEvent.endTime == it.end }
}

fun VEvent.getOccurrences(): PeriodList {
    val now = Instant.now()
    return this.calculateRecurrenceSet(Period(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate()))
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

fun OffsetDateTime?.equals(date: java.util.Date) = this?.toInstant()?.toEpochMilli() == date.time
