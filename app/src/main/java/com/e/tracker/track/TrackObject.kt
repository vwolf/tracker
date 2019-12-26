package com.e.tracker.track

import android.content.Context
import com.e.tracker.MainActivity
import com.e.tracker.database.*
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import kotlin.properties.Delegates

/**
 * Service class for track (Polyline)
 * Read and write DB
 *
 */
class TrackObject {


    private var osmActivityJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + osmActivityJob)

    lateinit var coordsSource: TrackCoordDatabaseDao


    // track details
    var type: TrackSourceType = TrackSourceType.DATABASE
    var id: Long = 0
    var trackName: String? = null
    var trackDescription: String = ""
    var latitude: Double by Delegates.observable( 0.0) { _, old, new ->
        println("OBSERVABLE UPDATE old: $old, new: $new")
    }
    var longitude: Double = 0.0

    var trackSourceType : TrackSourceType = TrackSourceType.DATABASE
    var coords = mutableListOf<TrackCoordModel>()
        set(value) = updateTrack()

    var coordsGpx = arrayListOf<GeoPoint>()

    var name: String by Delegates.observable("name") { _, oldValue, newValue ->
        println("oldName: $oldValue, newName: $newValue")
    }



    private fun updateTrack() {}

    /**
     * Add coord to end of path or at path point index
     *
     * @param geoPoint coordinates for path point
     * @param position position of coord in path,
     */
    fun addCoord(geoPoint: GeoPoint, position: Int? = coords.size) : Boolean{
        var trackCoordModel = TrackCoordModel()
        trackCoordModel.latitude = geoPoint.latitude
        trackCoordModel.longitude = geoPoint.longitude
        trackCoordModel.track = id

        position ?: coords.size
        trackCoordModel.trackPosition =  position!!


        uiScope.launch {

            withContext(Dispatchers.IO) {
                val result = coordsSource.insert(trackCoordModel)
                println("insert result: $result")
                if (result != -1L) {
                    if (result > coords.size) {
                        coords.add(trackCoordModel)
                        coordsGpx.add(geoPoint)

                    } else {
                        coords.add(result.toInt(), trackCoordModel)
                        coordsGpx.add(result.toInt(), geoPoint)
                    }

                    if (result < coords.size) {
                        println("add Coord result: $result")
                    }
                }
            }

//            val result = insertCoord(trackCoordModel)
//            if (result != -1L) {
//                // added at end or
//                coords.add(result.toInt(), trackCoordModel)
//
//                if (result < coords.size) {
//                    uiScope.launch { updateCoords(1, result.toInt(), coords.size) }
//                }
//            }
        }

        // if coords not added to end of path then update coords follwing of new coord
        return true
    }

    /**
     *
     * @param trackCoordModel
     * @return Long id of insert TrackCoord
     */
    private suspend fun insertCoord(trackCoordModel: TrackCoordModel)  {

        withContext(Dispatchers.IO) {
            val result = coordsSource.insert(trackCoordModel)
            println("insert result: $result")
            result
        }
        //return -1L
    }



    /**
     * Update trackPosition in TrackCoordDatabase
     * Increase trackPosition with value starting at startIdx to endIdx
     * startIdx is TrackCoordModel.trackPosition
     * endIdx is null then set to size of coords in track
     *
     *
     */
    private suspend fun updateCoords(value: Int, startIdx: Int, endIdx: Int) {

        withContext(Dispatchers.IO) {
            for ( i in startIdx..endIdx) {
                coordsSource.updateCoordsTrackPosition(startIdx, i + 1, id)
            }
        }
    }


    fun deleteCoord(id: Long) {
        val trackCoordModel = coords.first { it.id == id }
        if( trackCoordModel is TrackCoordModel ) {
            uiScope.launch { deleteCoordInDB(trackCoordModel) }
        }
    }

    fun deleteCoord(trackModel: TrackModel) {

    }

    /**
     * Find TrackCoordModel with gpoPoint coordinates and remove from DB
     */
    fun deleteCoord(geoPoint: GeoPoint) {
        val coordIndex = coordsGpx.indexOfFirst { it == geoPoint }
        if ( coordIndex >= 0 ) {
            uiScope.launch { deleteCoordInDB(coords[coordIndex]) }

            coords.removeAt(coordIndex)
            coordsGpx.removeAt(coordIndex)
        }
    }


    private suspend fun deleteCoordInDB(trackCoordModel: TrackCoordModel) {
        withContext(Dispatchers.IO) {
            coordsSource.delete(trackCoordModel)
        }
    }

//    private suspend fun insert(track: TrackModel) {
//        var trackCoordModel = TrackCoordModel()
//        trackCoordModel.latitude = track.latitude
//        trackCoordModel.longitude = track.longitude
//
//        withContext(Dispatchers.IO) {
//            val trackId = database.insert(track)
//
//            trackCoordModel.track = trackId
//            coordSource.insert(trackCoordModel)
//        }
//    }
}


enum class TrackSourceType {
    FILE,
    DATABASE,
    NEW;
}
