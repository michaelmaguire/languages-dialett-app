package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Form
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.data.Tense
import ca.michaelmaguire.dialett.nav.Destination
import ca.michaelmaguire.dialett.search.Kind
import ca.michaelmaguire.dialett.search.fold

/** All the verbs, A to Z; the filter also finds a verb by any of its forms. */
@Composable
fun VerbListScreen(lib: Library, nav: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    val sorted = remember(lib) { lib.data.verbs.sortedBy { fold(it.dialect) } }
    val shown = remember(query) {
        if (query.isBlank()) sorted
        else lib.index.search(query, setOf(Kind.VERB, Kind.VERB_FORM))
            .map { (it.destination as Destination.VerbPage).id }.distinct().mapNotNull { lib.verb(it) }
    }
    Column {
        FilterField(query, { query = it }, "Filter ${sorted.size} verbs, or type a form")
        LazyColumn {
            items(shown, key = { it.id }) { v ->
                ListRow(v.dialect + if (v.infinitiveInferred) " $INFERRED" else "", "${v.italian} · ${v.english}",
                    { nav.open(Destination.VerbPage(v.id, if (query.isBlank()) "" else query)) })
            }
        }
    }
}

/** A verb's page: infinitive, gerund, participle and class, then every tense, mood by mood. */
@Composable
fun VerbScreen(lib: Library, nav: Navigator, id: String, highlight: String) {
    val v = lib.verb(id) ?: return
    val mark = fold(highlight)
    val moods = remember(id) { v.tenses.groupBy { it.mood } }
    BoxWithConstraints {
    // two tense cards side by side when there's room (tablet, landscape), else one per row
    val perRow = if (maxWidth > 560.dp) 2 else 1
    val rows = remember(id, perRow) { moods.mapValues { (_, t) -> t.chunked(perRow) } }
    // open at the row with the form searched for. Items: the header card, then per mood a heading and its rows
    val first = remember(id, mark, perRow) {
        var item = if (v.also.isNotEmpty()) 2 else 1   // the header card, and the "Also" card if any
        var found = 0
        loop@ for ((_, rs) in rows) {
            item++
            for (r in rs) {
                if (r.any { t -> t.forms.any { matches(it, mark) } }) { found = item; break@loop }
                item++
            }
        }
        found
    }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = first)
    LazyColumn(state = list) {
        item {
            InfoCard {
                Text(v.dialect + if (v.infinitiveInferred) " $INFERRED" else "", style = MaterialTheme.typography.headlineMedium)
                Labelled("IT", v.italian)
                Labelled("EN", v.english)
                Labelled("gerundio", v.gerund.joinToString(", ") { it.text + if (it.inferred) " $INFERRED" else "" })
                Labelled("participio", v.participle + if (v.participleInferred) " $INFERRED" else "")
                Text(v.verbClass, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
                if (v.found.isNotEmpty()) Text(
                    "Forms found in the course: ${v.found}. $INFERRED = predicted from the course's patterns.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (v.also.isNotEmpty()) item {
            InfoCard {
                Text("Also", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                v.also.forEach { a ->
                    Column(Modifier.padding(top = 6.dp)) {
                        Text(a.dialect, style = MaterialTheme.typography.titleMedium)
                        Text(listOf(a.italian, a.english).filter(String::isNotEmpty).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        for ((mood, rs) in rows) {
            item { SectionHeader(mood) }
            items(rs) { r ->
                Row(Modifier.fillMaxWidth()) {
                    r.forEach { t -> Column(Modifier.weight(1f)) { TenseCard(t, mark) } }
                    repeat(perRow - r.size) { Column(Modifier.weight(1f)) {} }
                }
            }
        }
        if (v.note.isNotEmpty() || v.noteEnglish.isNotEmpty()) item {
            InfoCard {
                if (v.note.isNotEmpty()) Text(v.note, style = MaterialTheme.typography.bodyMedium)
                if (v.noteEnglish.isNotEmpty()) Text(v.noteEnglish, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (v.sources.isNotEmpty()) item {
            Text("Sources: " + v.sources.joinToString("; ") { sourceTitle(it) }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(16.dp))
        }
    }
}
}

/** Whether a form is the one searched for: one of its words, or (for "ta bütat") the whole of it. */
private fun matches(f: Form, mark: String): Boolean {
    if (mark.isEmpty()) return false
    val text = fold(f.text)
    return text == mark || mark in text.split(' ', '/') || (' ' in mark && mark in text)
}

@Composable
private fun TenseCard(t: Tense, mark: String) {
    InfoCard {
        val all = t.forms.all { it.inferred }
        Text(t.name + if (all) " $INFERRED" else "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        t.forms.forEach { f ->
            val hit = matches(f, mark)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    .background(if (hit) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent, RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(f.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(72.dp).padding(start = 4.dp))
                Text(
                    f.text + if (f.inferred && !all) " $INFERRED" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (f.inferred) FontWeight.Normal else FontWeight.SemiBold,
                    fontStyle = if (f.inferred) FontStyle.Italic else FontStyle.Normal,
                    color = if (f.inferred) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
