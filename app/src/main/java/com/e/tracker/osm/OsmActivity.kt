package com.e.tracker.osm

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.e.tracker.R
import com.e.tracker.Support.OsmMapType
import com.e.tracker.Support.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.e.tracker.database.*
import com.e.tracker.track.TrackObject
import com.e.tracker.track.TrackSourceType
import com.e.tracker.xml.gpx.GPXParser
import com.e.tracker.xml.gpx.domain.TrackSegment
import com.e.tracker.xml.gpx.domain.WayPoint
import com.e.tracker.Support.Permissions
import com.e.tracker.osm.dialogs.OsmBottomSheet
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import java.io.File


const val OSM_LOG = "OSM"

/**
 * OsmBottomSheets: waypoint_new_bottom_sheet, waypoint_bottom_sheet, map_bottom_sheet
 * Implement FragmentOsmMap.OsmBottomSheet interface
 * Implement OsmBottomSheet.OsmDialogListener interface to receive event callbacks
 *
 */
class OsmActivity : AppCompatActivity(),
    AdressesDialogFragment.NoticeDialogListener,
    WayPointNewBottomSheetDialog.ItemClickListener,
    FragmentOsmMap.OsmBottomSheet,
    OsmBottomSheet.OsmDialogListener {

    // db connections
    private val trackSource : TrackDatabaseDao
        get() = TrackDatabase.getInstance(application).trackDatabaseDao
    private val coordsSource : TrackCoordDatabaseDao
            get() = TrackDatabase.getInstance(application).trackCoordDatabaseDao
    private val trackWayPointSource : TrackWayPointDao
        get() = TrackDatabase.getInstance(application).trackWayPointDao

    private var osmActivityJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + osmActivityJob)

    private var trackObject = TrackObject()
    private var mapFragment: FragmentOsmMap = FragmentOsmMap()


    override fun onCreate(savedInstanceState: Bundle?) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_osm)

        // data sources for track object
        trackObject.coordsSource = coordsSource
        trackObject.trackWayPointSource = trackWayPointSource

        mapFragment.trackObject = trackObject

        val bundle = intent.extras

        when (bundle?.getString("TYPE")) {
            "database" -> {
                val id = intent.getLongExtra("ID", -1)
                if (id > 0) {
                    makeTrackFromDb(id)
                }
                trackObject.type = TrackSourceType.DATABASE
            }
            "file" -> {
                trackObject.type = TrackSourceType.FILE
                trackObject.trackSourceType = TrackSourceType.FILE
                val filePath = bundle.getString("PATH")
                if (filePath.isNullOrBlank()) {
                    Toast.makeText(this, "No track path!", Toast.LENGTH_SHORT).show()
                } else {
                    makeTrackFromFile(filePath)
                }

            }
            else -> {
                trackObject.type = TrackSourceType.NEW
                trackObject.trackSourceType = TrackSourceType.NEW
                trackObject.latitude = 52.4908
                trackObject.longitude = 13.4186
            }
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .add( R.id.container, mapFragment )
            .commit()

        val gpsPermissions = arrayOf( Manifest.permission.ACCESS_FINE_LOCATION)
        Permissions(applicationContext, this).requestPermissions(gpsPermissions, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION )

    }

    override fun onAttachFragment(fragment: Fragment) {
        if( fragment is FragmentOsmMap) {
            fragment.setOnOpenDialogListener(this)
        }
    }

    /**
     * Dispatch MotionEvent.ACTION_UP to map for map scroll
     *
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        if( ev?.action == MotionEvent.ACTION_UP) {
            Log.i(OSM_LOG, "OsmActivity dispatchTouchEvent: $ev")
            this@OsmActivity.mapFragment.receiveActionUP()
        }

        if( ev?.action == MotionEvent.ACTION_DOWN) {

        }
        return super.dispatchTouchEvent(ev)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("onActivityResult: $requestCode + $resultCode")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == OsmMapType.OSM_TRACK.value) {
            println("OSM map view in track mode.")
        }
    }


    /**
     * Set TrackObject values
     * Read coordinates for track from db.
     *
     * @param id
     */
    private fun makeTrackFromDb(id: Long) {

       // var trackObject = TrackObject()

        uiScope.launch {
            val trackModel = getTrack(id)

            trackObject.trackName = trackModel?.trackName
            trackObject.trackSourceType = TrackSourceType.DATABASE
            trackObject.id = id
            trackObject.latitude = trackModel?.latitude ?: 0.0
            trackObject.longitude = trackModel?.longitude ?: 0.0

            // get coords for track
            uiScope.launch {
                val trackCoords = getCoords(id)
                if (!trackCoords.isNullOrEmpty()) {
                    println("Coords in Track: ${trackCoords.size}")
                    trackObject.coords.addAll(trackCoords)
                    this@OsmActivity.mapFragment.trackObject = trackObject
                    this@OsmActivity.mapFragment.updateMap()
                    this@OsmActivity.mapFragment.trackObject.updateTrack()

                }
            }
        }
    }


    /**
     * Read track from table
     *
     * @param id
     */
    private suspend fun getTrack(id: Long) : TrackModel? {
       return  withContext(Dispatchers.IO) {
            val aModel = trackSource.get(id)
            aModel
        }
    }

    /**
     * Read coords from db
     *
     * @param id
     */
    private suspend fun getCoords(id: Long) : List<TrackCoordModel> {
        return withContext(Dispatchers.IO) {
            val coords = coordsSource.getCoordsForId(id)
            coords
        }
    }

    /**
     * Parse gpx file, extract meta data and coords
     *
     * @param path absolute path to .*gpx file
     */
    private fun makeTrackFromFile(path: String) {
        //var parsedGpx: Gpx
        val inputStream = File(path).inputStream()
        val parsedGpx = GPXParser().parse(inputStream)

        val tracks = parsedGpx.tracks

        for ( track in tracks) {
            trackObject.trackName = track.trackName ?: ""
            trackObject.trackDescription = track.trackDesc ?: ""
        }

        trackObject.id = 0
        // parse coords, points can be in waypoints or in tracks[i]
       if (parsedGpx.wayPoints.isNotEmpty()) {
            for (p in parsedGpx.wayPoints) {
                if (p is WayPoint) {
                    val c = TrackCoordModel()
                    c.latitude = p.latitude
                    c.longitude = p.longitude
                    c.altitude = p.elevation

                    trackObject.coords.add(c)
                    trackObject.coordsGpx.add(GeoPoint(c.latitude ?: 0.0, c.longitude ?: 0.0))
                }
            }
        } else {
            if (parsedGpx.tracks.isNotEmpty()) {
                // for the moment, use only track 1
                val t = parsedGpx.tracks.first()
                val ts = t.trackSegments
                val firstTrack = ts[0] as TrackSegment

                for (p in firstTrack.trackPoints) {
                    val c = TrackCoordModel()
                    c.latitude = p.latitude
                    c.longitude = p.longitude
                    c.altitude = p.elevation

                    trackObject.coords.add(c)
                    trackObject.coordsGpx.add(GeoPoint(c.latitude ?: 0.0, c.longitude ?: 0.0))
                }

            }
        }

        trackObject.latitude = trackObject.coordsGpx.first().latitude
        trackObject.longitude = trackObject.coordsGpx.first().longitude

        this@OsmActivity.mapFragment.trackObject = trackObject
    }


    /**
     * From AddressDialogFragment with address and position.
     * Return to NewTrackFragment
     *
     * @param dialog
     * @param result address string
     * @param geoPoint Coordinates of address on map
     */
    override fun onDialogPositiveClick(dialog: DialogFragment, result: String, geoPoint: GeoPoint) {
        println("Dialog positiveClick $result")
        println("Dialog selected: ")
        val data = Intent()
        data.putExtra("addresskey", result)
        data.putExtra("latitudekey", geoPoint.latitude)
        data.putExtra("longitudekey", geoPoint.longitude)

        setResult(Activity.RESULT_OK, data)
        finish()
    }


    override fun onDialogNegativeClick(dialog: DialogFragment) {
        println("Dialog negativeClick")
    }

    /////////////// OPTIONS MENU /////////////////////////////////

    // put the option on screen
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_help -> {
                println("Menu item Help selected")
                return true
            }

            R.id.menu_showWaypoints -> {
                if (item.isChecked ) {
                    item.isChecked = false
                    this@OsmActivity.mapFragment.showWayPoints( false )
                } else {
                    item.isChecked = true
                    this@OsmActivity.mapFragment.showWayPoints( true)
                }
                return true
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }

        return false
        //return super.onOptionsItemSelected(item)
    }

    /////////////// BottomSheetDialog //////////////////////////

    /**
     * Open a BottomSheetDialogFragment in New Waypoint mode.
     *
     * @param type what kind of action (new, show, edit)
     */
    override fun openOsmBottomSheet(type: String) {
        Log.i(OSM_LOG, "openOsmBottomSheet layout $type")
        when(type) {
            "WayPoint_New" -> {
                val dialog = OsmBottomSheet.getInstance(
                    R.layout.waypoint_new_bottom_sheet,
                    type
                )
                dialog.show(supportFragmentManager, dialog.tag)
            }
        }
    }

    /**
     * Open a BottomSheetDialogFragment in Display or Edit WayPoint mode.
     *
     * @param type
     * @param trackWayPointModel waypoint data
     */
    override fun openOsmBottomSheetWithContent(type: String, trackWayPointModel: TrackWayPointModel) {
        Log.i(OSM_LOG, "openOsmBottomSheetWithContent layout $type")
        when (type) {
            "WayPoint" -> {
                val dialog = OsmBottomSheet.getInstance(
                    R.layout.waypoint_bottom_sheet,
                    type,
                    trackWayPointModel
                )
                dialog.show(supportFragmentManager, dialog.tag)
            }
            "WayPoint_Edit" -> {
                val dialog = OsmBottomSheet.getInstance(
                    R.layout.waypoint_edit_bottom_sheet,
                    type,
                    trackWayPointModel
                )
                dialog.show(supportFragmentManager, dialog.tag)
            }
        }
    }



    /**
     * OsmBottomSheet call
     *
     * @param item
     */
    override fun onItemClick(item: String) {
        Log.i(OSM_LOG, "FragmentOsmMap.onItemClick $item")
        uiScope.launch {
            mapFragment.onItemClick(item)
        }

    }

    /**
     * Receive TrackWayPointModel
     * Update pointId to selected point
     *
     * @param waypoint
     * @param dialog BottomSheetDialog to dismiss after saving waypoint
     */
    override fun onSaveWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet) {
        Log.i(OSM_LOG, "OsmActivity.onSaveWaypoint")
        wayPointModel.trackId = trackObject.id
        wayPointModel.pointId = (mapFragment.selectedMarkersPathPosition.first() + 1).toLong()

        uiScope.launch {
            val res = trackObject.insertWayPoint(wayPointModel)
            Log.i(OSM_LOG, "onSaveWayPoint result $res")
            // ToDo there should be another way to get the result from insertWayPoint()
            if (res.equals(-1)) {
                Toast.makeText(applicationContext, "Error saving waypoint", Toast.LENGTH_LONG).show()
            } else {
                dialog.dismiss()
                // waypoint successfully added, update display
                mapFragment.showWayPoints(true)
            }
        }
        //trackObject.addWayPoint(waypoint)
    }

    override fun onEditWaypoint(trackWayPointModel: TrackWayPointModel, dialog: OsmBottomSheet) {
        Log.i(OSM_LOG, "OsmActivity.onEditWaypoint")
        dialog.dismiss()

        openOsmBottomSheetWithContent("WayPoint_Edit", trackWayPointModel)
    }

    override fun onUpdateWaypoint(trackWayPointModel: TrackWayPointModel, dialog: OsmBottomSheet) {
        Log.i(OSM_LOG, "OsmActivity.onUpdateWaypoint")

        uiScope.launch {
            val res = trackObject.updateWayPoint(trackWayPointModel)
            Log.i(OSM_LOG, "onUpdateWayPoint result: $res")
            if (res.equals(-1)) {
                Toast.makeText(applicationContext, "Error updating waypoint", Toast.LENGTH_LONG).show()
            } else {
                dialog.dismiss()
            }
        }
    }
}
