package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Expression
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.nav.Destination
import ca.michaelmaguire.dialett.search.Kind
import ca.michaelmaguire.dialett.search.fold

private fun translation(x: Expression) = listOf(x.italian, x.english).filter(String::isNotEmpty).joinToString(" · ")

/** The topics, with a filter over every expression. */
@Composable
fun TopicListScreen(lib: Library, nav: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    Column {
        FilterField(query, { query = it }, "Filter all expressions")
        if (query.isBlank()) LazyColumn {
            items(lib.data.expressions, key = { it.id }) { t ->
                ListRow(t.dialect, "${t.italian} · ${t.english}", { nav.open(Destination.ExpressionTopic(t.id)) },
                    tags = listOf(t.sections.sumOf { it.entries.size }.toString()))
            }
        } else {
            val hits = remember(query) { lib.index.search(query, setOf(Kind.EXPRESSION)) }
            LazyColumn {
                items(hits, key = { it.destination.toString() }) { h ->
                    ListRow(h.title, h.subtitle, { nav.open(h.destination) }, detail = h.detail)
                }
            }
        }
    }
}

/** One topic: sayings and idioms, then phrases. */
@Composable
fun TopicScreen(lib: Library, nav: Navigator, id: String) {
    val t = lib.topic(id) ?: return
    var query by rememberSaveable { mutableStateOf("") }
    val q = fold(query)
    Column {
        FilterField(query, { query = it }, "Filter ${t.sections.sumOf { it.entries.size }} expressions")
        LazyColumn {
            t.sections.forEachIndexed { si, s ->
                val shown = s.entries.withIndex().filter { (_, x) ->
                    q.isEmpty() || q in fold("${x.dialect} ${x.italian} ${x.english}")
                }
                if (shown.isEmpty()) return@forEachIndexed
                item(key = "h$si") { SectionHeader("${s.english} · ${s.italian} (${shown.size})") }
                items(shown, key = { "$si/${it.index}" }) { (ei, x) ->
                    ListRow(x.dialect + if (x.check) " $CHECK" else "", translation(x),
                        { nav.open(Destination.ExpressionItem(t.id, si, ei)) },
                        tags = listOf(x.region).filter { it.isNotEmpty() && it != "Rögn" })
                }
            }
        }
    }
}

/** One expression, with the dictionary words and verbs in it. */
@Composable
fun ExpressionScreen(lib: Library, nav: Navigator, d: Destination.ExpressionItem) {
    val t = lib.topic(d.topic) ?: return
    val x = t.sections.getOrNull(d.section)?.entries?.getOrNull(d.entry) ?: return
    val words = remember(d) { lib.wordsIn(x.dialect) }
    LazyColumn {
        item {
            InfoCard {
                Text(x.dialect + if (x.check) " $CHECK" else "", style = MaterialTheme.typography.headlineSmall)
                Labelled("IT", x.italian)
                Labelled("EN", x.english)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (x.region.isNotEmpty()) Tag(x.region)
                    Tag(t.english)
                }
                x.sources.forEach {
                    Text(sourceTitle(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        if (words.isNotEmpty()) {
            item { SectionHeader("Words in it") }
            items(words) { (word, meaning, dest) -> ListRow(word, meaning, { nav.open(dest) }) }
        }
    }
}
