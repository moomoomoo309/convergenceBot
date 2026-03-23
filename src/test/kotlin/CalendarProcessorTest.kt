
import convergence.discord.calendar.CalendarProcessor
import convergence.discord.calendar.DAYS
import convergence.discord.calendar.DiscordEventWrapper
import convergence.discord.calendar.EventInstance
import convergence.discord.calendar.VEventWrapper
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.ScheduledEvent
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import org.junit.Before
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.*

class CalendarProcessorTest {

    @Before
    fun setup() {
        System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            "net.fortuna.ical4j.util.MapTimeZoneCache"
        )
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun makeDateTime(instant: Instant): DateTime {
        val dt = DateTime(instant.toEpochMilli())
        dt.isUtc = true
        return dt
    }

    private fun makeVEvent(
        uid: String,
        summary: String,
        start: Instant,
        end: Instant,
        description: String? = null,
        location: String? = null
    ): VEvent {
        val dtStart = makeDateTime(start)
        val dtEnd = makeDateTime(end)
        val vevent = VEvent(dtStart, dtEnd, summary)
        vevent.properties.add(Uid(uid))
        if (description != null) vevent.properties.add(Description(description))
        if (location != null) vevent.properties.add(Location(location))
        return vevent
    }

    private fun makeCalendar(vararg events: VEvent): Calendar {
        val cal = Calendar()
        cal.properties.add(ProdId("-//Test//Test//EN"))
        cal.properties.add(Version.VERSION_2_0)
        for (event in events) cal.components.add(event)
        return cal
    }

    private fun makeOffsetDateTime(instant: Instant): OffsetDateTime =
        OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun makeDiscordEvent(
        name: String,
        description: String?,
        start: Instant,
        end: Instant?
    ): ScheduledEvent {
        val mock = mockk<ScheduledEvent>()
        every { mock.name } returns name
        every { mock.description } returns description
        every { mock.startTime } returns makeOffsetDateTime(start)
        every { mock.endTime } returns end?.let { makeOffsetDateTime(it) }
        return mock
    }

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    private val soon: Instant = now.plus(1, ChronoUnit.DAYS)
    private val hourLater: Instant = soon.plus(1, ChronoUnit.HOURS)
    private val testUid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    // ─── EventInstance field access ───────────────────────────────────────────

    @Test
    fun eventInstanceUidReturnedFromVEvent() {
        val vevent = makeVEvent(testUid, "Test", soon, hourLater)
        val instance = EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater))
        assertEquals(testUid, instance.uid)
    }

    @Test
    fun eventInstanceSummaryReturnedFromVEvent() {
        val vevent = makeVEvent(testUid, "My Summary", soon, hourLater)
        val instance = EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater))
        assertEquals("My Summary", instance.summary)
    }

    @Test
    fun eventInstanceUidIsNullWhenMissing() {
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val vevent = VEvent(dtStart, dtEnd, "No UID")
        val instance = EventInstance(vevent, dtStart, dtEnd)
        assertNull(instance.uid)
    }

    @Test
    fun eventInstanceSummaryIsNullWhenMissing() {
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val vevent = VEvent()
        vevent.properties.add(Uid(testUid))
        val instance = EventInstance(vevent, dtStart, dtEnd)
        assertNull(instance.summary)
    }

    // ─── VEventWrapper field access ───────────────────────────────────────────

    @Test
    fun vEventWrapperNameFallsBackToUnnamedEvent() {
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val vevent = VEvent(dtStart, dtEnd, null as String?)
        val wrapper = VEventWrapper(EventInstance(vevent, dtStart, dtEnd))
        assertEquals("Unnamed event", wrapper.name)
    }

    @Test
    fun vEventWrapperNameUsesVEventSummary() {
        val vevent = makeVEvent(testUid, "My Event", soon, hourLater)
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("My Event", wrapper.name)
    }

    @Test
    fun vEventWrapperDescriptionDefaultsToEmpty() {
        val vevent = makeVEvent(testUid, "No Desc", soon, hourLater)
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("", wrapper.description)
    }

    @Test
    fun vEventWrapperDescriptionUsesVEventDescription() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater, description = "Some details")
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("Some details", wrapper.description)
    }

    @Test
    fun vEventWrapperStartIsInstantFromEventInstanceStart() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater)
        val dtStart = makeDateTime(soon)
        val wrapper = VEventWrapper(EventInstance(vevent, dtStart, makeDateTime(hourLater)))
        assertEquals(dtStart.toInstant(), wrapper.start)
    }

    @Test
    fun vEventWrapperEndIsInstantFromEventInstanceEnd() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater)
        val dtEnd = makeDateTime(hourLater)
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), dtEnd))
        assertEquals(dtEnd.toInstant(), wrapper.end)
    }

    @Test
    fun vEventWrapperUidDelegatesToEventInstance() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater)
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals(testUid, wrapper.uid)
    }

    @Test
    fun vEventWrapperLocationFallsBackToNoLocation() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater)
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("No Location", wrapper.location)
    }

    @Test
    fun vEventWrapperLocationUsesVEventLocation() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater, location = "123 Main St")
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("123 Main St", wrapper.location)
    }

    // ─── VEventWrapper same-type equality ────────────────────────────────────

    @Test
    fun vEventWrapperEqualsSameTypeWhenAllFieldsMatch() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater, description = "desc")
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val w1 = VEventWrapper(EventInstance(vevent, dtStart, dtEnd))
        val w2 = VEventWrapper(EventInstance(vevent, dtStart, dtEnd))
        assertEquals(w1, w2)
    }

    @Test
    fun vEventWrapperNotEqualWhenDescriptionDiffers() {
        val e1 = makeVEvent(testUid, "Event", soon, hourLater, description = "desc A")
        val e2 = makeVEvent(testUid, "Event", soon, hourLater, description = "desc B")
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val w1 = VEventWrapper(EventInstance(e1, dtStart, dtEnd))
        val w2 = VEventWrapper(EventInstance(e2, dtStart, dtEnd))
        assertNotEquals(w1, w2)
    }

    @Test
    fun vEventWrappersWithSameNameStartEndHaveSameHashCode() {
        val e1 = makeVEvent(testUid, "Event", soon, hourLater, description = "different desc")
        val e2 = makeVEvent(testUid, "Event", soon, hourLater)
        val dtStart = makeDateTime(soon)
        val dtEnd = makeDateTime(hourLater)
        val w1 = VEventWrapper(EventInstance(e1, dtStart, dtEnd))
        val w2 = VEventWrapper(EventInstance(e2, dtStart, dtEnd))
        assertEquals(w1.hashCode(), w2.hashCode())
    }

    // ─── addUIDToDescription ─────────────────────────────────────────────────

    @Test
    fun addUIDToDescriptionWithEmptyDescriptionReturnsUid() {
        val uid = testUid
        assertEquals(uid, CalendarProcessor.addUIDToDescription("", uid))
    }

    @Test
    fun addUIDToDescriptionAlreadyEndsWithUidReturnsUnchanged() {
        val uid = testUid
        val desc = "Some details\n$uid"
        assertEquals(desc, CalendarProcessor.addUIDToDescription(desc, uid))
    }

    @Test
    fun addUIDToDescriptionAppendsUidOnNewLine() {
        val uid = testUid
        val desc = "Some details"
        assertEquals("Some details\n$uid", CalendarProcessor.addUIDToDescription(desc, uid))
    }

    // ─── getCalDAVEventsNextDays: window behavior ─────────────────────────────

    @Test
    fun emptyCalendarListReturnsEmpty() {
        assertEquals(emptyList(), CalendarProcessor.getCalDAVEventsNextDays(emptyList()))
    }

    @Test
    fun calendarWithNoEventsReturnsEmpty() {
        val cal = makeCalendar()
        assertEquals(emptyList(), CalendarProcessor.getCalDAVEventsNextDays(listOf(cal)))
    }

    @Test
    fun singleEventInWindowIsReturned() {
        val vevent = makeVEvent(testUid, "In-Window Event", soon, hourLater)
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(1, result.size)
        assertEquals(testUid, result[0].uid)
        assertEquals("In-Window Event", result[0].summary)
    }

    @Test
    fun eventAfterWindowIsNotReturned() {
        val afterWindow = now.plus(DAYS + 1, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Too Far Future", afterWindow, afterWindow.plus(1, ChronoUnit.HOURS))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(0, result.size)
    }

    @Test
    fun pastEventIsNotReturned() {
        val past = now.minus(2, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Past Event", past, past.plus(1, ChronoUnit.HOURS))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(0, result.size)
    }

    @Test
    fun eventsAreSortedByStartTime() {
        val start1 = now.plus(5, ChronoUnit.DAYS)
        val start2 = now.plus(2, ChronoUnit.DAYS)
        val start3 = now.plus(10, ChronoUnit.DAYS)
        val uid1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val uid2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        val uid3 = "cccccccc-cccc-cccc-cccc-cccccccccccc"
        val e1 = makeVEvent(uid1, "Event 5d", start1, start1.plus(1, ChronoUnit.HOURS))
        val e2 = makeVEvent(uid2, "Event 2d", start2, start2.plus(1, ChronoUnit.HOURS))
        val e3 = makeVEvent(uid3, "Event 10d", start3, start3.plus(1, ChronoUnit.HOURS))
        val cal = makeCalendar(e1, e2, e3)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(3, result.size)
        assertEquals(uid2, result[0].uid, "First result should be the earliest event")
        assertEquals(uid1, result[1].uid, "Second result should be the middle event")
        assertEquals(uid3, result[2].uid, "Third result should be the latest event")
    }

    @Test
    fun eventsFromMultipleCalendarsAreAggregated() {
        val uid1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val uid2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        val e1 = makeVEvent(uid1, "Event A", soon, hourLater)
        val e2 = makeVEvent(uid2, "Event B", soon.plus(2, ChronoUnit.HOURS), hourLater.plus(2, ChronoUnit.HOURS))
        val cal1 = makeCalendar(e1)
        val cal2 = makeCalendar(e2)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal1, cal2))
        assertEquals(2, result.size)
        val resultUids = result.map { it.uid }.toSet()
        assertTrue(uid1 in resultUids, "uid1 should be in results")
        assertTrue(uid2 in resultUids, "uid2 should be in results")
    }

    // ─── getCalDAVEventsNextDays: recurring events ────────────────────────────

    @Test
    fun recurringEventProducesMultipleInstances() {
        val recurrenceStart = now.plus(1, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Weekly Meeting", recurrenceStart, recurrenceStart.plus(1, ChronoUnit.HOURS))
        vevent.properties.add(RRule(Recur("FREQ=WEEKLY;COUNT=3")))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(3, result.size)
        for (instance in result)
            assertEquals(testUid, instance.uid)
        for (i in 0 until result.size - 1)
            assertTrue(result[i].start < result[i + 1].start, "Occurrences should be in ascending order")
    }

    @Test
    fun recurringEventOccurrencesOutsideWindowNotReturned() {
        // Event repeats every 20 days — only occurrences at day 1 and day 21 are within the 30-day window
        val start = now.plus(1, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Long Gap Recurrence", start, start.plus(1, ChronoUnit.HOURS))
        vevent.properties.add(RRule(Recur("FREQ=DAILY;INTERVAL=20;COUNT=3")))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(2, result.size)
    }

    @Test
    fun cancelledExceptionIsExcluded() {
        val recurrenceStart = now.plus(1, ChronoUnit.DAYS)
        val master = makeVEvent(testUid, "Daily Standup", recurrenceStart, recurrenceStart.plus(1, ChronoUnit.HOURS))
        master.properties.add(RRule(Recur("FREQ=DAILY;COUNT=3")))

        // Cancel the second occurrence
        val secondOccurrence = recurrenceStart.plus(1, ChronoUnit.DAYS)
        val cancellation = VEvent()
        cancellation.properties.add(Uid(testUid))
        cancellation.properties.add(RecurrenceId(makeDateTime(secondOccurrence)))
        cancellation.properties.add(Status.VEVENT_CANCELLED)

        val cal = makeCalendar(master, cancellation)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        // Should have 1st and 3rd occurrence; 2nd is cancelled
        assertEquals(2, result.size)
        assertEquals(makeDateTime(recurrenceStart), result[0].start)
        assertEquals(makeDateTime(recurrenceStart.plus(2, ChronoUnit.DAYS)), result[1].start)
    }

    @Test
    fun modifiedExceptionReplacesOriginalOccurrence() {
        val recurrenceStart = now.plus(1, ChronoUnit.DAYS)
        val master = makeVEvent(testUid, "Team Meeting", recurrenceStart, recurrenceStart.plus(1, ChronoUnit.HOURS))
        master.properties.add(RRule(Recur("FREQ=DAILY;COUNT=2")))

        // Modify the second occurrence: shift it 2 hours later
        val secondOccurrence = recurrenceStart.plus(1, ChronoUnit.DAYS)
        val modifiedStart = secondOccurrence.plus(2, ChronoUnit.HOURS)
        val modifiedEnd = modifiedStart.plus(1, ChronoUnit.HOURS)
        val exception = makeVEvent(testUid, "Team Meeting (Modified)", modifiedStart, modifiedEnd)
        exception.properties.add(RecurrenceId(makeDateTime(secondOccurrence)))

        val cal = makeCalendar(master, exception)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(2, result.size)
        assertEquals(makeDateTime(recurrenceStart), result[0].start, "First occurrence should be unchanged")
        assertEquals(makeDateTime(modifiedStart), result[1].start, "Second occurrence should be at modified start time")
        assertEquals(makeDateTime(modifiedEnd), result[1].end, "Second occurrence should have modified end time")
    }

    // ─── DiscordEventWrapper field access ─────────────────────────────────────

    @Test
    fun discordEventWrapperNameDelegatesToScheduledEvent() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("My Event", "desc", soon, hourLater))
        assertEquals("My Event", wrapper.name)
    }

    @Test
    fun discordEventWrapperDescriptionIsEmptyWhenNull() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, hourLater))
        assertEquals("", wrapper.description)
    }

    @Test
    fun discordEventWrapperDescriptionUsesScheduledEventDescription() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", "Details here", soon, hourLater))
        assertEquals("Details here", wrapper.description)
    }

    @Test
    fun discordEventWrapperStartIsInstantFromScheduledEvent() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, hourLater))
        assertEquals(soon, wrapper.start)
    }

    @Test
    fun discordEventWrapperEndIsInstantFromScheduledEvent() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, hourLater))
        assertEquals(hourLater, wrapper.end)
    }

    @Test
    fun discordEventWrapperEndIsNullWhenScheduledEventHasNoEndTime() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, null))
        assertNull(wrapper.end)
    }

    @Test
    fun discordEventWrapperLocationAlwaysReturnsNoLocation() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, hourLater))
        assertEquals("No Location", wrapper.location)
    }

    // ─── DiscordEventWrapper uid extraction ───────────────────────────────────

    @Test
    fun discordEventWrapperUidExtractedFromEndOfDescription() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", "Some event\n$testUid", soon, hourLater))
        assertEquals(testUid, wrapper.uid)
    }

    @Test
    fun discordEventWrapperUidExtractedWhenDescriptionIsOnlyUid() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", testUid, soon, hourLater))
        assertEquals(testUid, wrapper.uid)
    }

    @Test
    fun discordEventWrapperUidIsNullWhenDescriptionIsNull() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", null, soon, hourLater))
        assertNull(wrapper.uid)
    }

    @Test
    fun discordEventWrapperUidIsNullWhenDescriptionDoesNotEndWithValidUid() {
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", "Just some plain text", soon, hourLater))
        assertNull(wrapper.uid)
    }

    @Test
    fun discordEventWrapperUidIsNullWhenLastSegmentContainsUppercaseChars() {
        // uidRegex only matches lowercase hex; uppercase UUID should not match
        val uppercaseUid = testUid.uppercase()
        val wrapper = DiscordEventWrapper(makeDiscordEvent("Event", uppercaseUid, soon, hourLater))
        assertNull(wrapper.uid)
    }

    // ─── DiscordEventWrapper same-type equality ───────────────────────────────

    @Test
    fun discordEventWrapperEqualsSameTypeWhenAllFieldsMatch() {
        val w1 = DiscordEventWrapper(makeDiscordEvent("Event", "desc", soon, hourLater))
        val w2 = DiscordEventWrapper(makeDiscordEvent("Event", "desc", soon, hourLater))
        assertEquals(w1, w2)
    }

    @Test
    fun discordEventWrapperNotEqualWhenNameDiffers() {
        val w1 = DiscordEventWrapper(makeDiscordEvent("Event A", "desc", soon, hourLater))
        val w2 = DiscordEventWrapper(makeDiscordEvent("Event B", "desc", soon, hourLater))
        assertNotEquals(w1, w2)
    }

    @Test
    fun discordEventWrapperNotEqualWhenDescriptionDiffers() {
        val w1 = DiscordEventWrapper(makeDiscordEvent("Event", "desc A", soon, hourLater))
        val w2 = DiscordEventWrapper(makeDiscordEvent("Event", "desc B", soon, hourLater))
        assertNotEquals(w1, w2)
    }

    @Test
    fun discordEventWrapperHashCodeBasedOnNameStartEnd() {
        // hashCode uses name, start, end — description differences should not affect it
        val w1 = DiscordEventWrapper(makeDiscordEvent("Event", "desc A", soon, hourLater))
        val w2 = DiscordEventWrapper(makeDiscordEvent("Event", "desc B", soon, hourLater))
        assertEquals(w1.hashCode(), w2.hashCode())
    }

    // ─── Cross-type equality ──────────────────────────────────────────────────

    @Test
    fun vEventWrapperEqualsDiscordEventWrapperWhenNameStartEndMatch() {
        val vevent = makeVEvent(testUid, "Shared Event", soon, hourLater)
        val vWrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        val dWrapper = DiscordEventWrapper(makeDiscordEvent("Shared Event", "some desc", soon, hourLater))
        assertEquals<Any>(vWrapper, dWrapper)
    }

    @Test
    fun vEventWrapperNotEqualToDiscordEventWrapperWhenNameDiffers() {
        val vevent = makeVEvent(testUid, "VEvent Name", soon, hourLater)
        val vWrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        val dWrapper = DiscordEventWrapper(makeDiscordEvent("Different Name", "desc", soon, hourLater))
        assertNotEquals<Any>(vWrapper, dWrapper)
    }

    @Test
    fun discordEventWrapperEqualsVEventWrapperWhenNameStartEndMatch() {
        val vevent = makeVEvent(testUid, "Shared Event", soon, hourLater)
        val vWrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        val dWrapper = DiscordEventWrapper(makeDiscordEvent("Shared Event", "some desc", soon, hourLater))
        assertEquals<Any>(dWrapper, vWrapper)
    }

    @Test
    fun discordEventWrapperNotEqualToVEventWrapperWhenStartDiffers() {
        val laterStart = soon.plus(3, ChronoUnit.HOURS)
        val vevent = makeVEvent(testUid, "Event", soon, hourLater)
        val vWrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        val dWrapper = DiscordEventWrapper(makeDiscordEvent("Event", "desc", laterStart, hourLater))
        assertNotEquals<Any>(dWrapper, vWrapper)
    }

    // ─── VEventWrapper blank location ─────────────────────────────────────────

    @Test
    fun vEventWrapperLocationFallsBackToNoLocationWhenBlank() {
        val vevent = makeVEvent(testUid, "Event", soon, hourLater, location = "   ")
        val wrapper = VEventWrapper(EventInstance(vevent, makeDateTime(soon), makeDateTime(hourLater)))
        assertEquals("No Location", wrapper.location)
    }

    // ─── getCalDAVEventsNextDays: window boundary behavior ────────────────────

    @Test
    fun eventStartingExactlyAtWindowEdgeIsNotReturned() {
        // An event starting exactly DAYS days from now is outside the half-open window [now, now+DAYS)
        val atEdge = Instant.now().plus(DAYS, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Edge Event", atEdge, atEdge.plus(1, ChronoUnit.HOURS))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(0, result.size)
    }

    @Test
    fun eventJustBeforeWindowEdgeIsReturned() {
        val justBefore = Instant.now().plus(DAYS - 1, ChronoUnit.DAYS)
        val vevent = makeVEvent(testUid, "Near-Edge Event", justBefore, justBefore.plus(1, ChronoUnit.HOURS))
        val cal = makeCalendar(vevent)
        val result = CalendarProcessor.getCalDAVEventsNextDays(listOf(cal))
        assertEquals(1, result.size)
    }
}
