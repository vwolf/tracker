package com.e.tracker.xml.gpx.domain

class Copyright private constructor(builder: Builder) {
    private val author: String?
    private val year: Int?
    private val license: String?

    class Builder {
        var mAuthor: String? = null
        var mYear: Int? = null
        var mLicense: String? = null

        fun setAuthor(author: String?): Builder {
            mAuthor = author
            return this
        }

        fun setYear(year: Int?): Builder {
            mYear = year
            return this
        }

        fun setLicense(license: String?): Builder {
            mLicense = license
            return this
        }

        fun build(): Copyright {
            return Copyright(this)
        }
    }

    init {
        author = builder.mAuthor
        year = builder.mYear
        license = builder.mLicense
    }
}