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

package com.real3rr0r303yt.nothingnotes.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingnotes.markdown.InlineRun
import com.real3rr0r303yt.nothingnotes.markdown.Markdown
import com.real3rr0r303yt.nothingnotes.markdown.MdBlock
import com.real3rr0r303yt.nothingnotes.ui.theme.MonoFamily
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingRed
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingSurfaceHigh
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingWhite

/** Read-only Markdown rendering with tappable to-do checkboxes. */
@Composable
fun MarkdownView(
    source: String,
    onToggleTodo: (line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(source) { Markdown.parse(source) }
    val body = MaterialTheme.typography.bodyLarge.copy(color = NothingWhite)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(
                        inline(block.text),
                        style = style.copy(color = NothingWhite),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                is MdBlock.Paragraph -> Text(inline(block.text), style = body)
                is MdBlock.Bullet -> ListRow(block.indent) {
                    Box(Modifier.padding(top = 9.dp).size(5.dp).background(NothingWhite, CircleShape))
                    Text(inline(block.text), style = body, modifier = Modifier.weight(1f))
                }
                is MdBlock.Numbered -> ListRow(block.indent) {
                    Text("${block.number}.", style = body.copy(fontFamily = MonoFamily, color = NothingGrey))
                    Text(inline(block.text), style = body, modifier = Modifier.weight(1f))
                }
                is MdBlock.Todo -> TodoRow(block, body, onToggleTodo)
                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min).padding(vertical = 2.dp)) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(NothingRed))
                    Text(
                        inline(block.text),
                        style = body.copy(color = NothingGrey, fontStyle = FontStyle.Italic),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                is MdBlock.Code -> Text(
                    block.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily, color = NothingWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NothingSurfaceHigh, MaterialTheme.shapes.small)
                        .padding(12.dp),
                )
                MdBlock.Rule -> DottedRule()
                MdBlock.Blank -> Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ListRow(indent: Int, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.padding(start = (indent * 16).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun TodoRow(block: MdBlock.Todo, body: TextStyle, onToggle: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = (block.indent * 16).dp)
            .clickable(role = Role.Checkbox) { onToggle(block.line) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .then(
                    if (block.checked) Modifier.background(NothingRed, CircleShape)
                    else Modifier.border(1.5.dp, NothingGrey, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (block.checked) Icon(Icons.Outlined.Check, null, tint = NothingWhite, modifier = Modifier.size(14.dp))
        }
        Text(
            inline(block.text),
            style = if (block.checked) {
                body.copy(color = NothingGreyDim, textDecoration = TextDecoration.LineThrough)
            } else {
                body
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** A horizontal rule drawn as a row of dots. */
@Composable
private fun DottedRule() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(2.dp),
    ) {
        val step = 8.dp.toPx()
        var x = step / 2
        while (x < size.width) {
            drawCircle(NothingOutline, 1.dp.toPx(), Offset(x, size.height / 2))
            x += step
        }
    }
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    Markdown.parseInline(text).forEach { run -> withStyle(run.toSpanStyle()) { append(run.text) } }
}

private fun InlineRun.toSpanStyle(): SpanStyle {
    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontFamily = if (code) MonoFamily else null,
        background = if (code) NothingSurfaceHigh else Color.Unspecified,
        color = if (code) NothingRed else Color.Unspecified,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
    )
}
