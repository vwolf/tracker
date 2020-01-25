package com.e.tracker.track

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.e.tracker.R
import com.e.tracker.database.TrackDatabase
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.FragmentNewTrackBinding
import com.e.tracker.osm.OsmActivity
import kotlinx.android.synthetic.main.fragment_new_track.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject

const val GET_LOCATION_ADDRESS = 1
/**
 * Define new track and insert into db
 * Edit track and update db
 *
 */
class NewTrackFragment : Fragment() {

    private var trackType = ObservableField<String>()
    private var trackId = 0L
    private var trackLoaded = TrackModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val track = arguments?.getParcelable<TrackParcel>("track")
       // if (track?.trackId?.equals(0)) {}
        if (track != null) {
            if (track.trackId != 0L) {
                trackId = track.trackId
                println("Edit track with id $trackId")
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentNewTrackBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_new_track, container, false)

        // get data source - db
        val application = requireNotNull(this.activity).application
        val dataSource = TrackDatabase.getInstance(application).trackDatabaseDao
        val coordSource = TrackDatabase.getInstance(application).trackCoordDatabaseDao
        val viewModelFactory = TrackViewModelFactory(dataSource, coordSource, application)

        val trackViewModel = ViewModelProviders.of(
            this, viewModelFactory).get(TrackViewModel::class.java)

        binding.newTrackViewModel = trackViewModel

        // handle type buttons events
        binding.buttonWalk.setOnClickListener { trackType.set("walking") }
        binding.buttonBike.setOnClickListener { trackType.set("biking") }

        if (trackId > 0L) {
            GlobalScope.async {
                val loadedTrack = trackViewModel.getTrackWithId(trackId)
                if (loadedTrack is TrackModel) {
                    trackLoaded = loadedTrack

                    // Submit button
                    binding.trackButtonSubmit.text = getString(R.string.track_button_edit)
                    binding.trackButtonSubmit.setOnClickListener {
                        editTrack(binding)
                        view?.findNavController()?.navigate(R.id.action_newTrackFragment_to_mainFragment)
                    }

                    track_name.setText(trackLoaded.trackName)
                    track_description.setText(trackLoaded.trackDescription)
                    track_location.setText(trackLoaded.location)
                    track_latitude.setText(trackLoaded.latitude.toString())
                    track_longitude.setText(trackLoaded.longitude.toString())

                    trackType.set("walking")
                    binding.trackType = trackType
                }
            }
        } else {
            // buttons to select type, set default value and bind to layout
            trackType.set("walking")
            binding.trackType = trackType

            // Submit button
            binding.trackButtonSubmit.setOnClickListener {
                submitNewTrack(binding)
                view?.findNavController()?.navigate(R.id.action_newTrackFragment_to_mainFragment)
            }

        }

        // for submit and edit track
        // Map button - open map and user can select point
        binding.buttonMap.setOnClickListener {
            val intent = Intent(requireContext(), OsmActivity::class.java)
            startActivityForResult(intent, GET_LOCATION_ADDRESS)
        }

        setHasOptionsMenu(true)

        return binding.root
    }

    // set all loaded values again???
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (trackLoaded.id > 0L) {
            track_name.setText(trackLoaded.trackName)
            track_description.setText(trackLoaded.trackDescription)
            track_location.setText(trackLoaded.location)
            track_latitude.setText(trackLoaded.latitude.toString())
            track_longitude.setText(trackLoaded.longitude.toString())

            trackType.set(trackLoaded.type)

        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.toolbar_menu, menu)
        val mb = MenuBuilder(context)
        mb.setOptionalIconsVisible(true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Build TrackModel from form and send to TrackViewModel
     *
     * @param binding
     */
    private fun submitNewTrack(binding: FragmentNewTrackBinding) {
        val newTrack = TrackModel()

        newTrack.trackName = track_name.text.toString()
        newTrack.trackDescription = track_description.text.toString()
        newTrack.location = track_location.text.toString()
        newTrack.latitude = track_latitude.text.toString().toDouble()
        newTrack.longitude = track_longitude.text.toString().toDouble()

        val startCoordinates = JSONObject().put("latitude" , track_latitude.text)
        startCoordinates.put("longitude", track_longitude.text)
        newTrack.startCoordinates = startCoordinates.toString()

        binding.newTrackViewModel?.insertNewTrack(newTrack, res = { newTrackInsert(it) } )
    }


    private fun editTrack(binding: FragmentNewTrackBinding) {
        trackLoaded.trackName = track_name.text.toString()
        trackLoaded.trackDescription = track_description.text.toString()
        trackLoaded.location = track_location.text.toString()
        trackLoaded.latitude = track_latitude.text.toString().toDouble()
        trackLoaded.longitude = track_longitude.text.toString().toDouble()

        val startCoordinates = JSONObject().put("latitude" , track_latitude.text)
        startCoordinates.put("longitude", track_longitude.text)
        trackLoaded.startCoordinates = startCoordinates.toString()

        binding.newTrackViewModel?.updateTrack(trackLoaded)
    }


    // AddressDialogFragment return
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == GET_LOCATION_ADDRESS) {
            if (resultCode == Activity.RESULT_OK) {
                    //val resultReturned = data.data.toString()
                val addressReturned = data?.getStringExtra("addresskey")
                val latitude = data?.getDoubleExtra("latitudekey", 0.0)
                val longitude = data?.getDoubleExtra("longitudekey", 0.0)

                println("Address return: $addressReturned")
                this.track_location.setText(addressReturned)
                this.track_latitude.setText(latitude.toString())
                this.track_longitude.setText(longitude.toString())

                // if trackname and description empty then set to address
                if (this.track_name.text.isEmpty()) {
                    this.track_name.setText(addressReturned)
                }

                if (this.track_description.text.isEmpty()) {
                    this.track_description.setText(addressReturned)
                }

            }
        }
    }

    /**
     * Return from new track insert
     *
     * @param result id of new track insert
     */
    private fun newTrackInsert(result: Long) {}
}