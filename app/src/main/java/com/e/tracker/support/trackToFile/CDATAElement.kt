package com.e.tracker.support.trackToFile

import com.e.tracker.support.trackToFile.PrintOptions
import com.e.tracker.support.trackToFile.TextElement

class CDATAElement internal constructor(text: String): TextElement(text) {

    override fun renderedText(printOptions: PrintOptions): String? {
        fun String.escapeCData() : String {
            val cdataEnd = "]]"
            val cdataStart = "<![CDATA["

            return this
                .replace(cdataEnd, "]]$cdataEnd$cdataStart>")
        }

        return "<![CDATA[${text.escapeCData()}]]>"
    }
}