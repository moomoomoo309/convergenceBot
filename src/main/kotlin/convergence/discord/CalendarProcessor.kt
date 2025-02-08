package convergence.discord

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.util.GenerateQuery
import convergence.*
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
    fun getAndCacheCalendar(url: String): CalDAVCollection? {
        calendarCache[url]?.let {
            return it
        }
        val cal = CalDAVCollection(url)
        try {
            cal.testConnection(httpClient)
        } catch(e: CalDAV4JException) {
            return null
        }
        calendarCache[url] = cal
        return cal
    }

    fun getCalDAVEventsNextDays(calCollection: CalDAVCollection, days: Long = 30L): List<VEvent> {
        val cals = calCollection.queryCalendars(httpClient, GenerateQuery().generate())
        val eventList = mutableSetOf<VEvent>()
        for (cal in cals) {
            eventList.addAll(cal.getComponents("VEVENT"))
        }
        val now = Instant.now()
        return eventList.filter {
            it.getConsumedTime(now.toIDate(), now.plus(days, ChronoUnit.DAYS).toIDate()).isNotEmpty()
        }.sortedBy { it.startDate.date }
    }

    private fun getDiscordEventsNextDays(jda: JDA, days: Long = 30L): List<ScheduledEvent> {
        return jda.scheduledEvents.filter {
            it.startTime.isAfter(OffsetDateTime.now())
                    && it.startTime.isBefore(OffsetDateTime.now().plus(days, ChronoUnit.DAYS))
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
        return syncToDiscord(syncedCalendar)
    }

    fun syncToDiscord(cal: SyncedCalendar): String {
        val calCollection = getAndCacheCalendar(cal.calURL) ?: return "No calendar found at URL \"${cal.calURL}\"."
        val calendarEvents = getCalDAVEventsNextDays(calCollection)
        val discordEvents = getDiscordEventsNextDays(jda)
        val guild = jda.getGuildById(cal.guildId) ?: return removeCalendarSync(cal)
        return syncToDiscord(guild, calendarEvents, discordEvents)
    }

    private fun syncToDiscord(guild: Guild, calEvents: List<VEvent>, discordEvents: List<ScheduledEvent>): String {
        val uidMap = mutableMapOf<String, Long>()
        val discordIterator = discordEvents.listIterator()
        val calEventsById = mutableMapOf<String, VEvent>()
        var currentDiscordEvent = discordIterator.next()
        for (event in calEvents) {
            calEventsById[event.uid.value] = event
            while (event.startDate.toInstant().isBefore(currentDiscordEvent.startTime.toInstant())) {
                currentDiscordEvent = discordIterator.next()
            }
            if (event.equalEnough(currentDiscordEvent)) {
                uidMap[event.uid.value] = currentDiscordEvent.idLong
            } else {
                addCalendarEventToDiscord(uidMap, guild, event)
            }
        }
        for (discordEvent in discordEvents) {
            val uid = decodeUID(discordEvent.description?.takeLast(UID_LENGTH))
            if (uidMap[uid] != null || calEventsById[uid]?.equalEnough(discordEvent) != true)
                discordEvent.delete().queue()
        }
        return "Calendar synced successfully."
    }

    private fun addCalendarEventToDiscord(uidMap: MutableMap<String, Long>, guild: Guild, event: VEvent) {
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
        return String(CharArray(uid.length) {
            uid[it].toInvisibleUnicodeEquivalent()
        })
    }

    private fun decodeUID(encodedUID: String?): String? {
        if (encodedUID == null)
            return null
        return String(CharArray(encodedUID.length) {
            encodedUID[it].fromInvisibleUnicodeEquivalent()
        })
    }
}
fun DateProperty.toInstant(): Instant = this.date.toInstant()
fun DateProperty.toOffsetDateTime(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)
private val defaultZoneOffset = OffsetDateTime.now().offset

fun Instant.toIDate(): Date {
    return Date(Date.from(this))
}

fun Char.toInvisibleUnicodeEquivalent(): Char {
    return when (this) {
        '-' -> '\u2063' // Invisible separator
        in '0'..'9' -> (this.code - '0'.code + '\uFE00'.code).toChar() // Variation selector 1-10
        in 'a'..'f' -> (this.code - 'a'.code + '\uFE0A'.code).toChar() // Variation selector 11-16
        else -> this
    }
}

fun Char.fromInvisibleUnicodeEquivalent(): Char {
    return when(this) {
        '\u2063' -> '-'
        in '\uFE00'..'\uFE09' -> (this.code - '\uFE00'.code + '0'.code).toChar()
        in '\uFE0A'..'\uFE0F' -> (this.code - '\uFE0A'.code + 'a'.code).toChar()
        else -> this
    }
}

fun VEvent.equalEnough(dEvent: ScheduledEvent): Boolean {
    // More fields can be added here if needed, but this should be alright
    return this.startDate.toInstant() == dEvent.startTime.toInstant()
            && this.endDate?.toInstant() == dEvent.endTime?.toInstant()
            && this.name == dEvent.name
}
