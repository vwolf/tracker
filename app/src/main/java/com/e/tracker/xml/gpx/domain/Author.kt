package com.e.tracker.xml.gpx.domain


class Author private constructor(builder: Builder) {
    val name: String?
    private val mEmail: Email?
    private val mLink: Link?

    val email: Email?
        get() = mEmail

    val link: Link?
        get() = mLink

    class Builder {
        var mName: String? = null
        var mEmail: Email? = null
        var mLink: Link? = null

        fun setName(name: String?): Builder {
            mName = name
            return this
        }

        fun setEmail(email: Email?): Builder {
            mEmail = email
            return this
        }

        fun setLink(link: Link?): Builder {
            mLink = link
            return this
        }

        fun build(): Author {
            return Author(this)
        }
    }

    init {
        name = builder.mName
        mEmail = builder.mEmail
        mLink = builder.mLink
    }
}