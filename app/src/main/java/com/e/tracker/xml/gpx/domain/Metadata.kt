package com.e.tracker.xml.gpx.domain


import org.joda.time.DateTime

import java.time.LocalDateTime

class Metadata private constructor(builder: Builder) {
    val name: String?
    val desc: String?

    private val mAuthor: Author?
    private val mCopyright: Copyright?
    private val mLink: Link?

    private val mTime: DateTime?
//    private val mTime: LocalDateTime
//    val mTime: String?

    val keywords: String?
    private val mBounds: Bounds?
    val extensions: String?

    val author: Author?
        get() = mAuthor

    val copyright: Copyright?
        get() = mCopyright

    val link: Link?
        get() = mLink

    val time: DateTime?
        get() = mTime

    /*val time: LocalDateTime?
        get() = mTime*/

//    val time: String?
//        get() = mTime

    val bounds: Bounds?
        get() = mBounds

    class Builder {
        var mName: String? = null
        var mDesc: String? = null
        var mAuthor: Author? = null
        var mCopyright: Copyright? = null
        var mLink: Link? = null
        //        var mTime: DateTime? = null
//        var mTime: LocalDateTime = null
        var mTime: DateTime? = null
        var mKeywords: String? = null
        var mBounds: Bounds? = null
        val mExtensions: String? = null

        fun setName(name: String?): Builder {
            mName = name
            return this
        }

        fun setDesc(desc: String?): Builder {
            mDesc = desc
            return this
        }

        fun setAuthor(author: Author?): Builder {
            mAuthor = author
            return this
        }

        fun setCopyright(copyright: Copyright?): Builder {
            mCopyright = copyright
            return this
        }

        fun setLink(link: Link?): Builder {
            mLink = link
            return this
        }

        fun setTime(time: DateTime?): Builder {
            mTime = time
            return this
        }

//        fun setTime(time: LocalDateTime): Builder {
//            mTime = time
//            return this
//        }

//        fun setTime(time: String) : Builder {
//            mTime = time
//            return this
//        }

        fun setKeywords(keywords: String?): Builder {
            mKeywords = keywords
            return this
        }

        fun setBounds(bounds: Bounds?): Builder {
            mBounds = bounds
            return this
        }

        fun build(): Metadata {
            return Metadata(this)
        }
    }

    init {
        name = builder.mName
        desc = builder.mDesc
        mAuthor = builder.mAuthor
        mCopyright = builder.mCopyright
        mLink = builder.mLink
        mTime = builder.mTime
        keywords = builder.mKeywords
        mBounds = builder.mBounds
        extensions = builder.mExtensions
    }
}