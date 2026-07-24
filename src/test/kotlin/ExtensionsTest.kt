
import convergence.substringBetween
import convergence.titleCase
import org.junit.Test
import kotlin.test.assertEquals

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
}
