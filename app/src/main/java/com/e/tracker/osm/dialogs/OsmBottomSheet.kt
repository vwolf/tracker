package com.e.tracker.osm.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.osm.OSM_LOG
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_description
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_header
import org.joda.time.DateTime


/**
 *
 * @param layoutResource
 */
class OsmBottomSheet(
    private val layoutResource: Int) :
    BottomSheetDialogFragment(),
    View.OnClickListener {

    private var mListener: OsmDialogListener? = null
    private var imagePathList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(layoutResource, container, false)

        return layout
    }

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (dialogType) {
            "NewWayPoint" -> {
                //var trackWayPoint = TrackWayPointModel()
                view.waypoint_btn_1.setOnClickListener {
                    onSaveWayPoint()
                }
                view.waypoint_btn_addimage.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet addImage button")
//                    val intent = Intent(Intent.ACTION_PICK)
//                    intent.type = "image/*"
//                    startActivityForResult(intent, 1001)

                    showPictureDialog()
                }
            }

            // waypoint_bottom_sheet
            "WayPoint" -> {
                if (trackWayPointModel != null) {
                    // text content
                    view.findViewById<TextView>(R.id.waypoint_header).text = trackWayPointModel.wayPointName
                    view.findViewById<TextView>(R.id.waypoint_description).text = trackWayPointModel.description

                    // media content
                    // Images
                    if (trackWayPointModel.wayPointImages.isNotEmpty()) {
                        for ( (ip, i) in trackWayPointModel.wayPointImages.withIndex()) {
                            if (ip == 0) {
                                // use imageView in layout
                                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(i.toUri()))
                                waypoint_image_view.setImageBitmap(bitmap)
                            } else {
                                // new imageView, clone from layout
                                val imageView = ImageView(requireContext())
                                imageView.layoutParams = waypoint_image_view?.layoutParams

                                waypoint_images_layout.addView(imageView)

                                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(i.toUri()))
                                imageView.setImageBitmap(bitmap)

                                imageView.adjustViewBounds = true
                                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        }
                    }

                    // Videos

                    // Audios

                }
            }
        }
    }



    private fun newImageView() : ImageView {
        val imageView = ImageView(requireContext())
        imageView.layoutParams = view?.waypoint_newimage_view?.layoutParams
        return imageView
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 1001) {
            view?.waypoint_newimage_view?.setImageURI(data?.data)
        }

        if (requestCode == 1) {
            if (data != null) {
//                val d = DocumentFile.fromSingleUri(requireContext(), data.data as Uri)
//                if (d != null) {
//                    val filePath = d.uri.path
//                    Log.i(OSM_LOG, "filePath: $filePath")
//                }
//                data.data?.let { returnUri ->
//                    val proj =
//                        arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
//                    context?.contentResolver?.query(returnUri, null, null, null, null)
//                }?.use { cursor ->
//                    /*
//                     * Get the column indexes of the data in the Cursor,
//                     * move to the first row in the Cursor, get the data,
//                     * and display it.
//                     */
//                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
//                    //val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
//                    //val contentTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.CONTENT_TYPE)
//
//
//                    cursor.moveToFirst()
//                    Log.i(OSM_LOG, "DISPLAY_NAME: ${cursor.getString(nameIndex)}")
//                    Log.i(OSM_LOG, "Size: ${cursor.getLong(sizeIndex)}")
//                    //Log.i(OSM_LOG, "ContentType: ${cursor.getInt(columnIndex)}")
//
//                    //findViewById<TextView>(R.id.filename_text).text = cursor.getString(nameIndex)
//                    //findViewById<TextView>(R.id.filesize_text).text = cursor.getLong(sizeIndex).toString()
//                    val pd = Environment.DIRECTORY_PICTURES
//                    val imgPath = pd + cursor.getString(nameIndex)
//                    Log.i(OSM_LOG, "Path image: $imgPath")
//                }

                //val contentUrl = data!!.data
                try {
                    var imageView = view?.waypoint_newimage_view
                    if (imagePathList.isNotEmpty()) {
                        imageView = newImageView()
                        view?.waypoint_newimages_layout?.addView(imageView)
                    }
                    imageView?.maxHeight = 160
                    //view?.waypoint_new_imageView?.maxWidth = 160
                    imageView?.setImageURI(data.data)
                    imageView?.adjustViewBounds = true
                    imageView?.scaleType = ImageView.ScaleType.FIT_CENTER

                    val p = data.data?.path as String
                    Log.i(OSM_LOG, "Path: $p")

                    if (data.data is Uri) {

                        Log.i(OSM_LOG, "Uri: ${data.data.toString()}")

                        // get image uri
                        var projection = arrayOf(MediaStore.Images.Media._ID)
                        val curi = data.data as Uri
                        requireActivity().contentResolver.query(curi, projection, null, null, null )?.use {
                            it.moveToFirst()

                            Log.i(OSM_LOG, "${it.getLong(0)}")

                            val uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                it.getLong(0)
                            )
                            Log.i(OSM_LOG, "Uri to save: $uri")
                            imagePathList.add(uri.toString())
                        }

                        // get display name of image
                        projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
                        data.data?.let { uri ->
                            context?.contentResolver?.query(uri, null, null, null, null)
                        }?.use {
                            cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            cursor.moveToFirst()
                            Log.i(OSM_LOG, "DisplayName: ${cursor.getString(nameIndex)}, Size: ${cursor.getLong(sizeIndex)}")
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed", Toast.LENGTH_LONG).show()
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


    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(requireContext())
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera ")
        pictureDialog.setItems(pictureDialogItems) {
            dialog, which ->
            when (which) {
            0 -> choosePhotoFromGallery()
            1 -> takePhotoFromCamera()
        }
        }
        pictureDialog.show()
    }

    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, 1)
    }

    private fun takePhotoFromCamera() {}

    /**
     * Make TrackWayPointModel and send to listener
     * 
     */
    private fun onSaveWayPoint() {
        val n = view?.waypoint_header?.text.toString()
        val nn = view?.waypoint_header?.text

        if (view?.waypoint_header?.text.toString().isNotEmpty()) {
            mListener?.onSaveWaypoint(
                TrackWayPointModel(
                    0L,
                    DateTime().toDate(),
                    waypoint_header.text.toString(),
                    waypoint_description.text.toString(),
                    "info",
                    0L,
                    0L,
                    imagePathList
                )
            )
        }
    }

    /**
     * Interface to communicate with dialog listener
     */
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