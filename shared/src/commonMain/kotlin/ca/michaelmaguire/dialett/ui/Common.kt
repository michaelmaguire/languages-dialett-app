package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

const val INFERRED = "†"
const val CHECK = "⚠"

/** The grammar's light Markdown: *italic* and **bold**. Unpaired asterisks (e.g. in a cut snippet) stay as text. */
fun rich(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val bold = text.startsWith("**", i)
        val marker = if (bold) "**" else if (text[i] == '*') "*" else null
        val end = marker?.let { text.indexOf(it, i + it.length) } ?: -1
        if (marker != null && end > i + marker.length) {
            withStyle(if (bold) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle(fontStyle = FontStyle.Italic)) {
                append(text.substring(i + marker.length, end))
            }
            i = end + marker.length
        } else {
            append(text[i])
            i++
        }
    }
}

@Composable
fun RichText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium, color: Color = Color.Unspecified) {
    Text(rich(text), modifier, style = style, color = color)
}

/** A filter box at the top of a list. */
@Composable
fun FilterField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value, onChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(28.dp),
    )
}

/** A heading inside a list (a letter, a section). */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** A small label: a region other than Rögn, a part of speech. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A row of a list: a title, the translation under it, tags on the right. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    detail: String = "",
    tags: List<String> = emptyList(),
    rich: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (rich) RichText(title, style = MaterialTheme.typography.titleMedium)
            else Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail.isNotEmpty()) RichText(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { Tag(it) } }
    }
}

/** A card for a piece of content (a sense, a tense, an example). */
@Composable
fun InfoCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val m = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    if (onClick != null) Card(onClick, m, colors = colors) { Column(Modifier.padding(14.dp)) { content() } }
    else Card(m, colors = colors) { Column(Modifier.padding(14.dp)) { content() } }
}

/** A labelled line: "IT  casa". */
@Composable
fun Labelled(label: String, text: String, italic: Boolean = false) {
    if (text.isEmpty()) return
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp, end = 10.dp))
        RichText(if (italic) "*$text*" else text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Where an entry comes from: a document's file name without its date prefix and extension. */
fun sourceTitle(file: String): String =
    file.replace(Regex("^\\d{4}-\\d{2}-\\d{2}-\\d{4}-"), "").replace(Regex("\\.(docx|pdf|xlsx|txt|csv)$"), "")
