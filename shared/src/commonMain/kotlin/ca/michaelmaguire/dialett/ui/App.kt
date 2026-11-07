package ca.michaelmaguire.dialett.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.michaelmaguire.dialett.data.Library
import ca.michaelmaguire.dialett.data.loadLibrary
import ca.michaelmaguire.dialett.nav.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Colours after the course's covers: a deep red and a lake blue.
private val Light = lightColorScheme(primary = Color(0xFF8E2C2C), secondary = Color(0xFF2E5E7E), tertiary = Color(0xFF5E6B2E))
private val Dark = darkColorScheme(primary = Color(0xFFFFB3AC), secondary = Color(0xFFA4CCEB), tertiary = Color(0xFFC6D38E))

fun defaultColors(dark: Boolean): ColorScheme = if (dark) Dark else Light

/** The whole app. Each platform supplies its colours (Android: the wallpaper's, on Android 12+). */
@Composable
fun App(colors: ColorScheme) {
    MaterialTheme(colorScheme = colors) {
        var library by remember { mutableStateOf<Library?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            try {
                library = withContext(Dispatchers.Default) { loadLibrary() }
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            }
        }
        val lib = library
        when {
            lib != null -> Main(lib, remember { Navigator() })
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) { Text("Couldn't read the data: $error") }
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Main(lib: Library, nav: Navigator) {
    PlatformBackHandler(nav.canGoBack) { nav.back() }
    val saved = rememberSaveableStateHolder()
    val dest = nav.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title(lib, dest), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (nav.stack.size > 1) IconButton(onClick = { nav.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { if (dest is Destination.GrammarChapter) LanguageSwitch(nav) },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = nav.tab == t, onClick = { nav.select(t) },
                        icon = { Icon(t.icon, contentDescription = null) }, label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // each screen keeps its scroll position and filter text while it's in a stack
            saved.SaveableStateProvider(nav.key()) { Screen(lib, nav, dest) }
        }
    }
}

@Composable
private fun Screen(lib: Library, nav: Navigator, dest: Destination) {
    when (dest) {
        Destination.Search -> SearchScreen(lib, nav)
        Destination.Dictionary -> DictionaryScreen(lib, nav)
        Destination.Verbs -> VerbListScreen(lib, nav)
        Destination.Grammar -> GrammarListScreen(lib, nav)
        Destination.Expressions -> TopicListScreen(lib, nav)
        is Destination.Word -> WordScreen(lib, nav, dest.entry)
        is Destination.VerbPage -> VerbScreen(lib, nav, dest.id, dest.highlight)
        is Destination.GrammarChapter -> ChapterScreen(lib, nav, dest.id, dest.block)
        is Destination.ExpressionTopic -> TopicScreen(lib, nav, dest.id)
        is Destination.ExpressionItem -> ExpressionScreen(lib, nav, dest)
    }
}

private fun title(lib: Library, dest: Destination): String = when (dest) {
    Destination.Search -> "Dialett da Rögn"
    Destination.Dictionary -> "Dictionary"
    Destination.Verbs -> "Verbs"
    Destination.Grammar -> "Grammar"
    Destination.Expressions -> "Expressions"
    is Destination.Word -> lib.data.dictionary[dest.entry].head
    is Destination.VerbPage -> lib.verb(dest.id)?.dialect ?: "Verb"
    is Destination.GrammarChapter -> lib.chapter(dest.id)?.let { listOf(it.number, it.english).filter(String::isNotEmpty).joinToString(". ") } ?: "Grammar"
    is Destination.ExpressionTopic -> lib.topic(dest.id)?.english ?: "Expressions"
    is Destination.ExpressionItem -> lib.topic(dest.topic)?.english ?: "Expression"
}
