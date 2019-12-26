package com.e.tracker.xml.gpx.domain


import java.util.*

class Route private constructor(builder: Builder) {
    private val mRoutePoints: List<RoutePoint>
    val routeName: String?
    val routeDesc: String?
    val routeCmt: String?
    val routeSrc: String?
    val routeNumber: Int?
    private val mRouteLink: Link?
    val routeType: String?
    val routePoints: List<Any>
        get() = mRoutePoints

    val routeLink: Link?
        get() = mRouteLink

    class Builder {
        var mRoutePoints: List<RoutePoint>? = null
        var mRouteName: String? = null
        var mRouteDesc: String? = null
        var mRouteCmt: String? = null
        var mRouteSrc: String? = null
        var mRouteNumber: Int? = null
        var mRouteLink: Link? = null
        var mRouteType: String? = null
        fun setRoutePoints(routePoints: List<RoutePoint>?): Builder {
            mRoutePoints = routePoints
            return this
        }

        fun setRouteName(routeName: String?): Builder {
            mRouteName = routeName
            return this
        }

        fun setRouteDesc(routeDesc: String?): Builder {
            mRouteDesc = routeDesc
            return this
        }

        fun setRouteCmt(routeCmt: String?): Builder {
            mRouteCmt = routeCmt
            return this
        }

        fun setRouteSrc(routeSrc: String?): Builder {
            mRouteSrc = routeSrc
            return this
        }

        fun setRouteNumber(routeNumber: Int?): Builder {
            mRouteNumber = routeNumber
            return this
        }

        fun setRouteLink(routeLink: Link?): Builder {
            mRouteLink = routeLink
            return this
        }

        fun setRouteType(routeType: String?): Builder {
            mRouteType = routeType
            return this
        }

        fun build(): Route {
            return Route(this)
        }
    }

    init {
        mRoutePoints = Collections.unmodifiableList<RoutePoint>(
            ArrayList<RoutePoint>(builder.mRoutePoints!!)
        )
        routeName = builder.mRouteName
        routeDesc = builder.mRouteDesc
        routeCmt = builder.mRouteCmt
        routeSrc = builder.mRouteSrc
        routeNumber = builder.mRouteNumber
        mRouteLink = builder.mRouteLink
        routeType = builder.mRouteType
    }
}