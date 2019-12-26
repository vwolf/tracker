package com.e.tracker.xml.gpx.domain

import java.util.*

class TrackSegment private constructor(builder: Builder) {
    val trackPoints: List<TrackPoint>

    class Builder {

        var mTrackPoints: List<TrackPoint>? = null

        fun setTrackPoints(trackPoints: List<TrackPoint>?): Builder {
            mTrackPoints = trackPoints
            return this
        }

        fun build(): TrackSegment {
            return TrackSegment(this)
        }
    }

    init {
        trackPoints =
            Collections.unmodifiableList(ArrayList(builder.mTrackPoints!!))
    }
}