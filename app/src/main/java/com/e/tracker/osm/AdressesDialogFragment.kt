package com.e.tracker.osm

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.e.tracker.R
import org.osmdroid.util.GeoPoint
import java.lang.ClassCastException
import java.lang.IllegalStateException

/**
 * Dialog with list of found address's
 *
 * @param title
 * @param items Address's found
 * @param geoPoint Coordinates on map as GeoPoint
 */
class AdressesDialogFragment(
    val title: String,
    val items: Array<CharSequence>,
    val geoPoint: GeoPoint) : DialogFragment() {


    internal lateinit var listener: NoticeDialogListener

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, result: String, geoPoint: GeoPoint)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }


    // Attach callback interface
    override fun onAttach(context: Context) {
        val ctx = requireContext()
        super.onAttach(ctx)
        try {
            listener = ctx as NoticeDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((ctx.toString() + " must implement NoticeDialogListener"))
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var selectedItem = 0

            builder.setTitle(title)

                .setSingleChoiceItems(items, 0,
                    DialogInterface.OnClickListener { dialog, which ->
                        selectedItem = which })


                .setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, id ->
                        listener.onDialogPositiveClick(this,
                            items[selectedItem].toString(),
                            geoPoint)
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        listener.onDialogNegativeClick(this)
                    })

            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}