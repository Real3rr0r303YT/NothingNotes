/*
 * NothingNotes – privacy-first notes for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingnotes.markdown

/**
 * A tiny, dependency-free Markdown subset tailored for notes:
 * headings, bullet/numbered lists, to-dos, quotes, code blocks, rules and
 * inline **bold**, *italic*, ~~strike~~ and `code`.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val indent: Int, val text: String) : MdBlock
    data class Numbered(val indent: Int, val number: String, val text: String) : MdBlock
    data class Todo(val indent: Int, val checked: Boolean, val text: String, val line: Int) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
    data object Blank : MdBlock
}

data class InlineRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
)

object Markdown {

    private val HEADING = Regex("""^(#{1,3})\s+(.*)$""")
    private val TODO = Regex("""^(\s*)[-*+]\s\[([ xX])]\s?(.*)$""")
    private val BULLET = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val NUMBERED = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
    private val QUOTE = Regex("""^>\s?(.*)$""")
    private val RULE = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")

    fun parse(source: String): List<MdBlock> {
        val lines = source.split('\n')
        val blocks = mutableListOf<MdBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.trimStart().startsWith("```")) {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                blocks += MdBlock.Code(code.toString())
                i++ // skip closing fence (or run past the end)
                continue
            }
            blocks += parseLine(line, i)
            i++
        }
        return blocks
    }

    private fun parseLine(line: String, index: Int): MdBlock {
        if (line.isBlank()) return MdBlock.Blank
        TODO.matchEntire(line)?.let { m ->
            val (indent, mark, text) = m.destructured
            return MdBlock.Todo(indent.indentLevel(), mark.isNotBlank(), text, index)
        }
        if (RULE.matches(line)) return MdBlock.Rule
        HEADING.matchEntire(line)?.let { m ->
            return MdBlock.Heading(m.groupValues[1].length, m.groupValues[2])
        }
        BULLET.matchEntire(line)?.let { m ->
            return MdBlock.Bullet(m.groupValues[1].indentLevel(), m.groupValues[2])
        }
        NUMBERED.matchEntire(line)?.let { m ->
            val (indent, number, text) = m.destructured
            return MdBlock.Numbered(indent.indentLevel(), number, text)
        }
        QUOTE.matchEntire(line)?.let { m -> return MdBlock.Quote(m.groupValues[1]) }
        return MdBlock.Paragraph(line)
    }

    private fun String.indentLevel() = replace("\t", "    ").length / 2

    /** Splits inline markup into styled runs. Unclosed markers stay literal. */
    fun parseInline(text: String): List<InlineRun> {
        val runs = mutableListOf<InlineRun>()
        parseInline(text, InlineRun(""), runs)
        return runs.filter { it.text.isNotEmpty() }
    }

    private fun parseInline(text: String, style: InlineRun, out: MutableList<InlineRun>) {
        val literal = StringBuilder()
        fun flush() {
            if (literal.isNotEmpty()) {
                out += style.copy(text = literal.toString())
                literal.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length && text[i + 1] in ESCAPABLE) {
                literal.append(text[i + 1])
                i += 2
                continue
            }
            if (c == '`') {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    flush()
                    out += style.copy(text = text.substring(i + 1, end), code = true)
                    i = end + 1
                    continue
                }
            }
            val marker = when {
                text.startsWith("**", i) -> "**"
                text.startsWith("__", i) -> "__"
                text.startsWith("~~", i) -> "~~"
                c == '*' || c == '_' -> c.toString()
                else -> null
            }
            if (marker != null) {
                var end = text.indexOf(marker, i + marker.length)
                // "***" closing a bold run that ends in italics: the bold marker is the last two.
                while (marker.length == 2 && end >= 0 && end + 2 < text.length && text[end + 2] == marker[0]) end++
                if (end > i + marker.length) {
                    flush()
                    val inner = text.substring(i + marker.length, end)
                    val innerStyle = when (marker) {
                        "**", "__" -> style.copy(bold = true)
                        "~~" -> style.copy(strike = true)
                        else -> style.copy(italic = true)
                    }
                    parseInline(inner, innerStyle, out)
                    i = end + marker.length
                    continue
                }
            }
            literal.append(c)
            i++
        }
        flush()
    }

    /** Plain-text rendition used for card previews and search. */
    fun toPlainText(source: String): String = parse(source).mapNotNull { block ->
        when (block) {
            is MdBlock.Heading -> plain(block.text)
            is MdBlock.Paragraph -> plain(block.text)
            is MdBlock.Bullet -> "•  " + plain(block.text)
            is MdBlock.Numbered -> "${block.number}. " + plain(block.text)
            is MdBlock.Todo -> (if (block.checked) "●  " else "○  ") + plain(block.text)
            is MdBlock.Quote -> plain(block.text)
            is MdBlock.Code -> block.text
            MdBlock.Rule -> null
            MdBlock.Blank -> ""
        }
    }.joinToString("\n").trim()

    private fun plain(text: String) = parseInline(text).joinToString("") { it.text }

    private const val ESCAPABLE = "\\`*_~#>-[]"
}
