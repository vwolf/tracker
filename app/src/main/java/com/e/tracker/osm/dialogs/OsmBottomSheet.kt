package com.e.tracker.osm.dialogs

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.net.toUri
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.osm.OSM_LOG
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.waypoint_btn_1
import kotlinx.android.synthetic.main.waypoint_edit_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_image.*
import kotlinx.android.synthetic.main.waypoint_images_edit.*
import kotlinx.android.synthetic.main.waypoint_images_edit.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.waypoint_newimages_layout
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_description
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_header
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_newimages_layout
import kotlinx.android.synthetic.main.waypoint_video_edit.view.*
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

    private var imagesToDelete = mutableListOf<ImageView>()

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
            "WayPoint_New" -> {

                view.waypoint_btn_deleteimage.alpha = 0.5f
                view.waypoint_btn_deleteimage.isEnabled = false

                view.waypoint_btn_1.setOnClickListener {
                    onSaveWayPoint()
                }
                // images
                view.waypoint_btn_addimage.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet addImage button")
                    showPictureDialog()
                }
                view.waypoint_btn_deleteimage.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet deleteImage button")
                    deleteImage()
                }
                // video
                view.waypoint_btn_addvideo.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet addVideo button")
                    showVideoDialog()
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
                                waypoint_imageview_genric.setImageBitmap(bitmap)
                                //waypoint_image_view.setImageBitmap(bitmap)
                            } else {
                                // new imageView, clone from layout
                                val imageView = ImageView(requireContext())
                                imageView.layoutParams = waypoint_imageview_genric?.layoutParams

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


                    waypoint_btn_1.setOnClickListener {
                        Log.i(OSM_LOG, "Waypoint edit button click")
                        editWayPoint()
                    }

                    waypoint_btn_2.setOnClickListener {
                        Log.i(OSM_LOG, "Waypoint delete button")
                    }
                }
            }

            "WayPoint_Edit" -> {
                // text content
                if (trackWayPointModel != null) {
                    view.findViewById<TextView>(R.id.waypoint_edit_header).text =
                        trackWayPointModel.wayPointName
                    view.findViewById<TextView>(R.id.waypoint_edit_description).text =
                        trackWayPointModel.description

                    if (trackWayPointModel.wayPointImages.isNotEmpty()) {
                        showImagesInEditMode()
                    }
                }

                waypoint_edit_save.setOnClickListener {
                    onUpdateWayPoint()
                }
            }
        }
    }


    /**
     *
     * ToDo Images not available anymore
     *
     */
    private fun showImagesInEditMode() {
        for ( i in trackWayPointModel.wayPointImages) {
                // new imageView, clone from layout
                val imageView = ImageView(requireContext())

                imageView.layoutParams = waypoint_imageview_genric?.layoutParams

                waypoint_newimages_layout.addView(imageView)
                //waypoint_images_layout.addView(imageView)

                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(i.toUri()))
                imageView.setImageBitmap(bitmap)

                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            imageView.setOnLongClickListener {
                Log.i(OSM_LOG, "Long touch on image")
                if (imageView.alpha == 1.0f) {
                    imageView.alpha = 0.5f
                    updateImagesToDelete(imageView, true)
                } else {
                    imageView.alpha = 1.0f
                    updateImagesToDelete(imageView, false)
                }

                true
            }
        }
    }


    private fun newImageView() : ImageView {
        val imageView = ImageView(requireContext())
        imageView.layoutParams = waypoint_imageview_genric.layoutParams
        //imageView.layoutParams = view?.waypoint_newimage_view?.layoutParams
        return imageView
    }


    private fun videoThumbnailView() : ImageView {
        val videoThumbnailView = ImageView(requireContext())
        videoThumbnailView.layoutParams = waypoint_imageview_genric.layoutParams
        return videoThumbnailView
    }

    //@TargetApi(29)
//    private fun getVideoThumbnail(uri: Uri, size: Size) : Bitmap {

//        if (Build.VERSION.SDK_INT > 28){
//            val options = BitmapFactory.Options()
//            options.inSampleSize = 1
//            val cr = requireContext().contentResolver
//            val bitmap = cr.loadThumbnail(uri, size, null)
//
//        }
//      }

    private fun getVideoThumbnail(id: Long) : Bitmap {
        val contentResolver = requireContext().contentResolver
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        val bitmap = MediaStore.Video.Thumbnails.getThumbnail(contentResolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, options)

        return bitmap
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (resultCode == Activity.RESULT_OK && requestCode == 1001) {
//            view?.waypoint_newimage_view?.setImageURI(data?.data)
//        }

        if (requestCode == 1) {
            if (data != null) {
                try {
//                    var imageView = view?.waypoint_newimage_view
//                    if (imagePathList.isNotEmpty()) {
//                       var imageView = newImageView()
//                        view?.waypoint_newimages_layout?.addView(imageView)
//                    }
                    var imageView = newImageView()
                    view?.waypoint_newimages_layout?.addView(imageView)
                    imageView?.maxHeight = 160
                    //view?.waypoint_new_imageView?.maxWidth = 160
                    imageView?.setImageURI(data.data)
                    imageView?.adjustViewBounds = true
                    imageView?.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView?.setOnLongClickListener {

                        Log.i(OSM_LOG, "Long touch on image")
                        if (imageView.alpha == 1.0f) {
                            imageView.alpha = 0.5f
                            updateImagesToDelete(imageView, true)
                        } else {
                            imageView.alpha = 1.0f
                            updateImagesToDelete(imageView, false)
                        }

                        true
                    }

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
                       // projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
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
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_LONG).show()
                }
            }
        }

        if (requestCode == 2) {
            if (data != null) {
                try {
                    // get video uri
                    val projection = arrayOf(MediaStore.Video.Media._ID)
                    val cUri = data.data as Uri
                    requireActivity().contentResolver.query(cUri, projection, null, null, null)?.use {
                        it.moveToFirst()
                        Log.i(OSM_LOG, "${it.getLong(0)}")
                        val vid = it.getLong(0)

                        // imageView for video thumbnail
                        var thumbnailView = videoThumbnailView()

                        val thumbnail = getVideoThumbnail(vid)
                        thumbnailView.setImageBitmap(thumbnail)
                        thumbnailView.layoutParams = ViewGroup.LayoutParams(160, 120)
                        waypoint_newvideo_layout.addView(thumbnailView)
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to load video", Toast.LENGTH_LONG).show()
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

    private fun showVideoDialog() {
        val videoDialog = AlertDialog.Builder(requireContext())
        videoDialog.setTitle("Select Action")
        val videoDialogItems = arrayOf("Select video from gallery", "Capture video with camera")
        videoDialog.setItems(videoDialogItems) {
            dialog, which ->
            when (which) {
                0 -> chooseVideoFromGallery()
                1 -> takeVideoWithCamera()
            }
        }
        videoDialog.show()
    }


    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, 1)
    }

    private fun takePhotoFromCamera() {}

    private fun chooseVideoFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(galleryIntent, 2)
    }

    private fun takeVideoWithCamera() {}
    /**
     *
     */
    private fun editWayPoint() {
        mListener?.onEditWaypoint(trackWayPointModel, this)
    }

    /**
     * Remove images in [imagesToDelete]
     *
     */
    private fun deleteImage() {
        val firstImageView = waypoint_newimages_layout.getChildAt(0)
        for ( imageView in imagesToDelete) {
            waypoint_newimages_layout.removeView(imageView)
        }
        imagesToDelete.clear()
        waypoint_btn_deleteimage.alpha = 0.5f
        waypoint_btn_deleteimage.isEnabled = false
    }

    /**
     *
     */
    private fun updateImagesToDelete(image: ImageView, action: Boolean) {
       if (action) {
           imagesToDelete.add(image)
       } else {
           imagesToDelete.remove(image)
       }

        if (imagesToDelete.isNotEmpty()) {
            waypoint_btn_deleteimage.alpha = 1.0f
            waypoint_btn_deleteimage.isEnabled = true
        } else {
            waypoint_btn_deleteimage.alpha = 0.5f
            waypoint_btn_deleteimage.isEnabled = false
        }

    }


    /**
     * Make TrackWayPointModel and send to listener
     * 
     */
    private fun onSaveWayPoint() {
//        val n = view?.waypoint_header?.text.toString()
//        val nn = view?.waypoint_header?.text

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
                ), this
            )

        } else {
            Toast.makeText(
                requireContext(),
                "Enter Name!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onUpdateWayPoint() {
        if (waypoint_edit_header.text.toString().isNotEmpty()) {
            trackWayPointModel.wayPointName = waypoint_edit_header.text.toString()
            trackWayPointModel.description = waypoint_edit_description.text.toString()
            trackWayPointModel.wayPointImages = imagePathList

            mListener?.onUpdateWaypoint(
                trackWayPointModel, this
            )
        } else {
            Toast.makeText(
                requireContext(),
                "Missing Name",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /**
     * Interface to communicate with dialog listener
     */
    interface OsmDialogListener {
        fun onItemClick(item: String)
        fun onSaveWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onEditWaypoint(trackWayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onUpdateWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
    }


    companion object {

        var dialogType: String = ""
        lateinit  var trackWayPointModel: TrackWayPointModel

        /**
         * Return BottomSheet with waypoint
         *
         * @param layoutResource BottomSheetDialog layout
         * @param dialogType
         */
        fun getInstance(layoutResource: Int, dialogType: String) : OsmBottomSheet {
            this.dialogType = dialogType

            return OsmBottomSheet(layoutResource)
        }

        /**
         *
         *
         */
        fun getInstance(layoutResource: Int, dialogType: String, trackWayPointModel: TrackWayPointModel) : OsmBottomSheet {
            this.dialogType = dialogType
            this.trackWayPointModel = trackWayPointModel

            return OsmBottomSheet(layoutResource)
        }
    }
}