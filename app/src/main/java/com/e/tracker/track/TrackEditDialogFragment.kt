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
    private var trackId: Long,
    private var idx: Int
) : DialogFragment() {

    companion object {
        private const val KEY = "select"

        @JvmStatic
        fun newInstance(title: String, trackId: Long, idx: Int, key: String) =
            TrackEditDialogFragment(title, trackId, idx).apply {
                arguments = Bundle().apply {
                    putString(KEY, key)
                }
            }
    }


    internal lateinit var listener: TrackEditDialogListener

    interface TrackEditDialogListener {
        fun onDialogCancelClick(dialog: DialogFragment)
        fun onDialogPositiveClick(dialog: DialogFragment, result: String, trackId: Long)
        fun onDialogOkClick(dialog: DialogFragment, result: DialogItems, trackId: Long, idx: Int)
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
            //var items = arrayOf("Save As File")
            var items = arrayOf<String>()

            if (trackId > 0) {
                // track source db and preselect Edit
                items = items.plus(arrayOf(DialogItems.TO_FILE.value, DialogItems.EDIT.value, DialogItems.DELETE.value))
                selectedItem = 1
            } else {
                // track source file
                items = items.plus(arrayOf(DialogItems.TO_DB.value, DialogItems.DELETE_FILE.value))
            }

            builder.setTitle(title)
                .setSingleChoiceItems(
                    items,
                    selectedItem
                ){ _, which -> selectedItem = which }

//                .setPositiveButton("OK")
//                    { _,
//                      _ -> listener.onDialogPositiveClick(this,
//                        items[selectedItem], trackId)
//                    }

                .setPositiveButton("OK")
                { _,
                  _ ->
                    DialogItems.values().find { it.value == items[selectedItem]}?.let { it1 ->
                        listener.onDialogOkClick(this,
                            it1,
                            trackId, idx)
                    }

                }

                .setNegativeButton("Cancel")
                    { _,
                      _ -> listener.onDialogCancelClick(this)
                    }


            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

enum class DialogItems(val value: String) {
    EDIT("Edit"),
    DELETE("Delete"),
    DELETE_FILE("Delete File"),
    TO_FILE("Save as File"),
    TO_DB("Save in DB")
}