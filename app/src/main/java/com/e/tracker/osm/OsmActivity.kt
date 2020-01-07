package com.e.tracker.osm

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.DialogFragment
import com.e.tracker.R
import com.e.tracker.Support.OsmMapType
import com.e.tracker.database.*
import com.e.tracker.track.TrackObject
import com.e.tracker.track.TrackSourceType
import com.e.tracker.xml.gpx.domain.Track
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint

class OsmActivity : AppCompatActivity(), AdressesDialogFragment.NoticeDialogListener  {


    val trackSource : TrackDatabaseDao
        get() = TrackDatabase.getInstance(application).trackDatabaseDao
    val coordsSource : TrackCoordDatabaseDao
            get() = TrackDatabase.getInstance(application).trackCoordDatabaseDao

    private var osmActivityJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + osmActivityJob)

    var trackObject = TrackObject()
    var mapFragment: FragmentOsmMap = FragmentOsmMap()



    override fun onCreate(savedInstanceState: Bundle?) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_osm)

        // track object init
        trackObject.coordsSource = coordsSource
        mapFragment.trackObject = trackObject

        var bundle = intent.extras

        val sourceType = bundle?.getString("TYPE")

        when (sourceType) {
            "database" -> {
                val id = intent.getLongExtra("ID", -1)
                if (id > -1) {
                    makeTrackFromDb(id)
                }
                trackObject.type = TrackSourceType.DATABASE
            }
            "file" -> {
                trackObject.type = TrackSourceType.FILE
            }
            else -> {
                trackObject.type = TrackSourceType.NEW
                trackObject.latitude = 52.4908
                trackObject.longitude = 13.4186
            }
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // osm map fragment
        //mapFragment = FragmentOsmMap(trackObject)

        supportFragmentManager.beginTransaction()
            .add( R.id.container, mapFragment )
            .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("onActivityResult: $requestCode + $resultCode")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == OsmMapType.OSM_TRACK.value) {
            println("OSM map view in track mode.")
        }
    }


    fun makeTrackFromDb(id: Long) {

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
                }
            }
        }

    }


    /**
     * Read track from table
     */
    private suspend fun getTrack(id: Long) : TrackModel? {
       return  withContext(Dispatchers.IO) {
            val aModel = trackSource.get(id)
            aModel
        }
    }




    private suspend fun getCoords(id: Long) : List<TrackCoordModel> {
        return withContext(Dispatchers.IO) {
            val coords = coordsSource.getCoordsForId(id)
            coords
        }
    }

    /**
     * From AddressDialogFragment with address and position.
     * Return to NewTrackFragment
     *
     * @param dialog
     * @param item address string
     * @param geoPoint Coordinates of address on map
     */
    override fun onDialogPositiveClick(dialog: DialogFragment, item: String, geoPoint: GeoPoint) {
        println("Dialog positiveClick $item")
        println("Dialog selected: ")
        var data = Intent()
        data.putExtra("addresskey", item)
        data.putExtra("latitudekey", geoPoint.latitude)
        data.putExtra("longitudekey", geoPoint.longitude)

        setResult(Activity.RESULT_OK, data)
        finish()
    }


    override fun onDialogNegativeClick(dialog: DialogFragment) {
        println("Dialog negatieClick")
    }


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

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }

        return false
        //return super.onOptionsItemSelected(item)
    }
}
