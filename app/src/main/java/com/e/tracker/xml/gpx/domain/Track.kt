package com.e.tracker.xml.gpx.domain


import java.util.*

class Track private constructor(builder: Builder) {
    val trackName: String?
    private val mTrackSegments: List<TrackSegment>
    val trackDesc: String?
    val trackCmt: String?
    val trackSrc: String?
    val trackNumber: Int?
    private val mTrackLink: Link?
    val trackType: String?

    val trackLink: Link?
        get() = mTrackLink

    val trackSegments: List<Any>
        get() = mTrackSegments

    class Builder {
        var mTrackName: String? = null
        var mTrackSegments: List<TrackSegment>? = null
        var mTrackDesc: String? = null
        var mTrackCmt: String? = null
        var mTrackSrc: String? = null
        var mTrackNumber: Int? = null
        var mTrackLink: Link? = null
        var mTrackType: String? = null
        fun setTrackName(trackName: String?): Builder {
            mTrackName = trackName
            return this
        }

        fun setTrackDesc(trackDesc: String?): Builder {
            mTrackDesc = trackDesc
            return this
        }

        fun setTrackSegments(trackSegments: List<TrackSegment>?): Builder {
            mTrackSegments = trackSegments
            return this
        }

        fun setTrackCmt(trackCmt: String?): Builder {
            mTrackCmt = trackCmt
            return this
        }

        fun setTrackSrc(trackSrc: String?): Builder {
            mTrackSrc = trackSrc
            return this
        }

        fun setTrackNumber(trackNumber: Int?): Builder {
            mTrackNumber = trackNumber
            return this
        }

        fun setTrackLink(link: Link?): Builder {
            mTrackLink = link
            return this
        }

        fun setTrackType(type: String?): Builder {
            mTrackType = type
            return this
        }

        fun build(): Track {
            return Track(this)
        }
    }

    init {
        trackName = builder.mTrackName
        trackDesc = builder.mTrackDesc
        trackCmt = builder.mTrackCmt
        trackSrc = builder.mTrackSrc
        trackNumber = builder.mTrackNumber
        mTrackSegments = Collections.unmodifiableList<TrackSegment>(
            ArrayList<TrackSegment>(builder.mTrackSegments!!)
        )
        mTrackLink = builder.mTrackLink
        trackType = builder.mTrackType
    }
}