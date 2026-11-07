package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Block
import ca.michaelmaguire.dialett.data.Column as VerbColumn
import ca.michaelmaguire.dialett.data.Example
import ca.michaelmaguire.dialett.data.Heading
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.data.Paragraphs
import ca.michaelmaguire.dialett.data.Table
import ca.michaelmaguire.dialett.data.VerbTable
import ca.michaelmaguire.dialett.nav.Destination

/** The chapters. */
@Composable
fun GrammarListScreen(lib: Library, nav: Navigator) {
    LazyColumn {
        items(lib.data.grammar, key = { it.id }) { c ->
            ListRow(listOf(c.number, c.english).filter(String::isNotEmpty).joinToString(". "), c.italian,
                { nav.open(Destination.GrammarChapter(c.id)) })
        }
    }
}

/** The IT / EN / both switch in the top bar of a chapter. */
@Composable
fun LanguageSwitch(nav: Navigator) {
    SingleChoiceSegmentedButtonRow(Modifier.padding(end = 8.dp)) {
        Languages.entries.forEachIndexed { i, l ->
            SegmentedButton(
                selected = nav.languages == l, onClick = { nav.languages = l },
                shape = SegmentedButtonDefaults.itemShape(i, Languages.entries.size),
                icon = {},
            ) { Text(l.label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** A chapter, opened at a block when a search led here. */
@Composable
fun ChapterScreen(lib: Library, nav: Navigator, id: String, block: Int) {
    val c = lib.chapter(id) ?: return
    val list = rememberLazyListState(initialFirstVisibleItemIndex = if (block > 0) block + 1 else 0)
    val all = lib.data.grammar
    val at = all.indexOf(c)
    LazyColumn(state = list) {
        item {
            Column(Modifier.padding(16.dp)) {
                if (nav.languages != Languages.ENGLISH) Text(c.italian, style = MaterialTheme.typography.headlineSmall)
                if (nav.languages != Languages.ITALIAN) Text(c.english, style = MaterialTheme.typography.headlineSmall,
                    color = if (nav.languages == Languages.BOTH) MaterialTheme.colorScheme.secondary else Color.Unspecified)
            }
        }
        itemsIndexed(c.blocks) { _, b -> BlockView(b, nav) }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (at > 0) OutlinedButton({ nav.open(Destination.GrammarChapter(all[at - 1].id)) }) { Text("← ${all[at - 1].english}", maxLines = 1) }
                else Text("")
                if (at < all.lastIndex) OutlinedButton({ nav.open(Destination.GrammarChapter(all[at + 1].id)) }) { Text("${all[at + 1].english} →", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun BlockView(b: Block, nav: Navigator) {
    val lang = nav.languages
    when (b) {
        is Heading -> Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
            if (lang != Languages.ENGLISH) RichText(b.italian, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (lang != Languages.ITALIAN) RichText(b.english, style = MaterialTheme.typography.titleMedium,
                color = if (lang == Languages.BOTH) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
        }
        is Paragraphs -> Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            b.pairs.forEach { p -> Bilingual(p.italian, p.english, lang) }
        }
        is Example -> ExampleCard(b, lang)
        is Table -> Grid(listOf(b.header.map { Cell(it, bold = true) }) + b.rows.map { r -> r.map { Cell(it) } })
        is VerbTable -> VerbGrid(b, nav)
    }
}

/** A paragraph in one language or both; in both, the English under the Italian, set off by a rule. */
@Composable
private fun Bilingual(italian: String, english: String, lang: Languages) {
    Column(Modifier.padding(vertical = 6.dp)) {
        if (lang != Languages.ENGLISH) RichText(italian, style = MaterialTheme.typography.bodyLarge)
        if (lang == Languages.BOTH) {
            Row(Modifier.padding(top = 6.dp)) {
                Column(Modifier.width(3.dp).background(MaterialTheme.colorScheme.secondaryContainer)) { Text(" ") }
                RichText(english, Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (lang == Languages.ENGLISH) RichText(english, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A glossed example: the sentence, each word over its gloss (scrolls sideways), the translations, the source. */
@Composable
private fun ExampleCard(x: Example, lang: Languages) {
    InfoCard {
        Text("(${x.number})  ${x.dialect}", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
        if (x.words.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                x.words.forEachIndexed { i, w ->
                    Column {
                        Text(w, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(x.gloss.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (lang != Languages.ENGLISH) Labelled("IT", x.italian)
        if (lang != Languages.ITALIAN) Labelled("EN", x.english)
        if (x.comment.isNotEmpty()) RichText(x.comment, style = MaterialTheme.typography.bodySmall)
        if (x.source.isNotEmpty()) Text(sourceTitle(x.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

/** A table cell; link: a verb page it leads to. */
private data class Cell(val text: String, val bold: Boolean = false, val inferred: Boolean = false, val link: Destination? = null)

private fun width(cells: List<Cell>): Dp = ((cells.maxOfOrNull { it.text.length } ?: 4) * 8 + 20).coerceIn(56, 220).dp

/** A table (its first row the heading) that scrolls sideways when it's wider than the screen; each column as
 * wide as its text needs. */
@Composable
private fun Grid(all: List<List<Cell>>, caption: Cell? = null, nav: Navigator? = null) {
    val n = all.maxOf { it.size }
    val widths = (0 until n).map { i -> width(all.mapNotNull { it.getOrNull(i) }) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        caption?.let { CellText(it, nav, Modifier.padding(bottom = 4.dp)) }
        Column(Modifier.horizontalScroll(rememberScrollState()).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            all.forEachIndexed { r, row ->
                Row(Modifier.background(if (r == 0) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)) {
                    (0 until n).forEach { i ->
                        CellText(row.getOrNull(i) ?: Cell(""), nav, Modifier.width(widths[i]).padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CellText(c: Cell, nav: Navigator?, modifier: Modifier) {
    val m = if (c.link != null && nav != null) modifier.clickable { nav.open(c.link) } else modifier
    RichText(
        c.text + if (c.inferred) " $INFERRED" else "", m,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (c.bold) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (c.inferred) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (c.link != null) TextDecoration.Underline else TextDecoration.None,
        ),
        color = if (c.link != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
    )
}

/** A verb table: the persons down the side, a column per verb or per tense; verbs link to their pages. */
@Composable
private fun VerbGrid(b: VerbTable, nav: Navigator) {
    fun cell(c: VerbColumn, bold: Boolean) = Cell(
        c.title + if (c.subtitle.isNotEmpty()) " (${c.subtitle})" else "", bold = bold,
        link = c.verb.takeIf { it.isNotEmpty() }?.let { Destination.VerbPage(it) },
    )
    val header = listOf(Cell("")) + b.columns.map { cell(it, true) }
    val rows = b.rows.map { r -> listOf(Cell(r.label)) + r.forms.map { Cell(it.text, inferred = it.inferred) } }
    Grid(listOf(header) + rows, caption = cell(b.caption, true), nav = nav)
}
