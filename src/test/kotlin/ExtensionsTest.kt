
import convergence.substringBetween
import convergence.titleCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtensionsTest {

    // ─── substringBetween ───────────────────────────────────────────────────

    @Test
    fun substringBetweenExtractsMiddle() {
        assertEquals("bar", "foobarbaz".substringBetween("foo", "baz"))
    }

    @Test
    fun substringBetweenReturnsEmptyWhenStartNotFound() {
        assertEquals("", "hello".substringBetween("xxx", "yyy"))
    }

    @Test
    fun substringBetweenReturnsEmptyWhenEndNotFound() {
        assertEquals("", "hello".substringBetween("hel", "yyy"))
    }

    @Test
    fun substringBetweenReturnsEmptyWhenBothNotFound() {
        assertEquals("", "hello".substringBetween("xxx", "yyy"))
    }

    @Test
    fun substringBetweenHandlesAdjacentDelimiters() {
        assertEquals("", "abcabc".substringBetween("abc", "abc"))
    }

    // ─── titleCase ──────────────────────────────────────────────────────────

    @Test
    fun titleCaseCapitalizesFirstLetter() {
        assertEquals("Hello", "hello".titleCase())
    }

    @Test
    fun titleCaseLowercasesRest() {
        assertEquals("Hello", "HELLO".titleCase())
    }

    @Test
    fun titleCaseMixedCase() {
        assertEquals("Hello world", "hELLO WORLD".titleCase())
    }

    @Test
    fun titleCaseSingleCharacter() {
        assertEquals("A", "a".titleCase())
    }

    @Test
    fun titleCaseAlreadyCorrect() {
        assertEquals("Hello", "Hello".titleCase())
    }

    // ─── clearThen ──────────────────────────────────────────────────────────

    @Test
    fun clearThenClearsMapAndReturnsIt() {
        val map = mutableMapOf(1 to "a", 2 to "b")
        val result = map.clearThen()
        assertTrue(result.isEmpty())
        assertEquals(map, result)
    }

    @Test
    fun clearThenClearsListAndReturnsIt() {
        val list = mutableListOf("a", "b")
        val result = list.clearThen()
        assertTrue(result.isEmpty())
        assertEquals(list, result)
    }
}
