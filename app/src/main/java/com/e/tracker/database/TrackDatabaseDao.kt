package com.e.tracker.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrackDatabaseDao {

    @Insert
    fun insert(track: TrackModel) : Long

    @Update
    fun update(track: TrackModel)

    @Query("SELECT * FROM track_table WHERE id = :key")
    fun get(key: Long): TrackModel?

    @Query("SELECT * FROM track_table ORDER BY id DESC")
    fun getAllTracks(): LiveData<List<TrackModel>>

    @Query("SELECT * FROM track_table ORDER BY id DESC LIMIT 1")
    fun getLast() : TrackModel?
}