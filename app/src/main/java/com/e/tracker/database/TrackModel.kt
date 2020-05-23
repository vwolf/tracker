package com.e.tracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

// An entity class defines a table and each instance of that class
// represents a row in a table

// TrackModel holds one track
@Entity(tableName = "track_table")
//,
//    foreignKeys = arrayOf(
//        ForeignKey(entity = TrackCoordDatabase::class,
//            parentColumns = arrayOf("id"),
//            childColumns = arrayOf("coords"))
//    ))

data class TrackModel (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var trackName: String = "Name?",

    @ColumnInfo(name = "description")
    var trackDescription: String = "Description?",

    @ColumnInfo(name = "type")
    var type: String = "walking",

    @ColumnInfo(name = "location")
    var location : String = "location",

    @ColumnInfo(name = "options")
    var options : String = "options",

    @ColumnInfo(name = "createdAt")
    var createdAt: Date? = DateTime().toDate(),

    @ColumnInfo(name = "updatedAt")
    var updatedAt: Date? = null,

    @ColumnInfo(name = "latitude")
    var latitude: Double? = 0.0,

    @ColumnInfo(name = "longitude")
    var longitude: Double? = 0.0,

    @ColumnInfo(name = "startCoordinates")
    var startCoordinates: String = "startCoordinates",

    @ColumnInfo(name = "staticTrack")
    var staticTrack: String = "staticTrack"
)