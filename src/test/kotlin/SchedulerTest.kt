import convergence.ScheduledCommand
import convergence.Scheduler
import convergence.User
import convergence.settings
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchedulerTest {

    private val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    @Before
    fun setup() = resetGlobalState()

    @After
    fun teardown() = resetGlobalState()

    // ─── schedule ───────────────────────────────────────────────────────────

    @Test
    fun scheduleReturnsFormattedString() {
        val time = now.plusMinutes(5)
        val result = Scheduler.schedule(testChat, testUser, "echo", listOf("hello"), time)
        assertTrue(result.contains("echo"), "Response should mention the command name")
        assertTrue(result.contains("Scheduled"), "Response should start with 'Scheduled'")
    }

    @Test
    fun scheduleAddsToSerializedCommands() {
        val time = now.plusMinutes(10)
        Scheduler.schedule(testChat, testUser, "ping", listOf(), time)
        assertTrue(settings.serializedCommands.isNotEmpty(), "serializedCommands should not be empty after scheduling")
    }

    @Test
    fun scheduleAssignsIncrementingIds() {
        val time = now.plusMinutes(5)
        Scheduler.schedule(testChat, testUser, "echo", listOf("a"), time)
        Scheduler.schedule(testChat, testUser, "echo", listOf("b"), time)
        val ids = settings.serializedCommands.keys.toList()
        assertEquals(2, ids.size, "Should have 2 scheduled commands")
        assertTrue(ids[0] != ids[1], "IDs should be different")
    }

    // ─── unschedule ─────────────────────────────────────────────────────────

    @Test
    fun unscheduleRemovesExistingCommand() {
        val time = now.plusMinutes(5)
        Scheduler.schedule(testChat, testUser, "ping", listOf(), time)
        val id = settings.serializedCommands.keys.first()
        assertTrue(Scheduler.unschedule(id))
        assertFalse(settings.serializedCommands.containsKey(id))
    }

    @Test
    fun unscheduleReturnsFalseForNonexistentId() {
        assertFalse(Scheduler.unschedule(99999))
    }

    // ─── getCommands ────────────────────────────────────────────────────────

    @Test
    fun getCommandsReturnsAllScheduledCommands() {
        val time = now.plusMinutes(5)
        Scheduler.schedule(testChat, testUser, "echo", listOf("a"), time)
        Scheduler.schedule(testChat, testUser, "echo", listOf("b"), time)
        val all = Scheduler.getCommands()
        assertTrue(all.size >= 2, "Should return at least 2 commands")
    }

    @Test
    fun getCommandsBySenderFiltersCorrectly() {
        val time = now.plusMinutes(5)
        val otherUser = object : User(TestProtocol) {
            override fun toKey() = "TestUser(Other)"
        }
        Scheduler.schedule(testChat, testUser, "echo", listOf("a"), time)
        Scheduler.schedule(testChat, otherUser, "echo", listOf("b"), time)
        val mine = Scheduler.getCommands(testUser)
        assertTrue(mine.all { it.sender == testUser }, "Should only return commands from testUser")
    }

    @Test
    fun getCommandsByNullSenderReturnsAll() {
        val time = now.plusMinutes(5)
        Scheduler.schedule(testChat, testUser, "echo", listOf("a"), time)
        val all = Scheduler.getCommands(null)
        assertTrue(all.isNotEmpty(), "Should return all commands when sender is null")
    }

    @Test
    fun getCommandsWorksCorrectly() {
        val before = Scheduler.getCommands()
        val time = now.plusMinutes(5)
        Scheduler.schedule(testChat, testUser, "ping", listOf(), time)
        val after = Scheduler.getCommands()
        assertTrue(after.size > before.size, "getCommands should reflect newly scheduled command")
    }

    // ─── loadFromFile ───────────────────────────────────────────────────────

    @Test
    fun loadFromFileLoadsSerializedCommands() {
        val time = now.plusMinutes(5)
        val cmd = ScheduledCommand(time, testChat, testUser, "Test", "echo", listOf("hi"), 42)
        settings.serializedCommands[42] = cmd
        Scheduler.loadFromFile()
        val all = Scheduler.getCommands()
        assertTrue(all.any { it.id == 42 }, "Should have loaded command with id 42")
    }
}
