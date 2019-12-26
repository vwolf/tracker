package com.e.tracker.xml.gpx.domain

import java.util.*

class Gpx private constructor(builder: Builder) {
    val version: String?
    val creator: String?
    val metadata: Metadata?

    private val mWayPoints: List<WayPoint>
    private val mRoutes: List<Route>
    private val mTracks: List<Track>

    val wayPoints: List<Any>
        get() = mWayPoints

    val routes: List<Any>
        get() = mRoutes

    val tracks: List<Track>
        get() = mTracks

    class Builder {
        var mWayPoints: List<WayPoint>? = null
        var mRoutes: List<Route>? = null
        var mTracks: List<Track>? = null
        var mVersion: String? = null
        var mCreator: String? = null
        var mMetadata: Metadata? = null

        fun setTracks(tracks: List<Track>): Builder {
            mTracks = tracks
            return this
        }

        fun setWayPoints(wayPoints: List<WayPoint>?): Builder {
            mWayPoints = wayPoints
            return this
        }

        fun setRoutes(routes: List<Route>?): Builder {
            mRoutes = routes
            return this
        }

        fun setVersion(version: String?): Builder {
            mVersion = version
            return this
        }

        fun setCreator(creator: String?): Builder {
            mCreator = creator
            return this
        }

        fun setMetadata(mMetadata: Metadata?): Builder {
            this.mMetadata = mMetadata
            return this
        }

        fun build(): Gpx {
            return Gpx(this)
        }
    }

    init {
        version = builder.mVersion
        creator = builder.mCreator
        metadata = builder.mMetadata
        mWayPoints =
            Collections.unmodifiableList<WayPoint>(ArrayList<WayPoint>(builder.mWayPoints!!))
        mRoutes =
            Collections.unmodifiableList<Route>(ArrayList<Route>(builder.mRoutes!!))
        mTracks =
            Collections.unmodifiableList<Track>(ArrayList<Track>(builder.mTracks!!))
    }
}