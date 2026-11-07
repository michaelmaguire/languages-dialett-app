package ca.michaelmaguire.dialett.nav

/** A screen of the app. Search results lead to one of these. */
sealed interface Destination {
    data object Search : Destination
    data object Dictionary : Destination
    data object Verbs : Destination
    data object Grammar : Destination
    data object Expressions : Destination

    /** entry: position in AppData.dictionary. */
    data class Word(val entry: Int) : Destination

    /** highlight: a form to mark in the tables (from a search for a conjugated form), or "". */
    data class VerbPage(val id: String, val highlight: String = "") : Destination

    /** block: the block to scroll to, or 0. */
    data class GrammarChapter(val id: String, val block: Int = 0) : Destination

    data class ExpressionTopic(val id: String) : Destination

    data class ExpressionItem(val topic: String, val section: Int, val entry: Int) : Destination
}
