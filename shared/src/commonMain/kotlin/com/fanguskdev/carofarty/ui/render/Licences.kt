package com.fanguskdev.carofarty.ui.render

/**
 * The whole texts of the licences (LEG-05) in pages of [lines] lines at most [width] game pixels wide, each text from a
 * page of its own: in capitals, as the pixel font draws them, and with its paragraphs joined and wrapped again to the
 * width, since their lines were broken for a wider screen. No page starts with a blank line.
 */
internal fun licencePages(texts: List<String>, width: Int, lines: Int): List<List<String>> {
    if (lines <= 0) return emptyList()
    val pages = mutableListOf<List<String>>()
    for (text in texts) {
        var page = mutableListOf<String>()
        for (line in wrapText(text.uppercase(), width)) {
            if (page.isEmpty() && line.isEmpty()) continue
            page += line
            if (page.size == lines) {
                pages += page
                page = mutableListOf()
            }
        }
        if (page.isNotEmpty()) pages += page
    }
    return pages
}

/**
 * [text] in lines at most [width] game pixels wide: a blank line ends a paragraph, and so does a line that starts an
 * item of a list («-», «*», «1.» or «(a)»); the blank lines between paragraphs stay, and a word too long for a line is
 * cut.
 */
internal fun wrapText(text: String, width: Int): List<String> {
    val out = mutableListOf<String>()
    for ((paragraph, apart) in paragraphs(text)) {
        if (apart && out.isNotEmpty()) out += ""
        var line = ""
        for (word in paragraph.split(' ')) {
            if (word.isEmpty()) continue
            val longer = if (line.isEmpty()) word else "$line $word"
            if (PixelFont.width(longer) <= width) {
                line = longer
                continue
            }
            if (line.isNotEmpty()) out += line
            var rest = word
            while (PixelFont.width(rest) > width && rest.length > 1) {
                var cut = rest.length - 1
                while (cut > 1 && PixelFont.width(rest.substring(0, cut)) > width) cut--
                out += rest.substring(0, cut)
                rest = rest.substring(cut)
            }
            line = rest
        }
        if (line.isNotEmpty()) out += line
    }
    return out
}

/** The paragraphs of [text], each in one line, and whether a blank line came before it. */
private fun paragraphs(text: String): List<Pair<String, Boolean>> {
    val paragraphs = mutableListOf<Pair<String, Boolean>>()
    var current = StringBuilder()
    var apart = false
    var blank = false
    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.isEmpty() || startsItem(line)) {
            if (current.isNotEmpty()) paragraphs += current.toString() to apart
            current = StringBuilder()
        }
        if (line.isEmpty()) {
            blank = true
            continue
        }
        if (current.isEmpty()) {
            apart = blank
            blank = false
        } else {
            current.append(' ')
        }
        current.append(line)
    }
    if (current.isNotEmpty()) paragraphs += current.toString() to apart
    return paragraphs
}

private val ITEM = Regex("""^([-*]\s|\d+\.\s|\([a-z0-9]+\)\s)""", RegexOption.IGNORE_CASE)

private fun startsItem(line: String): Boolean = ITEM.containsMatchIn(line)
