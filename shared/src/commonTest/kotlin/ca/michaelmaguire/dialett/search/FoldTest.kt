package ca.michaelmaguire.dialett.search

import kotlin.test.Test
import kotlin.test.assertEquals

class FoldTest {
    @Test
    fun accentsApostrophesAndMarkersGo() {
        assertEquals("buta", fold("bütà"))
        assertEquals("le nai", fold("l’è nai"))
        assertEquals("caar caara", fold("caar/caara †"))
        assertEquals("in boca al luf", fold("«In boca al lüf!»"))
    }

    @Test
    fun repeatedLettersCollapse() {
        assertEquals("pe", collapse(fold("pée")))
        assertEquals("buta", collapse(fold("büttà")))
    }

    @Test
    fun mapPointsBackToTheOriginal() {
        val (text, map) = foldWithMap("Ul *dialett* da Rögn")
        val at = text.indexOf("rogn")
        assertEquals("Rögn", "Ul *dialett* da Rögn".substring(map[at], map[at] + 4))
    }
}
