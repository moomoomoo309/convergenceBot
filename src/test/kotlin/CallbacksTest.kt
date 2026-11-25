
import convergence.callbacks.*
import convergence.model.Availability
import convergence.model.Emoji
import convergence.model.Image
import convergence.model.IncomingMessage
import convergence.model.MessageHistory
import convergence.model.SimpleIncomingMessage
import convergence.model.SimpleOutgoingMessage
import convergence.model.Sticker
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests the typed per-event classes in [convergence.callbacks] that replaced the
 * old untyped `callbacks` map / `registerCallback` / `runCallbacks` machinery.
 *
 * Handlers are registered with [register] and dispatched with [fire]; [fire] ignores
 * the Boolean each handler returns (it exists only for source compatibility with the
 * old callback shape).
 */
class CallbacksTest {

    private class TestSticker : Sticker("test-sticker", null)
    private class TestAvailability : Availability("Online")
    private class TestEmoji : Emoji {
        override fun asString() = "test-emoji"
    }
    private class TestHistory :
        MessageHistory<SimpleIncomingMessage>(SimpleIncomingMessage("hi"), OffsetDateTime.now(), testUser)

    // ─── register / handlerCount ────────────────────────────────────────────

    @Test
    fun registerAddsHandler() {
        val event = StartedTypingEvent<TestChat, TestUser>()
        assertEquals(0, event.handlerCount)
        event.register { _, _ -> true }
        assertEquals(1, event.handlerCount)
    }

    @Test
    fun registerAddsHandlerToSecondEventClass() {
        val event = ReadByUserEvent<TestChat, TestUser>()
        assertEquals(0, event.handlerCount)
        event.register { _, _, _ -> true }
        assertEquals(1, event.handlerCount)
    }

    @Test
    fun registerAddsMultipleHandlers() {
        val event = StoppedTypingEvent<TestChat, TestUser>()
        event.register { _, _ -> true }
        event.register { _, _ -> true }
        assertEquals(2, event.handlerCount)
    }

    // ─── fire invokes handlers with the right arguments ─────────────────────

    @Test
    fun changedNicknameFirePassesArguments() {
        var calledWith: Triple<TestChat, TestUser, String>? = null
        val event = ChangedNicknameEvent<TestChat, TestUser>()
        event.register { chat, user, oldName -> calledWith = Triple(chat, user, oldName); true }
        event.fire(testChat, testUser, "oldname")
        assertEquals(Triple(testChat, testUser, "oldname"), calledWith)
    }

    @Test
    fun editMessageFirePassesArguments() {
        var calledWith: Triple<String, TestUser, String>? = null
        val event = EditMessageEvent<TestUser>()
        event.register { old, sender, new -> calledWith = Triple(old, sender, new); true }
        event.fire("old", testUser, "new")
        assertEquals(Triple("old", testUser, "new"), calledWith)
    }

    @Test
    fun startedTypingFirePassesChat() {
        var calledChat: TestChat? = null
        val event = StartedTypingEvent<TestChat, TestUser>()
        event.register { chat, _ -> calledChat = chat; true }
        event.fire(testChat, testUser)
        assertEquals(testChat, calledChat)
    }

    @Test
    fun stoppedTypingFireInvokesHandler() {
        var called = false
        val event = StoppedTypingEvent<TestChat, TestUser>()
        event.register { _, _ -> called = true; true }
        event.fire(testChat, testUser)
        assertTrue(called)
    }

    @Test
    fun changedAvailabilityFirePassesAvailability() {
        var calledAvail: Availability? = null
        val avail = TestAvailability()
        val event = ChangedAvailabilityEvent<TestChat, TestUser>()
        event.register { _, _, a -> calledAvail = a; true }
        event.fire(testChat, testUser, avail)
        assertEquals(avail, calledAvail)
    }

    @Test
    fun receivedImagesFirePassesMessageAndImages() {
        var calledMessage: IncomingMessage<*>? = null
        var calledImages: Array<out Image>? = null
        val images = arrayOf<Image>(object : Image() {
            override fun getURL() = URI("https://example.com/img")
            override fun getStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        })
        val event = ReceivedImagesEvent<TestChat, TestUser>()
        event.register { _, _, msg, imgs -> calledMessage = msg; calledImages = imgs; true }
        event.fire(testChat, testUser, SimpleIncomingMessage("body"), images)
        assertTrue(calledMessage is SimpleIncomingMessage, "handler should receive the message")
        val received = assertNotNull(calledImages, "handler should receive the images")
        assertTrue(received.contentEquals(images), "handler should receive the images")
    }

    @Test
    fun mentionedUsersFirePassesUserList() {
        var calledUsers: List<Any>? = null
        val event = MentionedUserEvent<TestChat, TestUser, SimpleIncomingMessage>()
        event.register { _, _, _, users -> calledUsers = users; true }
        event.fire(testChat, testUser, SimpleIncomingMessage("hi @bob"), listOf(testUser))
        assertEquals(listOf(testUser), calledUsers)
    }

    @Test
    fun receivedStickerFirePassesSticker() {
        var calledSticker: Sticker? = null
        val sticker = TestSticker()
        val event = ReceivedStickerEvent<TestChat, TestUser>()
        event.register { _, _, s -> calledSticker = s; true }
        event.fire(testChat, testUser, sticker)
        assertEquals(sticker, calledSticker)
    }

    @Test
    fun readByUserFirePassesMessageHistory() {
        var calledHistory: MessageHistory<*>? = null
        val history = TestHistory()
        val event = ReadByUserEvent<TestChat, TestUser>()
        event.register { _, _, m -> calledHistory = m; true }
        event.fire(testChat, testUser, history)
        assertEquals(history, calledHistory)
    }

    @Test
    fun reactionChangedFirePassesAllArguments() {
        var calledWith: Quintuple? = null
        val emoji = TestEmoji()
        val event = ReactionChangedEvent<TestChat, TestUser, SimpleIncomingMessage>()
        event.register { sender, chat, msg, e, oldAmount, newAmount ->
            calledWith = Quintuple(sender, chat, msg, e, oldAmount to newAmount)
            true
        }
        event.fire(testUser, testChat, SimpleIncomingMessage("reacted"), emoji, 1, 2)
        val captured = assertNotNull(calledWith, "handler should have been invoked")
        assertEquals(testUser, captured.sender)
        assertEquals(testChat, captured.chat)
        assertEquals(emoji, captured.emoji)
        assertEquals(1 to 2, captured.counts)
    }

    // ─── dispatch semantics ─────────────────────────────────────────────────

    @Test
    fun fireInvokesAllRegisteredHandlers() {
        var callCount = 0
        val event = StartedTypingEvent<TestChat, TestUser>()
        event.register { _, _ -> callCount++; true }
        event.register { _, _ -> callCount++; true }
        event.fire(testChat, testUser)
        assertEquals(2, callCount, "every registered handler should run")
    }

    @Test
    fun fireIgnoresHandlerReturnValue() {
        // Handlers return Boolean only for source compatibility with the old callback
        // API; a false return must not prevent later handlers from running.
        var secondCalled = false
        val event = StartedTypingEvent<TestChat, TestUser>()
        event.register { _, _ -> false }
        event.register { _, _ -> secondCalled = true; true }
        event.fire(testChat, testUser)
        assertTrue(secondCalled, "a false-returning handler must not stop later handlers")
    }

    @Test
    fun eventsAreIndependent() {
        // Registering on one event must not leak into another event of the same type.
        val eventA = StartedTypingEvent<TestChat, TestUser>()
        val eventB = StartedTypingEvent<TestChat, TestUser>()
        eventA.register { _, _ -> true }
        assertEquals(1, eventA.handlerCount)
        assertEquals(0, eventB.handlerCount)
    }

    private data class Quintuple(
        val sender: TestUser,
        val chat: TestChat,
        val message: SimpleIncomingMessage,
        val emoji: Emoji,
        val counts: Pair<Int, Int>
    )
}
