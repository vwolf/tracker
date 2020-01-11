package com.e.tracker.osm

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toolbar
import com.e.tracker.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.map_bottom_sheet.*


class MapBottomSheetDialog(val trackDistance: String, val trackElevation: String, val cb: () -> Unit ) : BottomSheetDialogFragment() {

    private lateinit var toolbar: Toolbar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            window?.setDimAmount(0f)
            //track_distance.setText(trackDistance)

//            setOnShowListener { setupMapBottomSheetDialog(this) }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        println("BottomSheetDialog cancel")
        cb()
    }

//    private fun setupMapBottomSheetDialog(dialog: BottomSheetDialog) {
//     val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
//         ?: return BottomSheetBehavior.from(bottomSheet).apply {
//             state = BottomSheetBehavior.STATE_EXPANDED
//             skipCollapsed = true
//             isHideable = true
//         }
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.track_distance).setText(trackDistance)
        view.findViewById<TextView>(R.id.track_elevation).setText(trackElevation)
    }

    companion object {
        fun getInstance( trackDistance: String, trackElevation: String = "", cb: () -> Unit) : MapBottomSheetDialog {
            return MapBottomSheetDialog(trackDistance, trackElevation, cb)
        }
    }
}