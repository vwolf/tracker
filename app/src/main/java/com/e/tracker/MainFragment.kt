package com.e.tracker

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentMainBinding
import com.e.tracker.osm.OSM_LOG
import com.e.tracker.track.TrackFileParcel
import com.e.tracker.track.Tracks
import com.e.tracker.track.TracksName
import com.e.tracker.xml.gpx.GPXParser
import com.e.tracker.xml.gpx.domain.Gpx
import java.io.File

class MainFragment : Fragment() {

    var filesList = listOf<File>()
    private val gpxParser : GPXParser = GPXParser()
    var gpxFileData = mutableListOf<TrackModel>()
    var gpxFileNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filesList = Tracks().getTracksFromExternalStorage(requireContext())

        if (filesList.isEmpty()) {
            Toast.makeText(requireContext(), "No tracks at external storage!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Tracks found ${filesList.size}", Toast.LENGTH_LONG).show()
            gpxFileData = parseGpxFiles(filesList)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentMainBinding>(inflater, R.layout.fragment_main, container, false)

        // To NewTrackFragment
        binding.buttonAdd.setOnClickListener{ view: View ->

            val newBundle = Bundle()
            //newBundle.putParcelable("filePaths", TrackFileParcel(filesList))
            newBundle.putParcelable("gpxFileNames", TracksName(gpxFileNames))
            view.findNavController().navigate(R.id.action_mainFragment_to_newTrackFragment, newBundle)
        }


        // To TrackListFragment
        binding.buttonShow.setOnClickListener { view: View ->

            val newBundle = Bundle()
            newBundle.putParcelable("filePaths", TrackFileParcel(filesList))
//
//            val aFragment = TrackListFragment()
//
//            fragmentManager!!.beginTransaction()
//                .add(R.id.main_constraint, aFragment)
//                .commit()

            view.findNavController().navigate(R.id.action_mainFragment_to_trackListFragment, newBundle)


        }

        binding.buttonStart.setOnClickListener { view: View ->

            view.findNavController().navigate(R.id.action_mainFragment_to_trackRecordingFragment)
        }

//        val sharedPref = activity?.getSharedPreferences(
//            getString(R.string.gpx_tracking_distance), Context.MODE_PRIVATE
//        )
//        Log.i(OSM_LOG, "Gpx tracking distance: ${sharedPref.toString()}")
//        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
//        val defaultValue = resources.getInteger(R.integer.gpx_tracking_default)
//        val gpxValue = sharedPref?.getInt(getString(R.string.gpx_tracking_distance), defaultValue)
//
//        Log.i(OSM_LOG, "SharedPref: $defaultValue, $gpxValue")

//        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//       // val v = prefs.getInt(getString((R.string.gpx_distance_key)), 33)
//        val w = prefs.getString("gpx_distance", "33")
//
//        val intervalDefault = resources.getString(R.string.gpx_interval_default)

        return binding.root
    }

    /**
     * Parse files in filePathList
     */
    private fun parseGpxFiles(filePathList: List<File>): MutableList<TrackModel> {
        val trackDataList = mutableListOf<TrackModel>()

        for ( t in filePathList) {
            val aTrack = TrackModel()
            val parsedGpx: Gpx
            val inputStream = File(t.absolutePath).inputStream()
            parsedGpx = gpxParser.parse(inputStream)

            getTracksMetaData(parsedGpx, aTrack)
            //var tracks = parsedGpx.tracks

            trackDataList.add(aTrack)
        }

        return trackDataList
    }

    private fun getTracksMetaData(parsedGpx: Gpx, trackModel: TrackModel) {
        val tracks = parsedGpx.tracks

        for ( track in tracks) {
            trackModel.trackName = track.trackName ?: ""
            trackModel.trackDescription = track.trackDesc ?: ""

            gpxFileNames[trackModel.trackName] = trackModel.trackDescription
        }
    }
}