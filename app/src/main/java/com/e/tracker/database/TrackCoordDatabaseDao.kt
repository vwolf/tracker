package com.e.tracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TrackCoordDatabaseDao {

    @Insert
    fun insert(trackCoord: TrackCoordModel) : Long

    @Insert
    fun insertCoords(trackCoords: List<TrackCoordModel>) : List<Long>

    @Update
    fun update(trackCoord: TrackCoordModel) : Int

    @Delete
    fun delete(trackCoord: TrackCoordModel) : Int

    @Query("SELECT * FROM trackcoord_table ORDER BY id DESC")
    fun getAllCoords(): LiveData<List<TrackCoordModel>>

    @Query("SELECT * FROM trackcoord_table WHERE track == :id ORDER BY trackPosition ASC")
    fun getCoordsForId(id: Long) : List<TrackCoordModel>

    @Update
    fun updateCoords(trackCoords: List<TrackCoordModel>)

    @Query("UPDATE trackcoord_table SET trackPosition = :newTrackPosition WHERE track == :track AND trackPosition == :trackPosition ")
    fun updateCoordsTrackPosition(trackPosition: Int, newTrackPosition: Int, track: Long)

    @Query("Update trackcoord_table SET trackPosition = :newTrackPosition WHERE id == :id")
    fun updatePositionOfCoord(newTrackPosition: Int, id: Long) : Int

    @Query("SELECT * FROM trackcoord_table WHERE track == :track AND trackPosition == :trackPosition")
    fun getCoordForTrackAtPosition(track: Long, trackPosition: Int) : TrackCoordModel
}