package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.search.Hit
import ca.michaelmaguire.dialett.search.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The filters above the results; Verbs covers the verbs and their conjugated forms. */
private enum class Filter(val label: String, val kinds: Set<Kind>) {
    ALL("All", Kind.entries.toSet()),
    WORDS("Words", setOf(Kind.WORD)),
    VERBS("Verbs", setOf(Kind.VERB, Kind.VERB_FORM)),
    PHRASES("Phrases", setOf(Kind.EXPRESSION)),
    GRAMMAR("Grammar", setOf(Kind.GRAMMAR)),
}

/** Results of each kind shown under "All" before "Show all". */
private const val PREVIEW = 6

/** Search in the dialect, Italian or English, across the whole book. */
@Composable
fun SearchScreen(lib: Library, nav: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    val results by produceState(emptyList<Hit>(), query, filter) {
        delay(80)   // let a few more letters arrive while typing
        value = withContext(Dispatchers.Default) { lib.index.search(query, filter.kinds) }
    }
    val list = rememberLazyListState()
    // new results start at the top (the list would keep the last query's scroll position); not on the
    // first run, so that coming back from a result keeps the place
    var last by rememberSaveable { mutableStateOf(query to filter) }
    LaunchedEffect(results) {
        if (last != (query to filter)) {
            last = query to filter
            list.scrollToItem(0)
        }
    }
    Column {
        FilterField(query, { query = it }, "Search in dialett, italiano or English")
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Filter.entries) { f ->
                FilterChip(filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        if (query.isBlank()) {
            Intro(lib)
            return@Column
        }
        if (results.isEmpty()) {
            Text("Nothing found for “$query”.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
            return@Column
        }
        LazyColumn(state = list) {
            val groups = results.groupBy { it.kind }
            for (kind in Kind.entries) {
                val hits = groups[kind] ?: continue
                val shown = if (filter == Filter.ALL) hits.take(PREVIEW) else hits
                item(key = "h-$kind") { SectionHeader("${kind.title} (${hits.size})") }
                items(shown, key = { "$kind/${it.destination}/${it.title}" }) { HitRow(it, nav) }
                if (shown.size < hits.size) item(key = "more-$kind") {
                    Text(
                        "Show all ${hits.size} ${kind.title.lowercase()}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable {
                            filter = Filter.entries.first { kind in it.kinds && it != Filter.ALL }
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item(key = "d-$kind") { HorizontalDivider() }
            }
        }
    }
}

@Composable
private fun HitRow(hit: Hit, nav: Navigator) {
    when (hit.kind) {
        Kind.GRAMMAR -> ListRow(hit.title, hit.subtitle, { nav.open(hit.destination) },
            detail = hit.snippet.ifEmpty { hit.detail }, rich = true)
        Kind.VERB_FORM -> ListRow(hit.title, hit.subtitle, { nav.open(hit.destination) }, detail = hit.detail)
        else -> ListRow(hit.title, hit.subtitle, { nav.open(hit.destination) },
            tags = listOf(hit.detail).filter { it.isNotEmpty() && hit.kind == Kind.WORD })
    }
}

@Composable
private fun Intro(lib: Library) {
    val d = lib.data
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("The dialect of Rögn (Arogno)", style = MaterialTheme.typography.titleLarge)
        Text(
            "${d.dictionary.size} words, ${d.verbs.size} verbs, " +
                "${d.expressions.sumOf { t -> t.sections.sumOf { it.entries.size } }} expressions and " +
                "${d.grammar.size} grammar chapters from Guido Casellini's course.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Type a word in the dialect, Italian or English. Accents don't matter: buta finds bütà. " +
                "A conjugated form finds its verb (bütavum), and a few words find a saying (boca lüf).",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "$INFERRED a form or rule worked out, not found in a course document · $CHECK to check with the teacher",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
        )
        Text("Edition ${d.edition}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
