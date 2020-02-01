package com.e.tracker.osm

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.e.tracker.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.*


class WayPointNewBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var mListener: ItemClickListener? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.waypoint_new_bottom_sheet, container, false)

    //    layout.waypoint_btn_1.setOnClickListener {  }
    //    layout.waypoint_btn_2.setOnClickListener {  }
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.waypoint_btn_save.setOnClickListener(this)
        //view.waypoint_btn_2.setOnClickListener(this)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        //fragmentManager?.getFragment(this.requireArguments(), R.layout.waypoint_new_bottom_sheet ) is ItemClickListener
        mListener = if (context is ItemClickListener) {
            context
        } else {
            throw RuntimeException(
                "$context must implement ItemClickListener"
            )
        }
    }


    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onClick(view: View?){
        Log.i(OSM_LOG, "onClick $view")
        mListener?.onItemClick( view.toString())
    }


    interface ItemClickListener {
        fun onItemClick(item: String)
    }

    companion object {
        fun getInstance() : WayPointNewBottomSheetDialog {
            return WayPointNewBottomSheetDialog()
        }
    }
}