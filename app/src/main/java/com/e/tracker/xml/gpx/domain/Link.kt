package com.e.tracker.xml.gpx.domain


/**
 * Created by Stu Stirling on 04/10/2017.
 */
class Link private constructor(builder: Builder) {
    val href: String?
    val text: String?
    val type: String?

    class Builder {
        var mLinkHref: String? = null
        var mLinkText: String? = null
        var mLinkType: String? = null

        fun setLinkHref(linkHref: String?): Builder {
            mLinkHref = linkHref
            return this
        }

        fun setLinkText(linkText: String?): Builder {
            mLinkText = linkText
            return this
        }

        fun setLinkType(linkType: String?): Builder {
            mLinkType = linkType
            return this
        }

        fun build(): Link {
            return Link(this)
        }
    }

    init {
        href = builder.mLinkHref
        text = builder.mLinkText
        type = builder.mLinkType
    }
}