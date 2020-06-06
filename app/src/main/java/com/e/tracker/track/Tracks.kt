package com.e.tracker.track

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.EnvironmentCompat
import com.e.tracker.database.TrackModel
import com.e.tracker.xml.gpx.domain.TrackSegment
import com.google.gson.annotations.Expose
import java.io.File

/**
 * Collect's all tracks, db and files
 *
 */
class Tracks() : Parcelable {

    @Expose
    var fileTracks = mutableListOf<Track>()

    constructor(parcel: Parcel) : this (
//        fileTracks = parcel.readArray()
//        in.readTypedList(this.fileTracks, Track.CREATOR)
    )

    fun addFileTrack(path: String, trackModel: TrackModel) {
        var newTrack = Track(TrackSourceType.FILE, trackModel)
        newTrack.filePath = path

        fileTracks.add(newTrack)
    }


    fun addDbTrack(trackModel: TrackModel) {
        var newTrack = Track(TrackSourceType.DATABASE, trackModel)
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }
    companion object {
        @JvmField val CREATOR = object : Parcelable.Creator<Tracks> {
            override fun createFromParcel(parcel: Parcel): Tracks = Tracks(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Tracks>(size)
        }
   }
}

/**
 *
 */
class Track(val trackSourceType: TrackSourceType, val trackModel: TrackModel) {

    // file track specific
    var filePath: String? = null

}


