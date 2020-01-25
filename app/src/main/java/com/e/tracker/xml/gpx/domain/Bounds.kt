package com.e.tracker.xml.gpx.domain



class Bounds private constructor(builder: Builder) {
    private val minLat: Double?
    private val minLon: Double?
    private val maxLat: Double?
    private val maxLon: Double?

    class Builder {
        var mMinLat: Double? = null
        var mMinLon: Double? = null
        var mMaxLat: Double? = null
        var mMaxLon: Double? = null

        fun setMinLat(minLat: Double?): Builder {
            mMinLat = minLat
            return this
        }

        fun setMinLon(minLon: Double?): Builder {
            mMinLon = minLon
            return this
        }

        fun setMaxLat(maxLat: Double?): Builder {
            mMaxLat = maxLat
            return this
        }

        fun setMaxLon(maxLon: Double?): Builder {
            mMaxLon = maxLon
            return this
        }

        fun build(): Bounds {
            return Bounds(this)
        }
    }

    init {
        minLat = builder.mMinLat
        minLon = builder.mMinLon
        maxLat = builder.mMaxLat
        maxLon = builder.mMaxLon
    }
}