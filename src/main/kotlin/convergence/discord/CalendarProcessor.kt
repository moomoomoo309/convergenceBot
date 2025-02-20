package convergence.discord

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.util.GenerateQuery
import convergence.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ScheduledEvent
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DateProperty
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
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

object CalendarProcessor {
    fun getAndCacheCalendar(url: String): CalDAVCollection? {
        calendarCache[url]?.let {
            return it
        }
        val cal = CalDAVCollection(url)
        try {
            cal.testConnection(httpClient)
        } catch(e: CalDAV4JException) {
            e.printStackTrace()
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

    private fun removeCalendarSync(calendar: SyncedCalendar): String {
        syncedCalendars.remove(calendar)
        Settings.update()
        return "Could not find guild with ID ${calendar.guildId}"
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
        val discordEvents = getDiscordEventsNextDays(jda, guildId)
        val guild = jda.getGuildById(guildId) ?: return "No guild found with ID $guildId."
        return syncToDiscord(
            guild,
            calendarEvents.sortedBy { it.getNextOccurrence().start },
            discordEvents
        )
    }

    private val uidRegex = Regex("[a-z0-9-]{36}")
    private fun syncToDiscord(guild: Guild, calEvents: List<VEvent>, discordEvents: List<ScheduledEvent>): String {
        val uidMap = mutableMapOf<String, Long>()
        val calEventsById = mutableMapOf<String, VEvent>()
        outer@for (event in calEvents) {
            calEventsById[event.uid.value] = event
            for (currentDiscordEvent in discordEvents) {
                if (event.equalEnough(currentDiscordEvent)) {
                    uidMap[event.uid.value] = currentDiscordEvent.idLong
                    continue@outer
                }
            }
            addCalendarEventToDiscord(uidMap, guild, event)
        }
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

    private fun addCalendarEventToDiscord(uidMap: MutableMap<String, Long>, guild: Guild, event: VEvent) {
        val nextOccurrence = event.getNextOccurrence()
        guild.createScheduledEvent(
            event.summary?.value ?: "Unnamed event",
            if (event.location?.value.isNullOrBlank()) "No Location" else event.location.value,
            nextOccurrence.start.toOffsetDateTime(),
            nextOccurrence.end.toOffsetDateTime()
        ).setDescription(addUIDToDescription(event.description?.value ?: "", event.uid.value)).queue {
            uidMap[event.uid.value] = it.idLong
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
}

fun DateProperty.toInstant(): Instant = this.date.toInstant()
fun DateProperty.toOffsetDateTime(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)
fun Date.toOffsetDateTime(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)
private val defaultZoneOffset = OffsetDateTime.now().offset

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
            && period.start.toInstant() == dEvent.startTime.toInstant()
            && period.end.toInstant() == dEvent.endTime?.toInstant()
            && (this.summary?.value ?: "Unnamed event") == dEvent.name
}

fun VEvent.getNextOccurrence(): Period {
    val now = Instant.now()
    return this.getConsumedTime(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate(), true).first()
}
