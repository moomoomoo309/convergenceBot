
import convergence.callbacks.*
import convergence.model.Availability
import convergence.model.Chat
import convergence.model.User
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertFailsWith(IndexOutOfBoundsException::class) {
            runCallbacks<StartedTyping>(testChat)
        }
    }
}
