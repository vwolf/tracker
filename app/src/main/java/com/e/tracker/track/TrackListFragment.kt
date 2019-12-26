package com.e.tracker.track

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.e.tracker.R
import com.e.tracker.Support.OsmMapType
//import com.e.tracker.database.TrackCoordDatabase
import com.e.tracker.database.TrackDatabase
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentTrackListBinding
import com.e.tracker.osm.OsmActivity
import com.e.tracker.xml.gpx.GPXParser
import com.e.tracker.xml.gpx.domain.Gpx
import kotlinx.android.parcel.Parcelize
import java.io.File


/**
 * List of tracks from database and external storage
 */
class TrackListFragment : Fragment() {

    // put filepath's to external storage locations into gpxFilePaths to be used later
    // do it later?
    var gpxFilePaths = listOf<File>()

    val gpxParser : GPXParser = GPXParser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filespaths = arguments?.getParcelable<TrackFileParcel>("filePaths")
        if ( !filespaths?.myfilesList.isNullOrEmpty() ) {
            gpxFilePaths = filespaths!!.myfilesList
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentTrackListBinding>(
            inflater, R.layout.fragment_track_list, container, false)


        var application = requireNotNull(this.activity).application

        // Create instance of the ViewModel Factory
        val dataSource = TrackDatabase.getInstance(application).trackDatabaseDao
        val coordsSource = TrackDatabase.getInstance(application).trackCoordDatabaseDao

        val viewModelFactory = TrackViewModelFactory(dataSource, coordsSource, application)

        // Get reference to the ViewModel associated with this fragment
        val trackListViewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(TrackViewModel::class.java)

        // To use the ViewModel with data binding give the binding object a reference to it
        binding.trackListViewModel = trackListViewModel

        binding.setLifecycleOwner(this)

        val adapter = TrackAdapter( TrackListener { clickedTrack ->
            showMap(clickedTrack)
            //trackListViewModel.onTrackItemClicked(clickedTrack)
            //val intent = Intent(requireContext(), OsmActivity::class.java)
            //startActivityForResult(intent, GET_LOCATION_ADDRESS)
        }, { editIconClicked: TrackModel ->
            trackListViewModel.onTrackEditIconClicked( editIconClicked )
        })

        binding.trackList.adapter = adapter

        // Add gpx tracks to

        // Observe tracks in ViewModel
        trackListViewModel.tracks.observe(viewLifecycleOwner, Observer {

            it.let {
                trackListViewModel.addData(it)
                adapter.data = trackListViewModel.mergedTracks
            }
        })

        parseGpxFiles(gpxFilePaths, trackListViewModel, adapter)

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        MenuBuilder(context).setOptionalIconsVisible(true)

        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("ActivityResult in TrackListFragment")
        super.onActivityResult(requestCode, resultCode, data)

    }

    /**
     * Parse gpx file at path. extract meta data ( name...) and add to ViewModel
     *
     * @param filesPathList
     */
    private fun parseGpxFiles(filesPathList: List<File>, viewModel: TrackViewModel, adapter: TrackAdapter)  {
        var trackDataList = mutableListOf<TrackModel>()

        for ( t in filesPathList) {
            var aTrack = TrackModel()


            var parsedGpx : Gpx
            val inputStream = File(t.absolutePath).inputStream()
            parsedGpx = gpxParser.parse(inputStream)

            extractMetaData(parsedGpx, aTrack)

            trackDataList.add(aTrack)

        }

        viewModel.addData(trackDataList)
        adapter.data = viewModel.mergedTracks
    }


    private fun extractMetaData(parsedGpx: Gpx, trackModel: TrackModel) {
        val tracks = parsedGpx.tracks

        for ( (index: Int, track: Any) in tracks.withIndex()) {
            trackModel.trackName = track.trackName ?: ""
            trackModel.trackDescription = track.trackDesc ?: ""
        }
    }

    /**
     * Get map data and display
     *
     */
    private fun showMap(clickedTrack: TrackModel) {

        val newBundle : Bundle = Bundle()
        newBundle.putString("TYPE", "database")
        newBundle.putLong("ID", clickedTrack.id)

        val intent = Intent(requireContext(), OsmActivity::class.java)
        intent.putExtras(newBundle)
        startActivityForResult(intent, OsmMapType.OSM_TRACK.value)

    }

}

@Parcelize
data class TrackFileParcel(val filesList: List<File>) : Parcelable {
    val myfilesList : List<File> = filesList
}

//@Parcelize
//data class TrackParcel(val track: TrackModel) : Parcelable {}