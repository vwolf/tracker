package com.e.tracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.text.FieldPosition
import java.util.*

@Entity(tableName = "trackcoord_table"
//    foreignKeys = arrayOf(
//        ForeignKey( entity = TrackModel::class,
//            parentColumns =  arrayOf("id"),
//            childColumns = arrayOf("track"))
//    )
)

data class TrackCoordModel (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "track")
    var track: Long = 0L,

    @ColumnInfo(name = "trackPosition")
    var trackPosition : Int = 0,

    @ColumnInfo(name = "lat")
    var latitude: Double = 0.0,

    @ColumnInfo(name = "lon")
    var longitude: Double = 0.0,

    var altitude: Double? = null,
    var accuracy: Double? = null,
    var heading: Double? = null,
    var speed: Double? = null,
    var speedAccuracy: Double? = null,

    var createdAt: Date? = null,
    var updateAt: Date? = null
)