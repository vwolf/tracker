package com.e.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.e.tracker.databinding.FragmentMainBinding
import com.e.tracker.track.TrackFileParcel
import com.e.tracker.track.TrackListFragment
import com.e.tracker.track.Tracks
import java.io.File

class MainFragment : Fragment() {

    var filesList = listOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filesList = Tracks().getTracksFromExternalStorage(requireContext())

        if (filesList.isEmpty()) {
            Toast.makeText(requireContext(), "No tracks at external storage!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Tracks found ${filesList.size}", Toast.LENGTH_LONG).show()
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
            view.findNavController().navigate(R.id.action_mainFragment_to_newTrackFragment)
        }


        // To TrackListFragment
        binding.buttonShow.setOnClickListener { view: View ->

            val newBundle: Bundle = Bundle()
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


}