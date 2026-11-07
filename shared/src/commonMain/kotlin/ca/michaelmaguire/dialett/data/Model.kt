package ca.michaelmaguire.dialett.data

import kotlinx.serialization.Serializable

// The app's data: files/dialett.json, made by step6_build_app_data.py in the languages-dialett repository
// from the same models as its pages and PDFs. Field names follow that file.

/** The schema version this app reads; a file with a higher one needs a newer app. */
const val DATA_VERSION = 1

@Serializable
data class AppData(
    val version: Int,
    val edition: String,
    val dictionary: List<Entry>,
    val verbs: List<Verb>,
    val expressions: List<Topic>,
    val grammar: List<Chapter>,
)

/** One headword of the dictionary, with its senses (meanings, or the same word from another region). */
@Serializable
data class Entry(
    val head: String,
    val flag: Boolean = false,
    val notes: List<String> = emptyList(),
    val senses: List<Sense>,
)

@Serializable
data class Sense(
    val dialect: String,
    val label: String = "",
    val pos: String = "",
    val italian: String = "",
    val english: String = "",
    val region: String = "",
    val note: String = "",
    val source: String = "",
    val check: Boolean = false,
    val newWord: Boolean = false,
    /** The id of this verb's conjugation page, or "". */
    val verb: String = "",
)

@Serializable
data class Verb(
    val id: String,
    val dialect: String,
    val italian: String,
    val english: String,
    val infinitiveInferred: Boolean = false,
    val verbClass: String = "",
    val gerund: List<Form> = emptyList(),
    val participle: String = "",
    val participleInferred: Boolean = false,
    val note: String = "",
    val noteEnglish: String = "",
    /** How many forms were found in the course documents, e.g. "12 of 68". */
    val found: String = "",
    val sources: List<String> = emptyList(),
    val tenses: List<Tense>,
)

@Serializable
data class Tense(val mood: String, val name: String, val forms: List<Form>)

/** A verb form; label is the person (mi, ti, le / lü …), inferred = predicted, not found in a document (†). */
@Serializable
data class Form(val label: String = "", val text: String, val inferred: Boolean = false)

@Serializable
data class Topic(
    val id: String,
    val dialect: String,
    val italian: String,
    val english: String,
    val sections: List<TopicSection>,
)

/** genre: "saying" (sayings and idioms) or "phrase". */
@Serializable
data class TopicSection(val genre: String, val italian: String, val english: String, val entries: List<Expression>)

@Serializable
data class Expression(
    val dialect: String,
    val italian: String = "",
    val english: String = "",
    val region: String = "",
    val check: Boolean = false,
    val sources: List<String> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String,
    /** "" for the introduction. */
    val number: String,
    val italian: String,
    val english: String,
    val blocks: List<Block>,
)

/** A piece of a grammar chapter. Text keeps the chapter's *italics* and **bold**. */
@Serializable
sealed interface Block

@Serializable
@kotlinx.serialization.SerialName("heading")
data class Heading(val italian: String, val english: String) : Block

@Serializable
@kotlinx.serialization.SerialName("paragraphs")
data class Paragraphs(val pairs: List<Bilingual>) : Block

@Serializable
data class Bilingual(val italian: String, val english: String)

/** A glossed example: the dialect sentence, its words with their glosses, and the translations. */
@Serializable
@kotlinx.serialization.SerialName("example")
data class Example(
    val number: Int,
    val dialect: String,
    val words: List<String> = emptyList(),
    val gloss: List<String> = emptyList(),
    val italian: String = "",
    val english: String = "",
    val source: String = "",
    val comment: String = "",
) : Block

@Serializable
@kotlinx.serialization.SerialName("table")
data class Table(val header: List<String>, val rows: List<List<String>>) : Block

/** A verb table: several verbs in one tense, or one verb in several tenses (a column each). */
@Serializable
@kotlinx.serialization.SerialName("verbs")
data class VerbTable(val caption: Column, val columns: List<Column>, val rows: List<VerbRow>) : Block

/** A caption or column heading; verb = the id of a verb page it links to, or "". */
@Serializable
data class Column(val title: String, val subtitle: String = "", val verb: String = "")

@Serializable
data class VerbRow(val label: String, val forms: List<Form>)
