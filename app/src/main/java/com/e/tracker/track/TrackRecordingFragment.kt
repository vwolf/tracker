package com.e.tracker.track

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.e.tracker.R
import com.e.tracker.support.OsmMapType
import com.e.tracker.database.TrackDatabase
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentTrackRecordingBinding
import com.e.tracker.osm.OsmActivity
import kotlinx.android.synthetic.main.fragment_track_recording.*
import org.joda.time.DateTime
import org.json.JSONObject


/**
 * Here we start a new track recording session
 *
 */
class TrackRecordingFragment  : Fragment() {

    lateinit var locationManager : LocationManager
    lateinit var locationListener: LocationListener

    lateinit var currentLocation : Location

    private var newTrack = TrackModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentTrackRecordingBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_track_recording, container, false
        )

        // set a default track name
        binding.trackrecordingName.setText( DateTime.now().toLocalDateTime().toString() )

        val application = requireNotNull(this.activity).application
        val dataSource = TrackDatabase.getInstance(application).trackDatabaseDao
        val coordSource = TrackDatabase.getInstance(application).trackCoordDatabaseDao
        val viewModelFactory = TrackViewModelFactory(dataSource, coordSource, application)
        val trackViewModel = ViewModelProviders.of(
            this, viewModelFactory).get(TrackViewModel::class.java)

        binding.newTrackViewModel = trackViewModel

        // track recording only works if location is enabled
        //val mgr = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            println("Permission denied READ_EXTERNAL_STORAGE")
            Toast.makeText(
                context,
                 "No permission to access location!",
                Toast.LENGTH_LONG
            ).show()
            binding.trackrecordingStart.isEnabled = false
            //makeRequest(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_READ_EXTERNAL)
        } else {
            println("Permission granted READ_EXTERNAL_STORAGE")

            binding.trackrecordingStart.isEnabled = false
            binding.trackrecordingStart.alpha = 0.4F

            // get current location
            // track recording only works if location is enabled
            locationListener = getLocation()

        }

        binding.trackrecordingStart.setOnClickListener {
            startRecording(binding.newTrackViewModel)
        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        println("TrackRecordingFragment onDetach()")
    }


    override fun onDestroy() {
        super.onDestroy()
        println("TrackRecordingFragment onDestroy()")

        locationManager.removeUpdates(locationListener)
    }


    /**
     * Write new track to db
     *
     */
    private fun startRecording( trackViewModel : TrackViewModel?) {
        if (currentLocation.latitude > 0.0) {
            newTrack.trackName = trackrecording_name.text.toString()
            newTrack.trackDescription = "new track recording"

            newTrack.latitude = currentLocation.latitude
            newTrack.longitude = currentLocation.longitude
            newTrack.type = "walking"

            val startCoordinates = JSONObject().put("latitude" , currentLocation.latitude)
            startCoordinates.put("longitude", currentLocation.longitude)
            newTrack.startCoordinates = startCoordinates.toString()

            trackViewModel?.insertNewTrack(newTrack, res = { trackInsertOk(it) })

        }
    }


    /**
     * This is the return call from track insert into db
     *
     * @param result  id of insert track
     */
    fun trackInsertOk(result: Long) {
        println("trackInsertOk with result: $result")
        if (result > -1) {
            val newBundle = Bundle()
            newBundle.putString("TYPE", "database")
            newBundle.putLong("ID", result)

            locationManager.removeUpdates(locationListener)

            val intent = Intent(requireContext(), OsmActivity::class.java)
            intent.putExtras(newBundle)
            startActivityForResult(intent, OsmMapType.OSM_TRACK.value)
        } else {

        }
    }


    private fun getLocation() : LocationListener {
       locationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                val latitude = location?.latitude
                val longitude = location?.longitude

                currentLocation = location!!

                trackrecording_start.alpha = 1.0F
                trackrecording_start.isEnabled = true

                println("locationManager location: ${location.toString()}")
            }

            override fun onProviderEnabled(p0: String?) {
                trackrecording_start.alpha = 1.0F
                trackrecording_start.isEnabled = true
            }

            override fun onProviderDisabled(p0: String?) {
                trackrecording_start.alpha = 0.4F
                trackrecording_start.isEnabled = false
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 10f, locationListener)

        } catch (e : SecurityException) {
            Toast.makeText(context, "Fehler gpx", Toast.LENGTH_LONG).show()
        }

        return locationListener
    }

    private fun removeLocationListener() {

    }
}

//class ll : LocationListener {
//
//    override fun onLocationChanged(p0: Location?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onProviderEnabled(p0: String?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onProviderDisabled(p0: String?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//}