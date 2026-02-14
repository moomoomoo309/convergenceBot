
import convergence.discord.calendar.CalendarProcessor
import convergence.discord.calendar.httpClient
import java.time.Instant
import kotlin.test.Test

class CalendarProcessorTest {
    val url = "https://alpharhoalphasig.mynetgear.com/remote.php/dav/public-calendars/enPn8E9mfbCsEsZk?export"

    @Test
    fun test() {
        val calCollection = CalendarProcessor.getAndCacheCalendar(url)
        val cals = calCollection!!.queryCalendars(httpClient, CalendarProcessor.buildCalendarQuery(Instant.now()))
        val events = CalendarProcessor.getCalDAVEventsNextDays(cals)
        for ((event, start) in events) {
            println("${event.summary.value}\nstartDate:${event.startDate.date.toInstant()}\n" +
                        "occurrence: ${start.toInstant()}\nUID: ${event.uid.value}")
            //println(objectMapper.writeValueAsString(event))
        }
    }
}
