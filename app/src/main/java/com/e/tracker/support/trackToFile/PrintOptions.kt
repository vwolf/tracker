package com.e.tracker.support.trackToFile

class PrintOptions(

    /// Whether to print newline and taps while rendering the document
    val pretty: Boolean = true,

    /**
     * Whether to print a single text element on the same line.
     *
     * ```
     * <element>
     *     text value
     * </element>
     * ```
     *
     * vs
     *
     * ```
     * <element>text value</element>
     * ```
     */
    val singleLineTextElements: Boolean = true,

    /**
     * Whether to use "self closing" tags for empty elements.
     *
     * ```
     * <element></element>
     * ```
     *
     * vs
     *
     * ```
     * <element />
     * ```
     */
    val useSelfClosingTags: Boolean = true) {

    internal var xmlVersion: XmlVersion = XmlVersion.V10
}