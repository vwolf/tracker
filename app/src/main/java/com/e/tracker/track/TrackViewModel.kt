package com.e.tracker.track

import android.app.Application
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.AndroidViewModel
import com.e.tracker.MainActivity
import com.e.tracker.database.TrackCoordDatabaseDao
import com.e.tracker.database.TrackCoordModel
import com.e.tracker.database.TrackDatabaseDao
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentNewTrackBinding
import com.e.tracker.osm.OsmActivity
import kotlinx.coroutines.*


/**
 *
 * @param database
 * @param application AndroidViewModel is same as ViewModel but makes the
 * application context available as a property
 */
class TrackViewModel (
    val database: TrackDatabaseDao,
    val coordSource: TrackCoordDatabaseDao,
    application: Application) : AndroidViewModel(application) {


    private var viewModelJob = Job()

    // scope determines what thread the coroutine will run
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private lateinit var binding: FragmentNewTrackBinding

    var tracks = database.getAllTracks()

    var mergedTracks = mutableListOf<TrackModel>()


    /**
     * Insert new track into table on .IO
     * 1. insert new track in TrackDatabase
     * 2. insert start coordinates in TrackCoordDatabase
     *
     * @param track
     */
    private suspend fun insert(track: TrackModel) {
        var trackCoordModel = TrackCoordModel()
        trackCoordModel.latitude = track.latitude
        trackCoordModel.longitude = track.longitude
        trackCoordModel.trackPosition = 1

        withContext(Dispatchers.IO) {
            val trackId = database.insert(track)

            trackCoordModel.track = trackId
            coordSource.insert(trackCoordModel)
        }
    }

    /**
     * Read track from table
     */
    private suspend fun get(id: Long) : TrackModel? {
        withContext(Dispatchers.IO) {
            val aModel = database.get(id)
            aModel
        }

        return TrackModel()
    }


    private suspend fun update() {}


    fun insertNewTrack(newTrack: TrackModel) {
        uiScope.launch { insert(newTrack) }
    }


    fun addData( data: List<TrackModel>) {
        mergedTracks.addAll(data)
    }


    /**
     * onClick Event from RecyclerView
     *
     * @param track
     */
    fun onTrackItemClicked(track: TrackModel) {
        println("onTrackItemClicked id: ${track.trackName}")

//        val context = getApplication<Application>().applicationContext
//        val intent = Intent(context,  OsmActivity::class.java)
//        startActivityForResult(OsmActivity(), intent, 1, null)
    }


    fun onTrackEditIconClicked(track: TrackModel) {
        println("onTrackEditIconClicked ${track.trackName}")
    }
}