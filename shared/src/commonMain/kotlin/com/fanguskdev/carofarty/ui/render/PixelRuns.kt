package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A small drawing given as rows of characters ('.' is empty), kept as its horizontal runs of the same character: drawing
 * it takes one rectangle per run instead of one per pixel, which is what a frame of the pixel font and of the placeholder
 * Caro costs (MOB-02). Worked out once; drawing allocates nothing.
 */
internal class PixelRuns(rows: List<String>) {
    /** Each run as four numbers: its column, its row, its length and its character (internal for [draw], which is inline). */
    internal val runs: IntArray

    init {
        val found = ArrayList<Int>()
        for (row in rows.indices) {
            val line = rows[row]
            var column = 0
            while (column < line.length) {
                val code = line[column]
                var end = column + 1
                while (end < line.length && line[end] == code) end++
                if (code != EMPTY) {
                    found += column
                    found += row
                    found += end - column
                    found += code.code
                }
                column = end
            }
        }
        runs = found.toIntArray()
    }

    /** Draws it with its top left at ([x], [y]), each pixel [size] × [size], in the [color] of each character (null skips it). */
    inline fun draw(
        scope: DrawScope,
        x: Int,
        y: Int,
        size: Int,
        color: (Char) -> Color?,
    ) {
        var i = 0
        while (i < runs.size) {
            val fill = color(runs[i + 3].toChar())
            if (fill != null) {
                scope.drawRect(
                    fill,
                    Offset((x + runs[i] * size).toFloat(), (y + runs[i + 1] * size).toFloat()),
                    Size((runs[i + 2] * size).toFloat(), size.toFloat()),
                )
            }
            i += 4
        }
    }

    private companion object {
        const val EMPTY = '.'
    }
}
