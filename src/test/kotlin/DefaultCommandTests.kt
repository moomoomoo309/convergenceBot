import com.fasterxml.jackson.module.kotlin.readValue
import convergence.*
import convergence.discord.BrotherInfo
import org.intellij.lang.annotations.Language
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
        assertEquals("Pong!", ping(emptyList(), testChat, testUser), "Ping command did not respond correctly.")
    }

    @Test
    fun echoTest() {
        val randomString = Math.random().toString()
        assertEquals(randomString, echo(listOf(randomString), testChat, testUser), "Echo command did not respond correctly.")
    }
}
