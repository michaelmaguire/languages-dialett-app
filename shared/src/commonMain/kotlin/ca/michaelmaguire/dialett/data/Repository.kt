package ca.michaelmaguire.dialett.data

import ca.michaelmaguire.dialett.resources.Res
import ca.michaelmaguire.dialett.nav.Destination
import ca.michaelmaguire.dialett.search.SearchIndex
import ca.michaelmaguire.dialett.search.fold
import kotlinx.serialization.json.Json

/** Everything the screens need: the data, the search index over it, and look-ups by id. */
class Library(val data: AppData) {
    val index = SearchIndex(data)
    private val verbsById = data.verbs.associateBy { it.id }
    private val chaptersById = data.grammar.associateBy { it.id }
    private val topicsById = data.expressions.associateBy { it.id }

    fun verb(id: String): Verb? = verbsById[id]
    fun chapter(id: String): Chapter? = chaptersById[id]
    fun topic(id: String): Topic? = topicsById[id]

    /** Folded one-word dialect headwords (each spelling of an entry) → the entries. */
    private val headwords: Map<String, List<Int>> = buildMap<String, MutableList<Int>> {
        data.dictionary.forEachIndexed { i, e ->
            for (s in e.senses) for (form in s.dialect.replace(Regex("\\([^)]*\\)"), " ").split(Regex("[/;]|\\s+o\\s+"))) {
                val f = fold(form)
                if (f.isNotEmpty() && ' ' !in f) getOrPut(f) { mutableListOf() }.let { if (i !in it) it += i }
            }
        }
    }

    /** Folded words of verb forms (bütavum, ciapaa) → the one verb they belong to. Words that are forms of
     * several verbs (auxiliaries like eva, clitics) are left out: they would point to the wrong one. */
    private val verbWords: Map<String, String> = run {
        val owners = HashMap<String, MutableSet<String>>()
        for (v in data.verbs) {
            val texts = v.tenses.flatMap { t -> t.forms.map { it.text } } + v.gerund.map { it.text } + v.dialect + v.participle
            for (t in texts) for (w in fold(t).split(' ', '/')) if (w.length >= 3) owners.getOrPut(w) { mutableSetOf() } += v.id
        }
        owners.filterValues { it.size == 1 }.mapValues { it.value.first() }
    }

    /** The dictionary words and verbs in a dialect text, in order, each once: (word as found, its label, where it leads). */
    fun wordsIn(text: String): List<Triple<String, String, Destination>> {
        val out = ArrayList<Triple<String, String, Destination>>()
        val seen = HashSet<Destination>()
        for (w in fold(text).split(' ')) {
            headwords[w]?.forEach { i ->
                val d = Destination.Word(i)
                val s = data.dictionary[i].senses.first()
                if (seen.add(d)) out += Triple(data.dictionary[i].head, listOf(s.italian, s.english).filter(String::isNotEmpty).joinToString(" · "), d)
            }
            if (headwords[w] == null) verbWords[w]?.let { id ->
                val v = verbsById.getValue(id)
                val d = Destination.VerbPage(id)
                if (seen.add(d)) out += Triple(v.dialect, "${v.italian} · ${v.english}", d)
            }
        }
        return out
    }
}

private val json = Json { ignoreUnknownKeys = true }

/** Reads the data file; throws if it's missing, unreadable, or from a newer schema. */
fun parseData(text: String): AppData {
    val data = json.decodeFromString(AppData.serializer(), text)
    require(data.version <= DATA_VERSION) {
        "The data file is version ${data.version}; this app reads version $DATA_VERSION. Update the app."
    }
    return data
}

suspend fun loadLibrary(): Library = Library(parseData(Res.readBytes("files/dialett.json").decodeToString()))
