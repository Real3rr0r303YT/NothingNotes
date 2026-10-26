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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    @Test
    fun `parses block types`() {
        val blocks = Markdown.parse(
            """
            # Title
            - item
            - [ ] open
            - [x] done
            1. first
            > quote
            ---
            plain
            """.trimIndent(),
        )
        assertEquals(MdBlock.Heading(1, "Title"), blocks[0])
        assertEquals(MdBlock.Bullet(0, "item"), blocks[1])
        assertEquals(MdBlock.Todo(0, false, "open", 2), blocks[2])
        assertEquals(MdBlock.Todo(0, true, "done", 3), blocks[3])
        assertEquals(MdBlock.Numbered(0, "1", "first"), blocks[4])
        assertEquals(MdBlock.Quote("quote"), blocks[5])
        assertEquals(MdBlock.Rule, blocks[6])
        assertEquals(MdBlock.Paragraph("plain"), blocks[7])
    }

    @Test
    fun `code fences keep line numbers of later todos correct`() {
        val blocks = Markdown.parse("```\ncode\n```\n- [ ] task")
        assertEquals(MdBlock.Code("code"), blocks[0])
        assertEquals(MdBlock.Todo(0, false, "task", 3), blocks[1])
    }

    @Test
    fun `parses nested inline styles`() {
        val runs = Markdown.parseInline("a **bold *both*** `x` ~~gone~~")
        assertEquals(
            listOf(
                InlineRun("a "),
                InlineRun("bold ", bold = true),
                InlineRun("both", bold = true, italic = true),
                InlineRun(" "),
                InlineRun("x", code = true),
                InlineRun(" "),
                InlineRun("gone", strike = true),
            ),
            runs,
        )
    }

    @Test
    fun `unclosed markers stay literal`() {
        assertEquals(listOf(InlineRun("2 * 3 = 6")), Markdown.parseInline("2 * 3 = 6"))
    }

    @Test
    fun `plain text strips markup`() {
        assertEquals("Hello\n○  todo", Markdown.toPlainText("# **Hello**\n- [ ] todo"))
    }

    @Test
    fun `checklist toggles and counts`() {
        val src = "- [ ] a\n- [x] b\ntext"
        assertEquals(Checklist.Progress(1, 2), Checklist.progress(src))
        assertEquals("- [x] a\n- [x] b\ntext", Checklist.toggle(src, 0))
        assertEquals("- [ ] a\n- [ ] b\ntext", Checklist.toggle(src, 1))
        assertEquals(src, Checklist.toggle(src, 2))
    }

    @Test
    fun `wrap and unwrap selection`() {
        val wrapped = MarkdownEditing.wrap("say hi", 4, 6, "**")
        assertEquals(TextEdit("say **hi**", 6, 8), wrapped)
        assertEquals(TextEdit("say hi", 4, 6), MarkdownEditing.wrap(wrapped.text, 6, 8, "**"))
    }

    @Test
    fun `line prefix toggles and replaces other list markers`() {
        val todo = MarkdownEditing.toggleLinePrefix("- milk", 3, 3, "- [ ] ")
        assertEquals("- [ ] milk", todo.text)
        assertEquals("milk", MarkdownEditing.toggleLinePrefix(todo.text, 7, 7, "- [ ] ").text)
        val multi = MarkdownEditing.toggleLinePrefix("a\nb", 0, 3, "- ")
        assertEquals("- a\n- b", multi.text)
    }

    @Test
    fun `enter continues and ends lists`() {
        val cont = MarkdownEditing.continueList("- [x] milk\n", 11)!!
        assertEquals("- [x] milk\n- [ ] ", cont.text)
        assertEquals(17, cont.selectionStart)

        val numbered = MarkdownEditing.continueList("9. nine\n", 8)!!
        assertEquals("9. nine\n10. ", numbered.text)

        val end = MarkdownEditing.continueList("- a\n- \n", 7)!!
        assertEquals("- a\n", end.text)
        assertTrue(MarkdownEditing.continueList("plain\n", 6) == null)
    }
}
