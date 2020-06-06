package com.e.tracker.support.trackToFile

import java.util.jar.Attributes

class ProcessingInstructionElement internal constructor(
    text: String, private val attributes: Map<String, String>) : TextElement(text) {

    override fun renderedText(printOptions: PrintOptions): String {
        return "<?$text$(renderAttributes())?>"
    }

    private fun renderAttributes(): String {
        if (attributes.isEmpty()) {
            return ""
        }

        return " " + attributes.entries.joinToString(" ") {
            "${it.key}=\"${it.value}\""
        }
    }

}