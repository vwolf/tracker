package com.e.tracker.xml.gpx.domain

class Email private constructor(builder: Builder) {
    val id: String?
    val domain: String?

    class Builder {
        var mId: String? = null
        var mDomain: String? = null

        fun setId(id: String?): Builder {
            mId = id
            return this
        }

        fun setDomain(domain: String?): Builder {
            mDomain = domain
            return this
        }

        fun build(): Email {
            return Email(this)
        }
    }

    init {
        id = builder.mId
        domain = builder.mDomain
    }
}