package com.e.tracker.osm.dialogs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.osm.OSM_LOG
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.waypoint_btn_1
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.waypoint_header
import java.lang.RuntimeException

/**
 *
 * @param layoutResource
 */
class OsmBottomSheet(
    private val layoutResource: Int) :
    BottomSheetDialogFragment(),
    View.OnClickListener {

    private var mListener: OsmDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(layoutResource, container, false)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (dialogType) {
            "NewWayPoint" -> {
                //var trackWayPoint = TrackWayPointModel()
                view.waypoint_btn_1.setOnClickListener { mListener?.onSaveWaypoint(
                    TrackWayPointModel( 0L, waypoint_header.text as String, "info", 0L, 0L  )
                ) }
            }

            "WayPoint" -> {
                if (trackWayPointModel != null) {
                    view.findViewById<TextView>(R.id.waypoint_header).text = trackWayPointModel.wayPointName
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mListener = if (context is OsmDialogListener) {
            context
        } else {
            throw RuntimeException(
                "$context must implement OsmDialogListener"
            )
        }
    }

    override fun onClick(view: View?) {
        Log.i(OSM_LOG, "OsmBottomSheet onClick()")
        mListener?.onItemClick( view.toString())
    }



    interface OsmDialogListener {
        fun onItemClick(item: String)
        fun onSaveWaypoint(waypoint: TrackWayPointModel)
    }


    companion object {

        var dialogType: String = ""
        lateinit  var trackWayPointModel: TrackWayPointModel

        /**
         *
         * @param layoutResource BottomSheetDialog layout
         */
        fun getInstance(layoutResource: Int, dialogType: String) : OsmBottomSheet {
            this.dialogType = dialogType

            return OsmBottomSheet(layoutResource)
        }

        fun getInstance(layoutResource: Int, dialogType: String, wp: TrackWayPointModel) : OsmBottomSheet {
            this.dialogType = dialogType
            this.trackWayPointModel = wp


            return OsmBottomSheet(layoutResource)
        }
    }
}