package com.amiri.note.util

/** One checklist row inside a note. */
data class ChecklistItem(val text: String, val done: Boolean)

/**
 * Minimal, dependency-free encoder for checklist items.
 * Format: one item per line as "1|text" or "0|text". Newlines in text are
 * escaped as \n, backslashes as \\. Robust against malformed input.
 */
object ChecklistCodec {

    fun encode(items: List<ChecklistItem>): String =
        items.joinToString("\n") { item ->
            val flag = if (item.done) "1" else "0"
            "$flag|" + escape(item.text)
        }

    fun decode(raw: String): List<ChecklistItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            if (line.isEmpty()) return@mapNotNull null
            val sep = line.indexOf('|')
            if (sep < 0) return@mapNotNull ChecklistItem(unescape(line), false)
            val flag = line.substring(0, sep)
            val text = unescape(line.substring(sep + 1))
            ChecklistItem(text, flag == "1")
        }
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\n", "\\n")

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2; continue }
                    '\\' -> { sb.append('\\'); i += 2; continue }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
