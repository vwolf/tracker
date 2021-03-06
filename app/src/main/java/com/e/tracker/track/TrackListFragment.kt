package com.e.tracker.track

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
//import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.e.tracker.R
import com.e.tracker.support.OsmMapType
import com.e.tracker.database.TrackDatabase
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentTrackListBinding
import com.e.tracker.osm.OsmActivity
import com.e.tracker.support.trackToFile.TrackToGpx
import com.e.tracker.xml.gpx.GPXParser
import com.e.tracker.xml.gpx.domain.Gpx
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.io.File



/**
 * List of tracks from database and external storage
 */
class TrackListFragment : Fragment(), TrackEditDialogFragment.TrackEditDialogListener {
    // extension property for TrackModel -> type of track source
//    var TrackModel.source: String
//        get() = "file"
//        set(value: String)  { source = value }

    // put filepath's to external storage locations into gpxFilePaths to be used later
    // do it later?
    private var gpxFilePaths = listOf<File>()
    private var gpxFilePathMap = mutableMapOf<String, String>()

    private val gpxParser : GPXParser = GPXParser()

    private lateinit var viewModel: TrackViewModel
    private lateinit var tracksObjects: TracksObj

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filespaths = arguments?.getParcelable<TrackFileParcel>("filePaths")
        if ( !filespaths?.myfilesList.isNullOrEmpty() ) {
            gpxFilePaths = filespaths!!.myfilesList
        }

        //arguments.getParcelable<TracksObj>("tracks").let { this.tracksObjects = it }
        val tracksObjects = arguments?.getParcelable<TracksObj>("tracks")
        if (tracksObjects != null) {
           this.tracksObjects = tracksObjects
        }
//        println(tracksObjects)
//        if (tracksObjects?.tracks?.fileTracks?.isNotEmpty() == true ) {
//            val m = tracksObjects?.tracks
//            println("${m.fileTracks.size}")
//       }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentTrackListBinding>(
            inflater, R.layout.fragment_track_list, container, false)


        val application = requireNotNull(this.activity).application

        // Create instance of the ViewModel Factory
        val dataSource = TrackDatabase.getInstance(application).trackDatabaseDao
        val coordsSource = TrackDatabase.getInstance(application).trackCoordDatabaseDao

        val viewModelFactory = TrackViewModelFactory(dataSource, coordsSource, application)

        // Get reference to the ViewModel associated with this fragment
        val trackListViewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(TrackViewModel::class.java)

        //val trackListViewModel = ViewModelProviders.of(this).get(TrackViewModel::class.java)

        // To use the ViewModel with data binding give the binding object a reference to it
        binding.trackListViewModel = trackListViewModel
        binding.lifecycleOwner = this

        viewModel = trackListViewModel

        val adapter = TrackAdapter( TrackListener { clickedTrack ->
            showMap(clickedTrack)
            //trackListViewModel.onTrackItemClicked(clickedTrack)
            //val intent = Intent(requireContext(), OsmActivity::class.java)
            //startActivityForResult(intent, GET_LOCATION_ADDRESS)
        } )
        { editIconClicked: TrackModel, idx ->
            // show dialog
//            var trackSource = "file"
//            if (editIconClicked.id > 0) {
//                trackSource = "db"
//            }
            //val dialogFragment = TrackEditDialogFragment("Select Location", trackSource)

            val dialogFragment = TrackEditDialogFragment.newInstance("Select Action", editIconClicked.id, idx, "TrackAction")
            dialogFragment.setTargetFragment(this, 100)
            dialogFragment.show(requireFragmentManager(), "TrackAction")
        }

        binding.trackList.adapter = adapter

        // Observe tracks in ViewModel
        trackListViewModel.tracks.observe(viewLifecycleOwner, Observer {

            it.let {
                trackListViewModel.addData(it)
                adapter.data = trackListViewModel.mergedTracks
            }
        })

        // Add gpx tracks to
        parseGpxFiles(gpxFilePaths, trackListViewModel, adapter)

        setHasOptionsMenu(true)

        return binding.root
    }


    @SuppressLint("RestrictedApi")
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
        val trackDataList = mutableListOf<TrackModel>()

        for ( t in filesPathList) {
            val aTrack = TrackModel()

            var parsedGpx : Gpx
            val inputStream = File(t.absolutePath).inputStream()
            parsedGpx = gpxParser.parse(inputStream)

            extractMetaData(parsedGpx, aTrack)

            //aTrack.source = "file"
            trackDataList.add(aTrack)
            gpxFilePathMap[aTrack.trackName] = t.absolutePath
        }

        viewModel.addData(trackDataList)
        adapter.data = viewModel.mergedTracks
    }


    private fun extractMetaData(parsedGpx: Gpx, trackModel: TrackModel) {
        val tracks = parsedGpx.tracks

        for ( track in tracks) {
            trackModel.trackName = track.trackName ?: ""
            trackModel.trackDescription = track.trackDesc ?: ""
        }
    }

    /**
     * Get map data and display
     *
     */
    private fun showMap(clickedTrack: TrackModel) {

        val newBundle = Bundle()
        if(clickedTrack.id > 0) {
            newBundle.putString("TYPE", "database")
            newBundle.putLong("ID", clickedTrack.id)
        } else {
            newBundle.putString("TYPE", "file")
            newBundle.putLong("ID", clickedTrack.id)
            newBundle.putString("TRACKNAME", clickedTrack.trackName)
            newBundle.putString("PATH", gpxFilePathMap[clickedTrack.trackName])
        }


        val intent = Intent(requireContext(), OsmActivity::class.java)
        intent.putExtras(newBundle)
        startActivityForResult(intent, OsmMapType.OSM_TRACK.value)

    }

    override fun onDialogCancelClick(dialog: DialogFragment) {
        println("onDialogCancelClick")
    }


    /**
     *
     * @param dialog
     * @param result Duplicate, Edit or Delete
     */
    override fun onDialogPositiveClick(dialog: DialogFragment, result: String, trackId: Long) {
        println("onDialogPositiveClick result: $result")
        when(result) {
            "Duplicate" -> {
                val b = DataBindingUtil.findBinding<FragmentTrackListBinding>(requireView())
                b?.trackList?.adapter?.notifyDataSetChanged()
                TrackToGpx(trackId, Application() )
            }

            "Edit" -> {
                val newBundle = Bundle()
                newBundle.putParcelable("track", TrackParcel(trackId))
                view?.findNavController()?.navigate(R.id.action_trackListFragment_to_newTrackFragment, newBundle)}

            "Delete" -> {
                viewModel.deleteTrackWithId(trackId)

                //val b = DataBindingUtil.findBinding<FragmentTrackListBinding>(requireView())
                //b?.trackList?.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onDialogOkClick(dialog: DialogFragment, result: DialogItems, trackId: Long, idx: Int) {
        println("onDialogOkClick ${result.value}")

        when (result) {
            DialogItems.EDIT -> {
                val newBundle = Bundle()
                newBundle.putParcelable("track", TrackParcel(trackId))
                view?.findNavController()?.navigate(R.id.action_trackListFragment_to_newTrackFragment, newBundle)
            }
            DialogItems.DELETE -> {
                viewModel.deleteTrackWithId(trackId)
            }
            DialogItems.DELETE_FILE -> {
                println("Delete file: ${tracksObjects.tracks.fileTracks[idx].filePath}")
                viewModel.deleteTrackFile(idx)
            }
            DialogItems.TO_DB -> {}
            DialogItems.TO_FILE -> {
                val b = DataBindingUtil.findBinding<FragmentTrackListBinding>(requireView())
                b?.trackList?.adapter?.notifyDataSetChanged()
                TrackToGpx(trackId, Application() )
            }
        }
    }
}

@Parcelize
data class TrackFileParcel(val filesList: List<File>) : Parcelable {
    @IgnoredOnParcel
    val myfilesList : List<File> = filesList
}

@Parcelize
data class TrackParcel(val aId: Long) : Parcelable {
    @IgnoredOnParcel
    val trackId: Long = aId
}

@Parcelize
data class TracksName(val tracks: Map<String, String>) : Parcelable {
    @IgnoredOnParcel
    val trackNames : Map<String, String> = tracks
}

@Parcelize
data class TracksObj(val tracks: Tracks) : Parcelable {
    @IgnoredOnParcel
    val t: Tracks = tracks
}