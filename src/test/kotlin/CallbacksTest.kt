import convergence.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallbacksTest {

    // ─── registerCallback ───────────────────────────────────────────────────

    @Test
    fun registerCallbackAddsToList() {
        val initialSize = callbacks[StartedTyping::class]?.size ?: 0
        val callback = StartedTyping { _, _ -> true }
        registerCallback(callback)
        assertEquals(initialSize + 1, callbacks[StartedTyping::class]?.size)
    }

    @Test
    fun registerCallbackCreatesListForNewEventClass() {
        val initialSize = callbacks[ReadByUser::class]?.size ?: 0
        val callback = ReadByUser { _, _, _ -> true }
        registerCallback(callback)
        assertEquals(initialSize + 1, callbacks[ReadByUser::class]?.size)
    }

    // ─── ChatEvent invoke (typed) ───────────────────────────────────────────

    @Test
    fun changedNicknameCallbackInvokesFunction() {
        var called = false
        val callback = ChangedNickname { _, _, _ -> called = true; true }
        callback.invoke(testChat, testUser, "oldname")
        assertTrue(called)
    }

    @Test
    fun editMessageCallbackInvokesFunction() {
        var calledWith: Triple<String, User, String>? = null
        val callback = EditMessage { old, sender, new -> calledWith = Triple(old, sender, new); true }
        callback.invoke("old", testUser, "new")
        assertEquals(Triple("old", testUser, "new"), calledWith)
    }

    @Test
    fun startedTypingCallbackInvokesFunction() {
        var calledChat: Chat? = null
        val callback = StartedTyping { chat, _ -> calledChat = chat; true }
        callback.invoke(testChat, testUser)
        assertEquals(testChat, calledChat)
    }

    @Test
    fun stoppedTypingCallbackInvokesFunction() {
        var called = false
        val callback = StoppedTyping { _, _ -> called = true; true }
        callback.invoke(testChat, testUser)
        assertTrue(called)
    }

    @Test
    fun changedAvailabilityCallbackInvokesFunction() {
        var calledAvail: Availability? = null
        val avail = object : Availability("Online") {}
        val callback = ChangedAvailability { _, _, a -> calledAvail = a; true }
        callback.invoke(testChat, testUser, avail)
        assertEquals(avail, calledAvail)
    }

    // ─── ChatEvent invoke (vararg) ──────────────────────────────────────────

    @Test
    fun startedTypingCallbackVarargInvoke() {
        var called = false
        val callback = StartedTyping { _, _ -> called = true; true }
        (callback as ChatEvent).invoke(testChat, testUser)
        assertTrue(called)
    }

    @Test
    fun stoppedTypingCallbackVarargInvoke() {
        var called = false
        val callback = StoppedTyping { _, _ -> called = true; true }
        (callback as ChatEvent).invoke(testChat, testUser)
        assertTrue(called)
    }

    @Test
    fun editMessageCallbackVarargInvoke() {
        var called = false
        val callback = EditMessage { _, _, _ -> called = true; true }
        (callback as ChatEvent).invoke("old", testUser, "new")
        assertTrue(called)
    }

    @Test
    fun changedAvailabilityCallbackVarargInvoke() {
        var called = false
        val avail = object : Availability("Away") {}
        val callback = ChangedAvailability { _, _, _ -> called = true; true }
        (callback as ChatEvent).invoke(testChat, testUser, avail)
        assertTrue(called)
    }

    // ─── runCallbacks ──────────────────────────────────────────────────────

    @Test
    fun runCallbacksReturnsMatchingCallbacks() {
        var callCount = 0
        val cb = StartedTyping { _, _ -> callCount++; true }
        registerCallback(cb)
        runCallbacks<StartedTyping>(testChat, testUser)
        assertTrue(callCount >= 1, "Callback should have been called at least once")
    }

    @Test
    fun runCallbacksWithWrongArgCountThrows() {
        val cb = StartedTyping { _, _ -> true }
        registerCallback(cb)
        assertFailsWith(IllegalArgumentException::class) {
            runCallbacks<StartedTyping>(testChat)
        }
    }

    // ─── invokeTyped error handling ─────────────────────────────────────────

    @Test
    fun invokeTyped1ThrowsOnZeroArgs() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String) -> Boolean = { true }
            invokeTyped(fct, emptyArray())
        }
    }

    @Test
    fun invokeTyped2ThrowsOnOneArg() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String, String) -> Boolean = { _, _ -> true }
            invokeTyped(fct, arrayOf("a"))
        }
    }

    @Test
    fun invokeTyped3ThrowsOnTwoArgs() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String, String, String) -> Boolean = { _, _, _ -> true }
            invokeTyped(fct, arrayOf("a", "b"))
        }
    }

    @Test
    fun invokeTyped4ThrowsOnThreeArgs() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String, String, String, String) -> Boolean = { _, _, _, _ -> true }
            invokeTyped(fct, arrayOf("a", "b", "c"))
        }
    }

    @Test
    fun invokeTyped5ThrowsOnFourArgs() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String, String, String, String, String) -> Boolean = { _, _, _, _, _ -> true }
            invokeTyped(fct, arrayOf("a", "b", "c", "d"))
        }
    }

    @Test
    fun invokeTyped6ThrowsOnFiveArgs() {
        assertFailsWith(IllegalArgumentException::class) {
            val fct: (String, String, String, String, String, String) -> Boolean = { _, _, _, _, _, _ -> true }
            invokeTyped(fct, arrayOf("a", "b", "c", "d", "e"))
        }
    }

    @Test
    fun invokeTyped1ReturnsCorrectValue() {
        val fct: (Int) -> Boolean = { it > 5 }
        assertTrue(invokeTyped(fct, arrayOf<Any>(10)))
        assertFalse(invokeTyped(fct, arrayOf<Any>(3)))
    }

    @Test
    fun invokeTyped2ReturnsCorrectValue() {
        val fct: (String, Int) -> Boolean = { s, i -> s.length == i }
        assertTrue(invokeTyped(fct, arrayOf<Any>("abc", 3)))
        assertFalse(invokeTyped(fct, arrayOf<Any>("abc", 5)))
    }

    @Test
    fun invokeTyped3ReturnsCorrectValue() {
        val fct: (Int, Int, Int) -> Boolean = { a, b, c -> a + b == c }
        assertTrue(invokeTyped(fct, arrayOf<Any>(1, 2, 3)))
        assertFalse(invokeTyped(fct, arrayOf<Any>(1, 2, 4)))
    }

    @Test
    fun invokeTyped4ReturnsCorrectValue() {
        val fct: (Int, Int, Int, Int) -> Boolean = { a, b, c, d -> a + b + c == d }
        assertTrue(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 6)))
        assertFalse(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 7)))
    }

    @Test
    fun invokeTyped5ReturnsCorrectValue() {
        val fct: (Int, Int, Int, Int, Int) -> Boolean = { a, b, c, d, e -> a + b + c + d == e }
        assertTrue(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 4, 10)))
        assertFalse(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 4, 11)))
    }

    @Test
    fun invokeTyped6ReturnsCorrectValue() {
        val fct: (Int, Int, Int, Int, Int, Int) -> Boolean = { a, b, c, d, e, f -> a + b + c + d + e == f }
        assertTrue(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 4, 5, 15)))
        assertFalse(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 4, 5, 16)))
    }

    @Test
    fun invokeTyped6WithMoreThan6ArgsLogsAndCallsWithCorrect() {
        val fct: (Int, Int, Int, Int, Int, Int) -> Boolean = { a, b, c, d, e, f -> a + b + c + d + e + f == 21 }
        assertTrue(invokeTyped(fct, arrayOf<Any>(1, 2, 3, 4, 5, 6, 99, 100)))
    }
}
