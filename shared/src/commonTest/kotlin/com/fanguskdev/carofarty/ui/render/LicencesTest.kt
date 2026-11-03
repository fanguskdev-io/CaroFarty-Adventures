package com.fanguskdev.carofarty.ui.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The whole texts of the licences in pages (LEG-05): every line fits, nothing is lost, each text from its own page. */
class LicencesTest {
    private val apache = """
                                 Apache License
                           Version 2.0, January 2004

      1. Definitions.

         "License" shall mean the terms and conditions for use, reproduction,
         and distribution as defined by Sections 1 through 9 of this document.

         (a) You must give any other recipients of the Work or
             Derivative Works a copy of this License; and
    """.trimIndent()

    private val list = "Librerías de terceros:\n- Kotlin: Apache 2.0.\n- Skia, en iOS: BSD."

    private fun words(text: String): List<String> = text.uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    @Test
    fun everyLineFitsAndNoWordIsLost() {
        for (width in listOf(150, 284, 400)) {
            val lines = wrapText(apache.uppercase(), width)
            for (line in lines) assertTrue(PixelFont.width(line) <= width, "«$line» in $width px")
            assertEquals(words(apache), lines.flatMap { words(it) })
        }
    }

    @Test
    fun theItemsOfAListGoEachOnItsOwnLine() {
        val lines = wrapText(list.uppercase(), 400)
        assertEquals(listOf("LIBRERÍAS DE TERCEROS:", "- KOTLIN: APACHE 2.0.", "- SKIA, EN IOS: BSD."), lines)
    }

    @Test
    fun eachTextBeginsAPageAndNoPageBeginsBlank() {
        val pages = licencePages(listOf(apache, list), width = 200, lines = 4)
        assertTrue(pages.all { it.size in 1..4 && it.first().isNotEmpty() })
        assertEquals("LIBRERÍAS DE TERCEROS:", pages.last { it.first().startsWith("LIBRER") }.first())
        assertEquals(words(apache) + words(list), pages.flatten().flatMap { words(it) })
    }

    @Test
    fun aWordLongerThanTheLineIsCut() {
        val lines = wrapText("HTTP://WWW.APACHE.ORG/LICENSES/LICENSE-2.0", 60)
        assertTrue(lines.size > 1 && lines.all { PixelFont.width(it) <= 60 })
        assertEquals("HTTP://WWW.APACHE.ORG/LICENSES/LICENSE-2.0", lines.joinToString(""))
    }
}
