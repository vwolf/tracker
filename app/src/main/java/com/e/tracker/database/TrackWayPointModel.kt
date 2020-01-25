package com.e.tracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trackWaypoint_table")

data class TrackWayPointModel (

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var wayPointName: String,

    @ColumnInfo(name = "type")
    var type: String = "info",

    @ColumnInfo(name = "trackId")
    var trackId: Long,

    @ColumnInfo(name = "pointId")
    var pointId: Long
)