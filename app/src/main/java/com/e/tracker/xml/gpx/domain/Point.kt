package com.e.tracker.xml.gpx.domain


import org.joda.time.DateTime

/**
 * A point containing a location, time and name.
 */
abstract class Point internal constructor(builder: Builder) {
    /**
     * @return the latitude in degrees
     */
    val latitude: Double?
    /**
     * @return the longitude in degrees
     */
    val longitude: Double?
    /**
     * @return the elevation in meters
     */
    val elevation: Double?
    val mTime: DateTime?
    //val mTime: LocalDateTime?
    //val mTime: String?

    /**
     * @return the point name
     */
    val name: String?
    /**
     * @return the description
     */
    val desc: String?
    /**
     * @return the type (category)
     */
    val type: String?

//    val time: DateTime?
//        get() = mTime

    abstract class Builder {
        var mLatitude: Double? = null
        var mLongitude: Double? = null
        var mElevation: Double? = null
        //var mTime: LocalDateTime? = null
        var mTime: DateTime? = null
        var mName: String? = null
        var mDesc: String? = null
        var mType: String? = null

        fun setLatitude(latitude: Double?): Builder {
            mLatitude = latitude
            return this
        }

        fun setLongitude(longitude: Double?): Builder {
            mLongitude = longitude
            return this
        }

        fun setElevation(elevation: Double?): Builder {
            mElevation = elevation
            return this
        }

//        fun setTime(time: LocalDateTime?): Builder {
//            mTime = time
//            return this
//        }

        fun setTime(time: DateTime?): Builder {
            mTime = time
            return this
        }

        fun setName(mame: String?): Builder {
            mName = mame
            return this
        }

        fun setDesc(desc: String?): Builder {
            mDesc = desc
            return this
        }

        fun setType(type: String?): Builder {
            mType = type
            return this
        }

        abstract fun build(): Point
    }

    init {
        latitude = builder.mLatitude
        longitude = builder.mLongitude
        elevation = builder.mElevation
        mTime = builder.mTime
        name = builder.mName
        desc = builder.mDesc
        type = builder.mType
    }
}