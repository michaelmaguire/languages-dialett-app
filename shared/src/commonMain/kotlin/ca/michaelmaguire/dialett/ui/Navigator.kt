package ca.michaelmaguire.dialett.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import ca.michaelmaguire.dialett.nav.Destination

enum class Tab(val label: String, val icon: ImageVector, val root: Destination) {
    SEARCH("Search", Icons.Filled.Search, Destination.Search),
    WORDS("Words", Icons.AutoMirrored.Filled.List, Destination.Dictionary),
    VERBS("Verbs", Icons.Filled.PlayArrow, Destination.Verbs),
    GRAMMAR("Grammar", Icons.Filled.Build, Destination.Grammar),
    EXPRESSIONS("Phrases", Icons.Filled.Face, Destination.Expressions),
}

/** Which grammar text to show: Italian, English, or both one after the other. */
enum class Languages(val label: String) { BOTH("IT + EN"), ITALIAN("IT"), ENGLISH("EN") }

/**
 * Where the user is: a tab, and in each tab a stack of screens (its root first). Each tab keeps its own
 * stack, so switching tabs and coming back returns to the same screen.
 */
class Navigator {
    var tab by mutableStateOf(Tab.SEARCH)
        private set
    private val stacks = Tab.entries.associateWith { mutableStateListOf(it.root) }

    /** The grammar's language choice, kept while the app runs. */
    var languages by mutableStateOf(Languages.BOTH)

    val stack: List<Destination> get() = stacks.getValue(tab)
    val current: Destination get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1 || tab != Tab.SEARCH

    fun open(destination: Destination) {
        stacks.getValue(tab).add(destination)
    }

    /** A tab tapped: switch to it, or if it's the current tab, back to its first screen. */
    fun select(t: Tab) {
        if (t == tab) stacks.getValue(t).let { while (it.size > 1) it.removeAt(it.lastIndex) } else tab = t
    }

    fun back() {
        val s = stacks.getValue(tab)
        if (s.size > 1) s.removeAt(s.lastIndex) else if (tab != Tab.SEARCH) tab = Tab.SEARCH
    }

    /** A key for the saved state (scroll position, filter text) of the screen at depth i of the current tab. */
    fun key(i: Int = stack.lastIndex): String = "${tab.name}/$i/${stack[i]}"
}
