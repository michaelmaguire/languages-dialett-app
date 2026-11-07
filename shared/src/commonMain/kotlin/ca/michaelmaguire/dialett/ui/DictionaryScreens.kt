package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Entry
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.nav.Destination
import ca.michaelmaguire.dialett.search.Kind
import ca.michaelmaguire.dialett.search.fold
import kotlinx.coroutines.launch

private fun subtitle(e: Entry) =
    e.senses.map { listOf(it.italian, it.english).filter(String::isNotEmpty).joinToString(" · ") }.distinct().take(3).joinToString("; ")

private fun regions(e: Entry) = e.senses.map { it.region }.filter { it.isNotEmpty() && it != "Rögn" }.distinct()

/** The dictionary, Dialett → Italiano · English, A to Z, with a filter. */
@Composable
fun DictionaryScreen(lib: Library, nav: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val entries = lib.data.dictionary
    // the entries under their first letter, in the dictionary's order
    val letters = remember(lib) {
        entries.indices.groupBy { fold(entries[it].head).firstOrNull()?.uppercaseChar()?.takeIf(Char::isLetter) ?: '#' }
    }
    Column {
        FilterField(query, { query = it }, "Filter ${entries.size} words")
        if (query.isBlank()) {
            // item index of each letter's header: headers and rows in one list
            val starts = remember(letters) {
                var n = 0
                letters.mapValues { (_, v) -> n.also { n += v.size + 1 } }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                items(letters.keys.toList()) { c ->
                    Text(
                        c.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { scope.launch { list.scrollToItem(starts.getValue(c)) } }.padding(10.dp),
                    )
                }
            }
            LazyColumn(state = list) {
                for ((c, ids) in letters) {
                    item(key = "h$c") { SectionHeader(c.toString()) }
                    items(ids, key = { it }) { i -> WordRow(entries[i]) { nav.open(Destination.Word(i)) } }
                }
            }
        } else {
            val hits = remember(query) { lib.index.search(query, setOf(Kind.WORD)) }
            if (hits.isEmpty()) Text("No word matches “$query”.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
            LazyColumn {
                items(hits, key = { it.destination.toString() }) { h ->
                    val i = (h.destination as Destination.Word).entry
                    WordRow(entries[i]) { nav.open(h.destination) }
                }
            }
        }
    }
}

@Composable
private fun WordRow(e: Entry, onClick: () -> Unit) {
    val label = e.senses.first().label
    ListRow(
        e.head + (if (label.isNotEmpty()) "  *$label*" else "") + (if (e.flag) " $CHECK" else ""),
        subtitle(e), onClick, tags = regions(e), rich = true,
    )
}

/** One headword: each meaning with its translations, region and notes; its verb table; expressions using it. */
@Composable
fun WordScreen(lib: Library, nav: Navigator, index: Int) {
    val e = lib.data.dictionary[index]
    val using = remember(index) {
        val forms = e.senses.flatMap { s -> s.dialect.split('/', ';').map { fold(it) } }.filter { it.isNotEmpty() && ' ' !in it }.toSet()
        lib.data.expressions.flatMap { t ->
            t.sections.flatMapIndexed { si, s ->
                s.entries.mapIndexedNotNull { ei, x ->
                    if (fold(x.dialect).split(' ').any { it in forms }) Triple(x, t, Destination.ExpressionItem(t.id, si, ei)) else null
                }
            }
        }.take(40)
    }
    LazyColumn {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text(e.head + if (e.flag) " $CHECK" else "", style = MaterialTheme.typography.headlineMedium)
            }
        }
        items(e.senses.size) { i ->
            val s = e.senses[i]
            InfoCard {
                if (i == 0 || s.newWord || s.dialect != e.senses[i - 1].dialect) {
                    RichText(s.dialect + if (s.label.isNotEmpty()) "  *${s.label}*" else "", style = MaterialTheme.typography.titleMedium)
                }
                Labelled("IT", s.italian)
                Labelled("EN", s.english)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.region.isNotEmpty()) Tag(s.region)
                    if (s.check) Tag("$CHECK to check")
                }
                if (s.note.isNotEmpty()) RichText(s.note, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.verb.isNotEmpty()) {
                    AssistChip(onClick = { nav.open(Destination.VerbPage(s.verb)) }, label = { Text("Conjugation →") },
                        modifier = Modifier.padding(top = 4.dp))
                }
                if (s.source.isNotEmpty()) Text(sourceTitle(s.source), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
            }
        }
        val extra = e.notes.filter { n -> e.senses.none { it.note == n } }
        if (extra.isNotEmpty()) item { InfoCard { extra.forEach { RichText(it) } } }
        if (using.isNotEmpty()) {
            item { SectionHeader("In expressions (${using.size})", Modifier.padding(top = 8.dp)) }
            items(using) { (x, t, d) -> ListRow(x.dialect, listOf(x.italian, x.english).filter(String::isNotEmpty).joinToString(" · "), { nav.open(d) }, detail = t.english) }
        }
    }
}
