package com.e.tracker.support.trackToFile

import java.lang.Appendable

class Comment internal constructor(val text: String): Element {

    override fun render(builder: Appendable, indent: String, printOptions: PrintOptions) {
        val lineEnding = getLineEnding(printOptions)

        builder.append("$indent<!-- ${text.replace("--", "&#45;&#45;")}")
    }
}