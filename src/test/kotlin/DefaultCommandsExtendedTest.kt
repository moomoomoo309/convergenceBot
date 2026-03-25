
import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Extended unit tests for the default commands defined in DefaultCommands.kt.
 *
 * Each test group is isolated: global state (timers, aliases, linkedChats, chatMap,
 * commands, sortedHelpText, commandDelimiters) is reset before and after every test.
 */
class DefaultCommandsExtendedTest {

    /**
     * A chat whose protocol is TestProtocol rather than UniversalProtocol.
     * Used in tests where we need to distinguish between the two so that
     * commands()/aliases() don't double-count UniversalProtocol entries.
     */
    private val testProtocolChat = object : Chat(TestProtocol, "TestProtocolChat") {
        override fun toKey() = "TestChat(TestProtocolChat)"
    }

    @Before
    fun setup() {
        timers.clear()
        aliases.clear()
        linkedChats.clear()
        chatMap.clear()
        reverseChatMap.clear()
        sortedHelpText.clear()
        commands.remove(TestProtocol)
        commands.remove(UniversalProtocol)
        commandDelimiters.clear()
        serializedCommands.clear()
    }

    @After
    fun teardown() {
        timers.clear()
        aliases.clear()
        linkedChats.clear()
        chatMap.clear()
        reverseChatMap.clear()
        sortedHelpText.clear()
        commands.remove(TestProtocol)
        commands.remove(UniversalProtocol)
        commandDelimiters.clear()
        serializedCommands.clear()
    }

    // ─── ping ─────────────────────────────────────────────────────────────────

    @Test
    fun pingReturnsPong() {
        assertEquals("Pong!", ping())
    }

    // ─── echo ─────────────────────────────────────────────────────────────────

    @Test
    fun echoJoinsArgsWithSpaces() {
        assertEquals("hello world", echo(listOf("hello", "world")))
    }

    @Test
    fun echoSingleArgument() {
        assertEquals("hello", echo(listOf("hello")))
    }

    @Test
    fun echoEmptyArgsList() {
        assertEquals("", echo(listOf()))
    }

    // ─── createTimer ──────────────────────────────────────────────────────────

    @Test
    fun createTimerRequiresName() {
        assertEquals("A name has to be provided.", createTimer(listOf()))
    }

    @Test
    fun createTimerSucceeds() {
        val result = createTimer(listOf("myTimer"))
        assertEquals("New timer \"myTimer\" created.", result)
        assertTrue("myTimer" in timers)
    }

    @Test
    fun createTimerSupportsMultiWordNames() {
        val result = createTimer(listOf("my", "timer"))
        assertEquals("New timer \"my timer\" created.", result)
        assertTrue("my timer" in timers)
    }

    @Test
    fun createTimerRejectsDuplicateName() {
        createTimer(listOf("dupeTimer"))
        assertEquals("That timer already exists!", createTimer(listOf("dupeTimer")))
    }

    // ─── resetTimer ───────────────────────────────────────────────────────────

    @Test
    fun resetTimerRequiresName() {
        assertEquals("A name has to be provided.", resetTimer(listOf()))
    }

    @Test
    fun resetTimerFailsForUnknownName() {
        assertEquals("That timer doesn't exist!", resetTimer(listOf("ghost")))
    }

    @Test
    fun resetTimerUpdatesTimestamp() {
        val pastTime = OffsetDateTime.now().minusMinutes(5)
        timers["myTimer"] = pastTime
        val result = resetTimer(listOf("myTimer"))
        assertTrue(result.startsWith("Timer reset."), "Expected 'Timer reset.' prefix, got: $result")
        assertTrue(timers["myTimer"]!!.isAfter(pastTime), "Expected timestamp to advance after reset")
    }

    @Test
    fun resetTimerReportsOldTimestamp() {
        val fixedTime = OffsetDateTime.now().minusHours(1)
        timers["myTimer"] = fixedTime
        val result = resetTimer(listOf("myTimer"))
        assertTrue(result.contains(fixedTime.toString()), "Expected old timestamp in reset message")
    }

    // ─── checkTimer ───────────────────────────────────────────────────────────

    @Test
    fun checkTimerRequiresName() {
        assertEquals("A name has to be provided.", checkTimer(listOf()))
    }

    @Test
    fun checkTimerFailsForUnknownName() {
        assertEquals("That timer doesn't exist!", checkTimer(listOf("ghost")))
    }

    @Test
    fun checkTimerReturnsCreationInfo() {
        val fixedTime = OffsetDateTime.now().minusMinutes(10)
        timers["myTimer"] = fixedTime
        val result = checkTimer(listOf("myTimer"))
        assertTrue(result.startsWith("The time it was created"), "Unexpected prefix: $result")
        assertTrue(result.contains(fixedTime.toString()), "Expected fixed time in result")
    }

    @Test
    fun checkTimerDoesNotModifyTimestamp() {
        val fixedTime = OffsetDateTime.now().minusMinutes(10)
        timers["myTimer"] = fixedTime
        checkTimer(listOf("myTimer"))
        assertEquals(fixedTime, timers["myTimer"], "checkTimer should not modify the stored time")
    }

    // ─── removeAlias ──────────────────────────────────────────────────────────

    @Test
    fun removeAliasRequiresExactlyOneArg() {
        assertEquals("Only one argument should be passed.", removeAlias(listOf(), testChat))
        assertEquals("Only one argument should be passed.", removeAlias(listOf("a", "b"), testChat))
    }

    @Test
    fun removeAliasFailsWhenNotRegistered() {
        assertEquals("No alias with name \"ghost\" found.", removeAlias(listOf("ghost"), testChat))
    }

    @Test
    fun removeAliasSucceeds() {
        val cmd = Command.of(UniversalProtocol, "echo", listOf(), ::echo, "test", "test")
        aliases[testChat] = mutableMapOf("mything" to Alias(testChat, "mything", cmd, listOf()))
        assertEquals("Alias \"mything\" removed.", removeAlias(listOf("mything"), testChat))
        assertTrue(aliases[testChat].isNullOrEmpty(), "Alias entry should be removed from the map")
    }

    @Test
    fun removeAliasCaseSensitiveLookup() {
        // Keys are stored lowercase; passing a different case should not find the alias.
        val cmd = Command.of(UniversalProtocol, "echo", listOf(), ::echo, "test", "test")
        aliases[testChat] = mutableMapOf("mything" to Alias(testChat, "mything", cmd, listOf()))
        assertEquals("No alias with name \"MyThing\" found.", removeAlias(listOf("MyThing"), testChat))
    }

    // ─── removeServerAlias ────────────────────────────────────────────────────

    @Test
    fun removeServerAliasRejectsNonServerProtocol() {
        // testChat uses UniversalProtocol which does not implement HasServer
        assertEquals(
            "This protocol doesn't support servers!",
            removeServerAlias(listOf("myAlias"), testChat)
        )
    }

    // ─── unschedule ───────────────────────────────────────────────────────────

    @Test
    fun unscheduleRejectsNonIntegerId() {
        assertEquals("abc is not an event ID!", unschedule(listOf("abc")))
    }

    @Test
    fun unscheduleReportsNotFoundForMissingId() {
        assertEquals("No event with index 9999 found.", unschedule(listOf("9999")))
    }

    // ─── link ─────────────────────────────────────────────────────────────────

    @Test
    fun linkRejectsNonIntegerId() {
        assertEquals("abc is not a chat ID!", link(listOf("abc"), testChat))
    }

    @Test
    fun linkReportsNotFoundForMissingId() {
        assertEquals("No chat with ID 9999 found.", link(listOf("9999"), testChat))
    }

    @Test
    fun linkAddsChatToLinkedChats() {
        val otherChat = object : Chat(UniversalProtocol, "Other") {
            override fun toKey() = "TestChat(Other)"
        }
        chatMap[42] = otherChat
        reverseChatMap[otherChat] = 42
        val result = link(listOf("42"), testChat)
        assertEquals("${otherChat.name} linked to ${testChat.name}.", result)
        assertTrue(testChat in linkedChats, "testChat should now be in linkedChats")
        assertTrue(otherChat in linkedChats[testChat]!!, "otherChat should be linked to testChat")
    }

    @Test
    fun linkCanLinkMultipleChats() {
        val chat1 = object : Chat(UniversalProtocol, "C1") { override fun toKey() = "TestChat(C1)" }
        val chat2 = object : Chat(UniversalProtocol, "C2") { override fun toKey() = "TestChat(C2)" }
        chatMap[1] = chat1
        chatMap[2] = chat2
        link(listOf("1"), testChat)
        link(listOf("2"), testChat)
        assertEquals(2, linkedChats[testChat]?.size, "Both chats should be linked")
    }

    // ─── unlink ───────────────────────────────────────────────────────────────

    @Test
    fun unlinkRejectsNonIntegerId() {
        assertEquals("abc is not a chat ID!", unlink(listOf("abc"), testChat))
    }

    @Test
    fun unlinkReportsNotFoundForMissingId() {
        assertEquals("No chat with ID 9999 found.", unlink(listOf("9999"), testChat))
    }

    @Test
    fun unlinkFailsWhenNoLinksExist() {
        val chat = object : Chat(UniversalProtocol, "C") { override fun toKey() = "TestChat(C)" }
        chatMap[42] = chat
        assertEquals("There are no chats linked to this one!", unlink(listOf("42"), testChat))
    }

    @Test
    fun unlinkFailsWhenChatIsNotLinked() {
        val chatInMap = object : Chat(UniversalProtocol, "InMap") { override fun toKey() = "TestChat(InMap)" }
        val chatLinked = object : Chat(UniversalProtocol, "Linked") { override fun toKey() = "TestChat(Linked)" }
        chatMap[42] = chatInMap
        chatMap[43] = chatLinked
        linkedChats[testChat] = mutableSetOf(chatLinked) // only chatLinked is linked
        assertEquals("That chat isn't linked to this one!", unlink(listOf("42"), testChat))
    }

    @Test
    fun unlinkRemovesEntry() {
        val chat = object : Chat(UniversalProtocol, "C") { override fun toKey() = "TestChat(C)" }
        chatMap[42] = chat
        linkedChats[testChat] = mutableSetOf(chat)
        val result = unlink(listOf("42"), testChat)
        assertEquals("Removed ${chat.name} from this chat's links.", result)
        // Map entry is cleaned up when the set becomes empty
        assertFalse(testChat in linkedChats, "linkedChats entry should be removed when set is empty")
    }

    @Test
    fun unlinkLeavesOtherLinksIntact() {
        val chat1 = object : Chat(UniversalProtocol, "C1") { override fun toKey() = "TestChat(C1)" }
        val chat2 = object : Chat(UniversalProtocol, "C2") { override fun toKey() = "TestChat(C2)" }
        chatMap[1] = chat1
        chatMap[2] = chat2
        linkedChats[testChat] = mutableSetOf(chat1, chat2)
        unlink(listOf("1"), testChat)
        assertTrue(chat2 in linkedChats[testChat]!!, "chat2 should still be linked after unlinking chat1")
    }

    // ─── links ────────────────────────────────────────────────────────────────

    @Test
    fun linksReportsNoneWhenEmpty() {
        assertEquals("No chats are linked to this one.", links(testChat))
    }

    @Test
    fun linksListsLinkedChatsWithIds() {
        val chat = object : Chat(UniversalProtocol, "Other") { override fun toKey() = "TestChat(Other)" }
        chatMap[42] = chat
        reverseChatMap[chat] = 42
        linkedChats[testChat] = mutableSetOf(chat)
        val result = links(testChat)
        assertTrue(result.startsWith("Linked chats:"), "Unexpected prefix: $result")
        assertTrue(result.contains("42"), "Expected chat ID 42 in result")
    }

    // ─── setCommandDelimiter / setDelimiter ───────────────────────────────────

    @Test
    fun setDelimiterRequiresArgument() {
        assertEquals("You need to pass the new delimiter!", setDelimiter(listOf(), testChat))
    }

    @Test
    fun setDelimiterAcceptsValidDelimiter() {
        assertEquals("Command delimiter set to \"/\".", setDelimiter(listOf("/"), testChat))
        assertEquals("/", commandDelimiters[testChat])
    }

    @Test
    fun setCommandDelimiterRejectsWhitespace() {
        assertFalse(setCommandDelimiter(testChat, " "), "Space should be rejected")
        assertFalse(setCommandDelimiter(testChat, "\t"), "Tab should be rejected")
        assertFalse(setCommandDelimiter(testChat, "\n"), "Newline should be rejected")
    }

    @Test
    fun setCommandDelimiterRejectsDoubleQuote() {
        assertFalse(setCommandDelimiter(testChat, "\""), "Double-quote should be rejected")
    }

    @Test
    fun setCommandDelimiterAcceptsVariousDelimiters() {
        for (delimiter in listOf("!", "/", "@", "#", "$", "|", "~")) {
            assertTrue(setCommandDelimiter(testChat, delimiter), "'$delimiter' should be accepted")
            assertEquals(delimiter, commandDelimiters[testChat])
        }
    }

    // ─── commands() ───────────────────────────────────────────────────────────

    @Test
    fun commandsReturnsNoneWhenNothingRegistered() {
        assertEquals("No commands found.", commands(testProtocolChat))
    }

    @Test
    fun commandsListsProtocolSpecificCommands() {
        commands[TestProtocol] = mutableMapOf(
            "alpha" to Command.of(TestProtocol, "alpha", listOf(), { -> null }, "help", "syntax"),
            "beta"  to Command.of(TestProtocol, "beta",  listOf(), { -> null }, "help", "syntax"),
        )
        val result = commands(testProtocolChat)
        assertEquals("alpha, beta", result, "Commands should be sorted alphabetically")
    }

    @Test
    fun commandsIncludesUniversalProtocolCommands() {
        commands[TestProtocol] = mutableMapOf(
            "mycmd" to Command.of(TestProtocol, "mycmd", listOf(), { -> null }, "help", "syntax")
        )
        commands[UniversalProtocol] = mutableMapOf(
            "ping" to Command.of(UniversalProtocol, "ping", listOf(), ::ping, "Pong!", "ping")
        )
        val result = commands(testProtocolChat)
        assertTrue(result.contains("mycmd"), "Protocol-specific command should appear")
        assertTrue(result.contains("ping"), "Universal command should appear")
    }

    // ─── aliases() ────────────────────────────────────────────────────────────

    @Test
    fun aliasesReturnsNoneWhenNothingRegistered() {
        assertEquals("No aliases found.", aliases(testChat))
    }

    @Test
    fun aliasesListsAliasesForChat() {
        val cmd = Command.of(UniversalProtocol, "echo", listOf(), ::echo, "test", "test")
        aliases[testChat] = mutableMapOf(
            "greet" to Alias(testChat, "greet", cmd, listOf("hello"))
        )
        val result = aliases(testChat)
        assertTrue(result.startsWith("Aliases:"), "Unexpected prefix: $result")
        assertTrue(result.contains("greet"), "Expected alias name in result")
    }

    @Test
    fun aliasesListsMultipleAliases() {
        val cmd = Command.of(UniversalProtocol, "echo", listOf(), ::echo, "test", "test")
        aliases[testChat] = mutableMapOf(
            "a1" to Alias(testChat, "a1", cmd, listOf()),
            "a2" to Alias(testChat, "a2", cmd, listOf()),
        )
        val result = aliases(testChat)
        assertTrue(result.contains("a1"))
        assertTrue(result.contains("a2"))
    }

    // ─── me ───────────────────────────────────────────────────────────────────

    @Test
    fun meFormatsMessageWithEmptyUsername() {
        // UniversalProtocol.getUserName returns "" — expected: "* <args>."
        val result = me(listOf("is", "online"), testChat, testUser)
        assertEquals("* is online.", result)
    }

    @Test
    fun meJoinsAllArguments() {
        val result = me(listOf("eats", "a", "sandwich"), testChat, testUser)
        assertEquals("* eats a sandwich.", result)
    }

    // ─── help ─────────────────────────────────────────────────────────────────

    @Test
    fun helpReturnsErrorForUnknownCommand() {
        // No commands registered, so any name lookup should fail
        val result = help(listOf("doesnotexist"), testChat)
        assertEquals("There is no command with the name \"doesnotexist\".", result)
    }

    @Test
    fun helpPageContainsRegisteredCommand() {
        registerCommand(Command.of(UniversalProtocol, "ping", listOf(), ::ping, "Replies with Pong!", "ping"))
        val result = help(listOf(), testChat)
        assertTrue(result.startsWith("Help page 1/1:"), "Expected page header, got: $result")
        assertTrue(result.contains("ping"), "Expected 'ping' in help output")
    }

    @Test
    fun helpLookupByNameReturnsCommandDetails() {
        registerCommand(Command.of(UniversalProtocol, "echo", listOf(), ::echo, "Echoes input.", "echo [msg...]"))
        val result = help(listOf("echo"), testChat)
        assertTrue(result.contains("Echoes input."), "Expected helpText in output")
        assertTrue(result.contains("echo [msg...]"), "Expected syntaxText in output")
    }

    @Test
    fun helpLookupIsCaseInsensitive() {
        registerCommand(Command.of(UniversalProtocol, "ping", listOf(), ::ping, "Replies with Pong!", "ping"))
        val result = help(listOf("PING"), testChat)
        assertFalse(result.startsWith("There is no command"), "Mixed-case lookup should find the command")
    }
}
