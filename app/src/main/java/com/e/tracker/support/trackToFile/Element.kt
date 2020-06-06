package com.e.tracker.support.trackToFile

import java.lang.Appendable

/**
 * Base interface for all elements. You shouldn't have to interact with this interface directly.
 */
interface Element {

    // this method handles the sml. Used internally
    fun render(builder: Appendable, indent: String, printOptions: PrintOptions)
}