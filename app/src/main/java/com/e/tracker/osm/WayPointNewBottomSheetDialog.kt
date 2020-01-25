package com.e.tracker.osm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.e.tracker.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WayPointNewBottomSheetDialog : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.waypoint_new_bottom_sheet, container, false)

    }

    companion object {
        fun getInstance() : WayPointNewBottomSheetDialog {
            return WayPointNewBottomSheetDialog()
        }
    }
}