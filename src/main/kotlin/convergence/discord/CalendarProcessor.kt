package convergence.discord

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.util.GenerateQuery
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ScheduledEvent
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DateProperty
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

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

object CalendarProcessor {
    fun getAndCacheCalendar(url: String): CalDAVCollection {
        calendarCache[url]?.let {
            return it
        }
        val cal = CalDAVCollection(url)
        calendarCache[url] = cal
        return cal
    }

    fun getEventsNextDays(calCollection: CalDAVCollection, days: Number): List<VEvent> {
        val cals = calCollection.queryCalendars(httpClient, GenerateQuery().generate())
        val eventList = mutableSetOf<VEvent>()
        for (cal in cals) {
            eventList.addAll(cal.getComponents("VEVENT"))
        }
        val now = Instant.now()
        return eventList.filter {
            it.getConsumedTime(now.toIDate(), now.plus(days.toLong(), ChronoUnit.DAYS).toIDate()).isNotEmpty()
        }.sortedBy { it.startDate.date }
    }

    fun getDiscordEvents(jda: JDA, withinNextDays: Long): List<ScheduledEvent> {
        return jda.scheduledEvents.filter {
            it.startTime.isAfter(OffsetDateTime.now())
                    && it.startTime.isBefore(OffsetDateTime.now().plus(withinNextDays, ChronoUnit.DAYS))
        }.sortedBy { it.startTime }
    }

    fun syncToDiscord(jda: JDA, guild: Guild, calEvents: List<VEvent>, discordEvents: List<ScheduledEvent>) {
        val uidMap = mutableMapOf<String, Long>()
        val discordIterator = discordEvents.listIterator()
        var currentDiscordEvent = discordIterator.next()
        for (event in calEvents) {
            while (event.startDate.date.toInstant().isBefore(currentDiscordEvent.startTime.toInstant())) {
                currentDiscordEvent = discordIterator.next()
            }
            if (event.startDate.date.toInstant() == currentDiscordEvent.startTime.toInstant()) {
                uidMap[event.uid.value] = currentDiscordEvent.idLong
            } else {
                addCalendarEventToDiscord(uidMap, jda, guild, event)
            }
        }
        for (discordEvent in discordEvents) {
            uidMap[decodeUID(discordEvent.description?.takeLast(UID_LENGTH))] ?: discordEvent.delete().queue()
        }
    }

    fun addCalendarEventToDiscord(uidMap: MutableMap<String, Long>, jda: JDA, guild: Guild, event: VEvent) {
        guild.createScheduledEvent(
            event.name,
            event.location.value,
            event.startDate.toOffsetDateTime(),
            event.endDate.toOffsetDateTime()
        ).setDescription(addUIDToDescription(event.description.value, event.uid.value)).queue {
            uidMap[event.uid.value] = it.idLong
        }
    }

    private fun addUIDToDescription(description: String?, uid: String): String {
        val encodedUID = encodeUID(uid)
        if (description.isNullOrEmpty()) {
            return encodedUID
        } else if (description.endsWith(encodedUID)) {
            return description
        }
        return description + "\n" + encodedUID
    }

    private fun encodeUID(uid: String): String {
        return uid
    }

    private fun decodeUID(encodedUID: String?): String? {
        if (encodedUID == null)
            return null
        return encodedUID
    }
}
fun DateProperty.toOffsetDateTime(): OffsetDateTime = this.date.toInstant().atOffset(defaultZoneOffset)
private val defaultZoneOffset = OffsetDateTime.now().offset

fun Instant.toIDate(): Date {
    return Date(Date.from(this))
}
