package com.e.tracker.Support

//const val OSM_TRACK = 1001
//const val OSM_ADDRESS = 1999

enum class OsmMapType(val value: Int) {
    OSM_TRACK(1001),
    OSM_TRACK_DB( 1002),
    OSM_TRACK_FILE(1003),
    OSM_ADDRESS(1009)
}