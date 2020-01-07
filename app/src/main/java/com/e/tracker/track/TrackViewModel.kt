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
import org.json.JSONObject


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
    private suspend fun getTrack(id: Long) : TrackModel? {
        return withContext(Dispatchers.IO) {
            val aModel = database.get(id)
            //aModel?.source = "db"
            aModel
        }
    }

    /**
     * Update a track
     * When updating a track, update tracks start coords
     *
     * @param track
     */
    private suspend fun update(track: TrackModel) {
      withContext(Dispatchers.IO) {
          val result = database.update(track)
          if (result > 0L) {
              var startCoord = coordSource.getCoordForTrackAtPosition(track.id, 1)
              if (startCoord is TrackCoordModel) {
                  startCoord.latitude = track.latitude
                  startCoord.longitude = track.longitude
              }
          }
      }
    }

    private suspend fun deleteTrack(id: Long) {
        withContext(Dispatchers.IO) {
            val track = mergedTracks.first { it.id == id }
            val result = database.delete(track)
            mergedTracks.remove(track)

            if (result is Unit) {
                // remove coord belonging to deleted track
                val trackCoords = coordSource.getCoordsForId(id)
                if (trackCoords.isNotEmpty()) {
                    for (c in trackCoords) {
                        coordSource.delete(c)
                    }
                }
            }
        }
    }


    fun insertNewTrack(newTrack: TrackModel) {
        uiScope.launch { insert(newTrack) }
    }


    fun updateTrack(track: TrackModel) {
        uiScope.launch { update(track) }
    }


    suspend fun getTrackWithId(id: Long) : TrackModel? {
        return withContext(Dispatchers.IO) {
            val aModel = database.get(id)
            //aModel?.source = "db"
            aModel
        }
    }

    fun deleteTrackWithId(id: Long) {
        uiScope.launch { deleteTrack(id) }
    }


    fun addData( data: List<TrackModel>) {
        for (d in data) {
            if ( !mergedTracks.contains(d) ) {
                mergedTracks.add(d)
            }
        }
        //mergedTracks.addAll(data)
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


    /**
     * Touch on edit icon on track item list view
     * Display choice depending on track type (file or db)
     *
     * @param track
     */
    fun onTrackEditIconClicked(track: TrackModel) {
        println("onTrackEditIconClicked ${track.trackName}")
//        if (track.id > 0) {
//            val dialogFragment = TrackEditDialogFragment("Select Location", "db")
//            //dialogFragment.show(this.fragmentManager!!, "myTag")
//
//        } else {
//
//        }

    }
}