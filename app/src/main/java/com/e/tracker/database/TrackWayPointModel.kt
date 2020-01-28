package com.e.tracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.joda.time.DateTime
import java.util.*

@Entity(tableName = "trackWaypoint_table")

data class TrackWayPointModel (

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "createdAt")
    var createdAt: Date? = DateTime().toDate(),

    @ColumnInfo(name = "name")
    var wayPointName: String,

    @ColumnInfo(name = "description")
    var description: String?,

    @ColumnInfo(name = "type")
    var type: String = "info",

    @ColumnInfo(name = "trackId")
    var trackId: Long,

    @ColumnInfo(name = "pointId")
    var pointId: Long,

    //@TypeConverter(Converters.class)
    @ColumnInfo(name = "waypointImages")
    var wayPointImages:  List<String>
)