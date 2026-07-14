import convergence.*
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

    // ─── mapEntries ─────────────────────────────────────────────────────────

    @Test
    fun mapEntriesTransformsKeysAndValues() {
        val map = mapOf(1 to "a", 2 to "b")
        val result = map.mapEntries { (k, v) -> "${k}x" to v.uppercase() }
        assertEquals(mapOf("1x" to "A", "2x" to "B"), result)
    }

    @Test
    fun mapEntriesOnEmptyMap() {
        val result = emptyMap<Int, Int>().mapEntries { (k, v) -> v to k }
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapEntriesCanChangeValueTypes() {
        val map = mapOf("a" to 1, "b" to 2)
        val result = map.mapEntries { (k, v) -> k to v * 10 }
        assertEquals(mapOf("a" to 10, "b" to 20), result)
    }

    // ─── mutable (Map) ──────────────────────────────────────────────────────

    @Test
    fun mutableMapReturnsSameInstanceWhenAlreadyMutable() {
        val map = mutableMapOf(1 to "a")
        val result = map.mutable()
        assertEquals(map, result)
    }

    @Test
    fun mutableMapOnEmptyMapReturnsMutable() {
        val map: Map<Int, String> = emptyMap()
        val result = map.mutable()
        result[2] = "b"
        assertTrue(2 in result)
    }

    // ─── mutable (List) ─────────────────────────────────────────────────────

    @Test
    fun mutableListReturnsSameInstanceWhenAlreadyMutable() {
        val list = mutableListOf("a")
        val result = list.mutable()
        assertEquals(list, result)
    }

    @Test
    fun mutableListOnEmptyListReturnsMutable() {
        val list: List<String> = emptyList()
        val result = list.mutable()
        result.add("b")
        assertTrue(result.contains("b"))
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
