import convergence.User
import convergence.echo
import convergence.ping
import org.junit.Test
import kotlin.test.assertEquals


val testChat = TestChat()

class TestUser: User(TestProtocol) {
    override fun toKey(): String {
        TODO("Not yet implemented")
    }
}

val testUser = TestUser()

class DefaultCommandTests {
    @Test
    fun pingTest() {
        assertEquals("Pong!", ping(), "Ping command did not respond correctly.")
    }

    @Test
    fun echoTest() {
        val randomString = Math.random().toString()
        assertEquals(randomString, echo(listOf(randomString)), "Echo command did not respond correctly.")
    }
}
