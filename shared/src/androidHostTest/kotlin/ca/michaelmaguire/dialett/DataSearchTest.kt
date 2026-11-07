package ca.michaelmaguire.dialett

import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.data.parseData
import ca.michaelmaguire.dialett.nav.Destination
import ca.michaelmaguire.dialett.search.Kind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The real data file (from languages-dialett's step 6), read and searched as the app does. */
class DataSearchTest {
    private val lib = Library(parseData(File("src/commonMain/composeResources/files/dialett.json").readText()))

    @Test
    fun everythingIsThere() {
        assertTrue(lib.data.dictionary.size > 1400, "words: ${lib.data.dictionary.size}")
        assertTrue(lib.data.verbs.size >= 140)
        assertEquals(13, lib.data.grammar.size)
    }

    @Test
    fun aVerbWithoutItsAccentsComesFirst() {
        val top = lib.index.search("buta").first()
        assertEquals(Kind.VERB, top.kind)
        assertEquals("bütà", top.title)
    }

    @Test
    fun aConjugatedFormFindsItsVerb() {
        val hit = lib.index.search("bütavum").first { it.kind == Kind.VERB_FORM }
        assertEquals("buta-gettare", (hit.destination as Destination.VerbPage).id)
        assertTrue("imperfetto" in hit.detail, hit.detail)
    }

    @Test
    fun italianAndEnglishFindTheWord() {
        assertEquals("cà", lib.index.search("casa", setOf(Kind.WORD)).first().title)
        assertTrue(lib.index.search("house", setOf(Kind.WORD)).take(3).any { it.title == "cà" })
    }

    @Test
    fun someWordsOfASayingFindIt() {
        assertTrue(lib.index.search("boca luf").any { it.kind == Kind.EXPRESSION && "lüf" in it.title })
    }

    @Test
    fun theGrammarIsSearched() {
        assertTrue(lib.index.search("congiuntivo").any { it.kind == Kind.GRAMMAR })
    }

    @Test
    fun anExpressionListsItsWords() {
        val words = lib.wordsIn("In boca al lüf").map { it.first }
        assertTrue(words.any { "lüf" in it }, words.toString())
    }
}
