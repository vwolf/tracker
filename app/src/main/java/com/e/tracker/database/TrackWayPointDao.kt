package com.e.tracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao

interface TrackWayPointDao {

    @Insert
    fun insert(wayPoint: TrackWayPointModel) : Long

    @Update
    fun update(wayPoint: TrackWayPointModel) : Int

    @Delete
    fun delete(wayPoint: TrackWayPointModel) : Int

    @Query("SELECT * FROM trackWaypoint_table ORDER BY id DESC")
    fun getWayPoints(): LiveData<List<TrackWayPointModel>>

    @Query("SELECT * FROM trackWaypoint_table WHERE trackId == :id ORDER BY pointId ASC")
    fun getWayPointsForId(id: Long) : List<TrackWayPointModel>

}