package convergence

import com.joestelmach.natty.Parser
import kotlin.test.assertEquals
import org.junit.Test


val dateTimeParser = Parser()

private val testChat = TestChat()
class TestUser: User(testChat)
private val testUser = TestUser()

class DefaultCommandTests {
    @Test
    fun pingTest() {
        assertEquals("Pong!", ping(testChat, emptyList(), testUser), "Ping command did not respond correctly.")
    }

    @Test
    fun echoTest() {
        val randomString = Math.random().toString()
        assertEquals(randomString, echo(testChat, listOf(randomString), testUser), "Echo command did not respond correctly.")
    }
}