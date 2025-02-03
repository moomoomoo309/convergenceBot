import convergence.discord.CalendarProcessor
import kotlin.test.Test

class CalendarProcessorTest {
    @Test
    fun test() {
        val cal = CalendarProcessor.getAndCacheCalendar("https://alpharhoalphasig.mynetgear.com/remote.php/dav/public-calendars/NsdwAWR5cJEpWi4w?export")
        val events = CalendarProcessor.getEventsNextDays(cal, 30)
        for (event in events) {
            println("${event.summary}@${event.startDate.date.toInstant()}: ${event.uid}")
        }
    }

}
