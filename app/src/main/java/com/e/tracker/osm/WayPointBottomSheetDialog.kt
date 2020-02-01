package com.e.tracker.osm

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.view.*

class WayPointBottomSheetDialog(
    val trackWayPointModel: TrackWayPointModel,
    val btnEdit: () -> Unit,
    val btnDelete: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            window?.setDimAmount(0.2f)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.waypoint_bottom_sheet, container, false)
//        v.waypoint_btn_save.setOnClickListener {
//            btnEdit() }
//
//        v.waypoint_btn_2.setOnClickListener {
//            btnDelete()
//        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.waypoint_header).text = trackWayPointModel.wayPointName
    }

    fun getBack() {

    }


    companion object {
        fun getInstance( trackWayPointModel: TrackWayPointModel, btnEdit: () -> Unit, btnDelete: () -> Unit) : WayPointBottomSheetDialog  {
            return WayPointBottomSheetDialog(trackWayPointModel, btnEdit, btnDelete)
        }
    }
}