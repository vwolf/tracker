package com.e.tracker.track

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.lang.ClassCastException
import java.lang.IllegalStateException

/**
 * Dialog for trackList element.
 * SingleChoiceItems depending on track source type (file or db)
 *
 */
class TrackEditDialogFragment(
    private var title: String,
    private var trackId: Long
) : DialogFragment() {

    companion object {
        private const val KEY = "select"

        @JvmStatic
        fun newInstance(title: String, trackId: Long, key: String) =
            TrackEditDialogFragment(title, trackId).apply {
                arguments = Bundle().apply {
                    putString(KEY, key)
                }
            }
    }


    internal lateinit var listener: TrackEditDialogListener

    interface TrackEditDialogListener {
        fun onDialogCancelClick(dialog: DialogFragment)
        fun onDialogPositiveClick(dialog: DialogFragment, result: String, trackId: Long)
    }


    override fun onAttach(context: Context) {
        //val ctx = requireContext()
        val ctx = targetFragment
        super.onAttach(context)
        try {
            listener = ctx as TrackEditDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((ctx.toString() + " must implement TrackEditDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var selectedItem = 0
            var items = arrayOf("Duplicate")
            if (trackId > 0) {
                items = items.plus(arrayOf("Edit", "Delete"))
                selectedItem = 1
            }

            builder.setTitle(title)
                .setSingleChoiceItems(
                    items,
                    selectedItem
                ){ _, which -> selectedItem = which }

                .setPositiveButton("OK")
                    { _,
                      _ -> listener.onDialogPositiveClick(this,
                        items[selectedItem], trackId)
                    }

                .setNegativeButton("Cancel")
                    { _,
                      _ -> listener.onDialogCancelClick(this)
                    }


            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}