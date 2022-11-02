package convergence

import com.squareup.moshi.Moshi
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

val testCommand = Command(testChat, "test", { _, _ -> "" }, "Test Help", "Test Syntax")

val localMoshi: Moshi = (SharedVariable.moshiBuilder.defaultValue as Moshi.Builder).build()

class SerializationTests {
    @Test
    fun scheduledCommand() {
        val scheduledCommand = ScheduledCommand(
            Instant.ofEpochMilli(0).atOffset(ZoneOffset.UTC),
            testUser,
            CommandData(testCommand, listOf("test")),
            0
        )

        println(localMoshi.toJson(scheduledCommand))
    }
}
