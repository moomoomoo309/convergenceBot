import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Suppress("detekt.EmptyFunctionBlock")
private fun makeProtocol(name: String): Protocol = object : Protocol(name) {
    override fun init() {}
    override fun configLoaded() {}
    override fun aliasCreated(alias: Alias) {}
    override fun sendMessage(chat: Chat, message: OutgoingMessage) = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getUserName(chat: Chat, user: User) = ""
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(): List<User> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat) = ""
    override fun commandScopeFromKey(key: String): CommandScope? = null
    override fun userFromKey(key: String): User? = null
}

class ConfigurationTest {

    @Before
    fun setup() {
        protocols.clear()
    }

    @After
    fun teardown() {
        protocols.clear()
    }

    // ─── scopeStrToProtocol ─────────────────────────────────────────────────

    @Test
    fun scopeStrToProtocolFindsProtocolByName() {
        protocols.add(TestProtocol)
        val result = scopeStrToProtocol("TestProtocol(someId)")
        assertEquals(TestProtocol, result)
    }

    @Test
    fun scopeStrToProtocolFindsUniversalProtocol() {
        protocols.add(UniversalProtocol)
        val result = scopeStrToProtocol("UniversalChat")
        assertEquals(UniversalProtocol, result)
    }

    @Test
    fun scopeStrToProtocolReturnsNullForUnknownProtocol() {
        val result = scopeStrToProtocol("NonexistentProtocol(id)")
        assertNull(result)
    }

    @Test
    fun scopeStrToProtocolReturnsNullForEmptyString() {
        val result = scopeStrToProtocol("")
        assertNull(result)
    }

    @Test
    fun scopeStrToProtocolMatchesLongestProtocolNameFirst() {
        val shortProtocol = makeProtocol("Ab")
        val longProtocol = makeProtocol("Abcde")
        protocols.add(shortProtocol)
        protocols.add(longProtocol)
        // "Abcde(foo)" — both "Ab" and "Abcde" match the prefix, but "Abcde" is longer
        val result = scopeStrToProtocol("Abcde(foo)")
        assertEquals(longProtocol, result)
    }

    @Test
    fun scopeStrToProtocolWithNoParentheses() {
        protocols.add(TestProtocol)
        val result = scopeStrToProtocol("TestProtocol")
        // substringBefore("(") returns "TestProtocol" when there's no "("
        assertEquals(TestProtocol, result)
    }

    // ─── getUserName ────────────────────────────────────────────────────────

    @Test
    fun getUserNameDelegatesToProtocol() {
        val result = getUserName(testChat, testUser)
        assertEquals("", result)
    }

    @Test
    fun getUserNameWithHasNicknamesReturnsNickname() {
        @Suppress("detekt.EmptyFunctionBlock")
        val nickProtocol = object : Protocol("NickTest"), HasNicknames {
            override fun init() {}
            override fun configLoaded() {}
            override fun aliasCreated(alias: Alias) {}
            override fun sendMessage(chat: Chat, message: OutgoingMessage) = false
            override fun getBot(chat: Chat): User = UniversalUser
            override fun getUserName(chat: Chat, user: User) = "username"
            override fun getChats(): List<Chat> = emptyList()
            override fun getUsers(): List<User> = emptyList()
            override fun getUsers(chat: Chat): List<User> = emptyList()
            override fun getChatName(chat: Chat) = ""
            override fun commandScopeFromKey(key: String): CommandScope? = null
            override fun userFromKey(key: String): User? = null
            override fun getUserNickname(chat: Chat, user: User) = "nickname"
            override fun getBotNickname(chat: Chat): String? = null
            override fun setUserNickname(chat: Chat, user: User, newName: String) = newName
            override fun setBotNickname(chat: Chat, newName: String) = newName
        }
        val chat = object : Chat(nickProtocol, "NickChat") {
            override fun toKey() = "NickChat(NickChat)"
        }
        val user = object : User(nickProtocol) {
            override fun toKey() = "NickTestUser(1)"
        }
        assertEquals("nickname", getUserName(chat, user))
    }

    @Test
    fun getUserNameFallsBackToUsernameWhenNicknameIsNull() {
        @Suppress("detekt.EmptyFunctionBlock")
        val nickProtocol = object : Protocol("NickTest2"), HasNicknames {
            override fun init() {}
            override fun configLoaded() {}
            override fun aliasCreated(alias: Alias) {}
            override fun sendMessage(chat: Chat, message: OutgoingMessage) = false
            override fun getBot(chat: Chat): User = UniversalUser
            override fun getUserName(chat: Chat, user: User) = "fallback"
            override fun getChats(): List<Chat> = emptyList()
            override fun getUsers(): List<User> = emptyList()
            override fun getUsers(chat: Chat): List<User> = emptyList()
            override fun getChatName(chat: Chat) = ""
            override fun commandScopeFromKey(key: String): CommandScope? = null
            override fun userFromKey(key: String): User? = null
            override fun getUserNickname(chat: Chat, user: User): String? = null
            override fun getBotNickname(chat: Chat): String? = null
            override fun setUserNickname(chat: Chat, user: User, newName: String) = newName
            override fun setBotNickname(chat: Chat, newName: String) = newName
        }
        val chat = object : Chat(nickProtocol, "NickChat2") {
            override fun toKey() = "NickChat2(NickChat2)"
        }
        val user = object : User(nickProtocol) {
            override fun toKey() = "NickTestUser2(1)"
        }
        assertEquals("fallback", getUserName(chat, user))
    }

    // ─── Protocol equality ──────────────────────────────────────────────────

    @Test
    fun protocolsAreEqualWhenNameMatches() {
        val p1 = makeProtocol("Same")
        val p2 = makeProtocol("Same")
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun protocolsAreNotEqualWhenNameDiffers() {
        val p1 = makeProtocol("A")
        val p2 = makeProtocol("B")
        assert(p1 != p2)
    }

    @Test
    fun protocolToStringReturnsSimpleClassName() {
        assertEquals("UniversalProtocol", UniversalProtocol.toString())
    }
}
