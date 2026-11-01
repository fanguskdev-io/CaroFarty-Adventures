package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The game's own pixel font: capitals of 5 × 7 pixels with Spanish accents, ñ and ¡¿, digits and the punctuation the
 * screens use. It is drawn with rectangles in game pixels, so it stays on the pixel grid at any scale (ADR 0003).
 */
internal object PixelFont {
    const val HEIGHT = 7

    /** How far apart two lines are, with room for the accents above the capitals. */
    const val LINE = 10

    private class Glyph(val rows: List<String>, val accent: List<String> = emptyList()) {
        val width: Int = rows.maxOf { it.length }
        val body = PixelRuns(rows)
        val mark = PixelRuns(accent)
    }

    private val acute = listOf("...#.", "..#..")
    private val tilde = listOf(".##.#", "#..#.")
    private val diaeresis = listOf("", ".#.#.")

    private val glyphs: Map<Char, Glyph> by lazy {
        val base = mapOf(
            'A' to listOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
            'B' to listOf("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."),
            'C' to listOf(".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."),
            'D' to listOf("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
            'E' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
            'F' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#...."),
            'G' to listOf(".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"),
            'H' to listOf("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
            'I' to listOf("###", ".#.", ".#.", ".#.", ".#.", ".#.", "###"),
            'J' to listOf("..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."),
            'K' to listOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
            'L' to listOf("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
            'M' to listOf("#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"),
            'N' to listOf("#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"),
            'O' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            'P' to listOf("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
            'Q' to listOf(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
            'R' to listOf("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
            'S' to listOf(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
            'T' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
            'U' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            'V' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."),
            'W' to listOf("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"),
            'X' to listOf("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
            'Y' to listOf("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
            'Z' to listOf("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
            '0' to listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
            '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
            '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
            '3' to listOf("####.", "....#", "....#", ".###.", "....#", "....#", "####."),
            '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
            '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
            '6' to listOf(".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."),
            '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
            '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
            '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."),
            ' ' to listOf("...", "...", "...", "...", "...", "...", "..."),
            '.' to listOf(".", ".", ".", ".", ".", ".", "#"),
            ',' to listOf("..", "..", "..", "..", "..", ".#", "#."),
            ':' to listOf(".", "#", ".", ".", ".", "#", "."),
            '!' to listOf("#", "#", "#", "#", "#", ".", "#"),
            '¡' to listOf("#", ".", "#", "#", "#", "#", "#"),
            '?' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#.."),
            '¿' to listOf("..#..", ".....", "..#..", ".#...", "#....", "#...#", ".###."),
            '-' to listOf("...", "...", "...", "###", "...", "...", "..."),
            '+' to listOf(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."),
            '/' to listOf("....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."),
            '(' to listOf(".#", "#.", "#.", "#.", "#.", "#.", ".#"),
            ')' to listOf("#.", ".#", ".#", ".#", ".#", ".#", "#."),
            '%' to listOf("##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##"),
            '\'' to listOf("#", "#", ".", ".", ".", ".", "."),
            '·' to listOf(".", ".", ".", "#", ".", ".", "."),
        ).mapValues { Glyph(it.value) }
        val accented = mapOf(
            'Á' to Glyph(base.getValue('A').rows, acute),
            'É' to Glyph(base.getValue('E').rows, acute),
            'Í' to Glyph(base.getValue('I').rows, listOf("..#", ".#.")),
            'Ó' to Glyph(base.getValue('O').rows, acute),
            'Ú' to Glyph(base.getValue('U').rows, acute),
            'Ü' to Glyph(base.getValue('U').rows, diaeresis),
            'Ñ' to Glyph(base.getValue('N').rows, tilde),
        )
        base + accented
    }

    private fun glyph(char: Char): Glyph = glyphs[char.uppercaseChar()] ?: glyphs.getValue('?')

    /** How many game pixels [text] takes across at [size] (each pixel of the font, [size] × [size] game pixels). */
    fun width(text: String, size: Int = 1): Int {
        var width = 0
        for (char in text) width += glyph(char).width + 1
        return (width - 1).coerceAtLeast(0) * size
    }

    /**
     * Draws [text] with its top left at ([x], [y]) in game pixels, in [color], with a [shadow] one pixel down if given, at
     * [size].
     */
    fun DrawScope.drawText(
        text: String,
        x: Int,
        y: Int,
        color: Color,
        shadow: Color? = null,
        size: Int = 1,
    ) {
        if (shadow != null) drawLine(text, x, y + size, color = shadow, size = size)
        drawLine(text, x, y, color, size)
    }

    private fun DrawScope.drawLine(
        text: String,
        x: Int,
        y: Int,
        color: Color,
        size: Int,
    ) {
        var left = x
        for (char in text) {
            val glyph = glyph(char)
            glyph.body.draw(this, left, y, size) { color }
            glyph.mark.draw(this, left, y - (glyph.accent.size + 1) * size, size) { color }
            left += (glyph.width + 1) * size
        }
    }
}
