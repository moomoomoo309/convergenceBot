package convergence

import org.junit.Test
import kotlin.test.assertEquals


private val testChat = TestChat()
class TestUser: User(testChat) {
    override fun serialize() = "{\"type\":\"TestUser\"}"

    companion object {
        init {
            deserializationFunctions["TestUser"] = { TestUser() }
        }
    }
}

private val testUser = TestUser()

class DefaultCommandTests {
    @Test
    fun pingTest() {
        assertEquals("Pong!", ping(emptyList(), testUser), "Ping command did not respond correctly.")
    }

    @Test
    fun echoTest() {
        val randomString = Math.random().toString()
        assertEquals(randomString, echo(listOf(randomString), testUser), "Echo command did not respond correctly.")
    }
}