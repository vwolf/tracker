package com.e.tracker.Support

//const val OSM_TRACK = 1001
//const val OSM_ADDRESS = 1999

enum class OsmMapType(val value: Int) {
    OSM_TRACK(1001),
    OSM_TRACK_DB( 1002),
    OSM_TRACK_FILE(1003),
    OSM_ADDRESS(1009)
}

enum class WayPointMediaIntents(val value: Int) {
    SELECT_IMAGE(1001),
    CAMERA_IMAGE(1002),
    SELECT_VIDEO(1003),
    CAMERA_VIDEO(1004),
    SELECT_AUDIO(1005),
    RECORD_AUDIO(1006)
}