package com.e.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentMainBinding
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