package ca.michaelmaguire.dialett.search

import ca.michaelmaguire.dialett.data.AppData
import ca.michaelmaguire.dialett.data.Example
import ca.michaelmaguire.dialett.data.Heading
import ca.michaelmaguire.dialett.data.Paragraphs
import ca.michaelmaguire.dialett.data.Table
import ca.michaelmaguire.dialett.data.VerbTable
import ca.michaelmaguire.dialett.nav.Destination

/** What a search result is, in the order the results list shows them. */
enum class Kind(val title: String) {
    WORD("Words"),
    VERB("Verbs"),
    VERB_FORM("Verb forms"),
    EXPRESSION("Expressions"),
    GRAMMAR("Grammar"),
}

/**
 * One result. title: what was found (the word, verb form or expression); subtitle: its translation;
 * detail: where (region, tense and person, grammar chapter); snippet: for grammar, the text around the match.
 */
data class Hit(
    val kind: Kind,
    val title: String,
    val subtitle: String,
    val detail: String,
    val destination: Destination,
    val score: Int,
    val snippet: String = "",
)

/** A searchable text, folded once. weight: how much a match here counts (a headword more than a note). */
private class Field(val original: String, val weight: Int) {
    val text = fold(original)
    val words = text.split(' ').filter { it.isNotEmpty() }
    val collapsed = collapse(text)
}

private class Doc(
    val kind: Kind,
    val fields: List<Field>,
    val title: String,
    val subtitle: String,
    val detail: String,
    val destination: Destination,
    /** Grammar paragraphs: show where the match is. */
    val snippets: Boolean = false,
)

/** How well one query word matches a field. */
private const val EXACT = 10
private const val PREFIX = 6
private const val INSIDE = 2
private const val SPELLING = 1

/**
 * Search over the whole book: dictionary words, verbs and each of their forms, expressions and the grammar.
 * Every word of the query must match somewhere in a result (in any field); a whole word scores more than the
 * start of one, which scores more than a match inside one. Typed accents don't matter, nor do doubled letters.
 */
class SearchIndex(data: AppData) {
    private val docs: List<Doc> = buildList {
        data.dictionary.forEachIndexed { i, e ->
            val s = e.senses
            add(Doc(
                Kind.WORD,
                listOf(Field(s.joinToString(" ") { it.dialect }, 3),
                    Field(s.joinToString(" ") { it.italian }, 2),
                    Field(s.joinToString(" ") { it.english }, 2),
                    Field(e.notes.joinToString(" "), 1)),
                title = e.head,
                subtitle = s.map { listOf(it.italian, it.english).filter(String::isNotEmpty).joinToString(" · ") }
                    .distinct().joinToString("; "),
                detail = s.map { it.region }.filter { it.isNotEmpty() && it != "Rögn" }.distinct().joinToString(", "),
                destination = Destination.Word(i),
            ))
        }
        for (v in data.verbs) {
            add(Doc(
                Kind.VERB,
                listOf(Field(v.dialect, 3), Field(v.italian, 2), Field(v.english, 2)),
                title = v.dialect, subtitle = "${v.italian} · ${v.english}", detail = v.verbClass,
                destination = Destination.VerbPage(v.id),
            ))
            // a verb with a particle (na föra) is found as a verb, and leads to its base verb's page
            for (a in v.also) {
                add(Doc(
                    Kind.VERB, listOf(Field(a.dialect, 3), Field(a.italian, 2), Field(a.english, 2)),
                    title = a.dialect, subtitle = "${a.italian} · ${a.english}", detail = "see ${v.dialect}",
                    destination = Destination.VerbPage(v.id),
                ))
            }
            // each distinct form once, with every tense and person it is
            val places = LinkedHashMap<String, MutableList<String>>()
            for (t in v.tenses) for (f in t.forms) {
                places.getOrPut(f.text) { mutableListOf() } += listOf(t.name, f.label).filter(String::isNotEmpty).joinToString(" · ")
            }
            for (f in v.gerund) places.getOrPut(f.text) { mutableListOf() } += "gerundio"
            for ((form, where) in places) {
                add(Doc(
                    Kind.VERB_FORM, listOf(Field(form, 2)),
                    title = form, subtitle = "${v.dialect} · ${v.italian} · ${v.english}",
                    detail = where.distinct().joinToString("; "),
                    destination = Destination.VerbPage(v.id, highlight = form),
                ))
            }
        }
        for (t in data.expressions) t.sections.forEachIndexed { si, s ->
            s.entries.forEachIndexed { ei, x ->
                add(Doc(
                    Kind.EXPRESSION,
                    listOf(Field(x.dialect, 2), Field(x.italian, 2), Field(x.english, 2)),
                    title = x.dialect, subtitle = listOf(x.italian, x.english).filter(String::isNotEmpty).joinToString(" · "),
                    detail = t.english, destination = Destination.ExpressionItem(t.id, si, ei),
                ))
            }
        }
        for (c in data.grammar) {
            val chapter = listOf(c.number, c.english).filter(String::isNotEmpty).joinToString(". ")
            add(Doc(
                Kind.GRAMMAR, listOf(Field(c.italian, 3), Field(c.english, 3)),
                title = c.english, subtitle = c.italian, detail = "Chapter ${c.number}".takeIf { c.number.isNotEmpty() } ?: "Introduction",
                destination = Destination.GrammarChapter(c.id),
            ))
            c.blocks.forEachIndexed { bi, b ->
                val at = Destination.GrammarChapter(c.id, bi)
                when (b) {
                    is Heading -> add(Doc(Kind.GRAMMAR, listOf(Field(b.italian, 2), Field(b.english, 2)),
                        title = b.english, subtitle = b.italian, detail = chapter, destination = at))
                    is Paragraphs -> b.pairs.forEach { p ->
                        add(Doc(Kind.GRAMMAR, listOf(Field(p.english, 1), Field(p.italian, 1)),
                            title = chapter, subtitle = "", detail = "", destination = at, snippets = true))
                    }
                    is Example -> add(Doc(Kind.GRAMMAR,
                        listOf(Field(b.dialect, 2), Field(b.italian, 1), Field(b.english, 1)),
                        title = b.dialect, subtitle = "${b.italian} · ${b.english}", detail = "$chapter, example ${b.number}",
                        destination = at))
                    is Table -> add(Doc(Kind.GRAMMAR,
                        listOf(Field((b.header + b.rows.flatten()).joinToString(" "), 1)),
                        title = chapter, subtitle = "", detail = "table", destination = at, snippets = true))
                    is VerbTable -> {}  // its forms are found as verb forms, on the verb's page
                }
            }
        }
    }

    /** Results, best first; kinds limits them (e.g. only words, for the dictionary's filter). */
    fun search(query: String, kinds: Set<Kind> = Kind.entries.toSet()): List<Hit> {
        val q = fold(query)
        val tokens = q.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        for (d in docs) {
            if (d.kind !in kinds) continue
            val score = score(d, q, tokens) ?: continue
            hits += Hit(d.kind, d.title, d.subtitle, d.detail, d.destination, score,
                if (d.snippets) snippet(d, tokens.first()) else "")
        }
        hits.sortWith(compareByDescending<Hit> { it.score }.thenBy { it.kind }.thenBy { it.title.length })
        return hits
    }

    private fun score(d: Doc, q: String, tokens: List<String>): Int? {
        var total = 0
        for (t in tokens) {
            val tc = collapse(t)
            var best = 0
            for (f in d.fields) {
                val level = when {
                    f.text.isEmpty() -> 0
                    t in f.words -> EXACT
                    f.words.any { it.startsWith(t) } -> PREFIX
                    t.length >= 3 && t in f.text -> INSIDE
                    tc.length >= 3 && tc in f.collapsed -> SPELLING
                    else -> 0
                }
                best = maxOf(best, level * f.weight)
            }
            if (best == 0) return null
            total += best
        }
        // a conjugated form only when the query matches a word of it from the start: otherwise every verb
        // with a common ending would show
        if (d.kind == Kind.VERB_FORM && d.fields[0].words.none { w -> tokens.any { w.startsWith(it) } }) return null
        // the whole query as the whole field, or its start: the word itself before words that contain it
        for (f in d.fields) {
            if (f.weight < 2) continue
            when {
                f.text == q -> total += 40 * f.weight
                f.text.startsWith(q) -> total += 12 * f.weight
            }
        }
        return total
    }

    private fun snippet(d: Doc, token: String): String {
        for (f in d.fields) {
            val (text, map) = foldWithMap(f.original)
            val at = text.indexOf(token)
            if (at < 0) continue
            val start = map[at]
            val from = (start - 50).coerceAtLeast(0).let { s -> f.original.lastIndexOf(' ', s).let { if (it < 0 || s == 0) s else it + 1 } }
            val to = (start + 110).coerceAtMost(f.original.length).let { e -> f.original.indexOf(' ', e).let { if (it < 0) f.original.length else it } }
            return (if (from > 0) "…" else "") + f.original.substring(from, to) + (if (to < f.original.length) "…" else "")
        }
        return d.fields.first().original.take(160)
    }
}
