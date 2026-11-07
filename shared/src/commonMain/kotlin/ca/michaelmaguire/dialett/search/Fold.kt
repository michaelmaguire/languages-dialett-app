package ca.michaelmaguire.dialett.search

/**
 * Text as search compares it: lower case, without accents (ü → u, ö → o, è → e), apostrophes dropped
 * (l’è → le) and other punctuation as spaces, so a word is found however it's typed on a phone keyboard.
 * Markers (†, ⚠) and Markdown asterisks go too.
 */
fun fold(text: String): String = foldWithMap(text).first

/**
 * fold(), with the position in [text] of each folded character, so that a match can be shown in the
 * original (snippets).
 */
fun foldWithMap(text: String): Pair<String, IntArray> {
    val out = StringBuilder(text.length)
    val map = IntArray(text.length + 1)
    var space = true
    for ((i, c) in text.withIndex()) {
        val f = plain(c.lowercaseChar())
        when {
            f == null -> continue
            f == ' ' -> if (!space) {
                map[out.length] = i
                out.append(' ')
                space = true
            }
            else -> {
                map[out.length] = i
                out.append(f)
                space = false
            }
        }
    }
    if (out.endsWith(" ")) out.setLength(out.length - 1)
    map[out.length] = text.length
    return out.toString() to map.copyOf(out.length + 1)
}

/** Repeated letters as one (pee → pe, büttà → buta): the course writes long vowels and doubled consonants both ways. */
fun collapse(folded: String): String {
    val out = StringBuilder(folded.length)
    for (c in folded) if (out.isEmpty() || out.last() != c || c == ' ') out.append(c)
    return out.toString()
}

/** A folded character, ' ' for a separator, or null to drop it. */
private fun plain(c: Char): Char? = when (c) {
    in 'a'..'z', in '0'..'9' -> c
    'à', 'á', 'â', 'ä', 'ã', 'å' -> 'a'
    'è', 'é', 'ê', 'ë' -> 'e'
    'ì', 'í', 'î', 'ï' -> 'i'
    'ò', 'ó', 'ô', 'ö', 'õ', 'ø' -> 'o'
    'ù', 'ú', 'û', 'ü' -> 'u'
    'ç' -> 'c'
    'ñ' -> 'n'
    'ß' -> 's'
    '\'', '’', '‘', '`', '´', 'ʼ', '*', '†', '⚠', '️' -> null
    else -> if (c.isLetterOrDigit()) c else ' '
}
