package com.e.tracker.osm.dialogs

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.graphics.Bitmap
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.osm.OSM_LOG
import com.e.tracker.osm.WAYPOINT_IMAGEFROMCAMERA
import com.e.tracker.osm.WAYPOINT_IMAGEFROMGALLERY
import com.e.tracker.osm.WAYPOINT_VIDEOFROMGALLERY
import com.e.tracker.support.image.PicHolder
import com.e.tracker.support.image.PictureFacer
import com.e.tracker.track.TrackViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_edit_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_image.*
import kotlinx.android.synthetic.main.waypoint_media_edit.*
import kotlinx.android.synthetic.main.waypoint_media_edit.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.view.*
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_description
import kotlinx.android.synthetic.main.waypoint_new_bottom_sheet.waypoint_header
import org.joda.time.DateTime
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 *
 * @param layoutResource
 */
class OsmBottomSheet(
    private val layoutResource: Int) :
    BottomSheetDialogFragment(),
    View.OnClickListener {

    private var mListener: OsmDialogListener? = null

    var imagePathList = mutableListOf<String>()
    //var imagesToDelete = mutableListOf<ImageView>()

    private var videoPathList = mutableListOf<String>()
    private var audioPathList = mutableListOf<String>()

    lateinit var imageService : ImageService
    lateinit var videoService: VideoService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(layoutResource, container, false)

        imageService = ImageService.getInstance(this)
        videoService = VideoService.getInstance(this)
        return layout
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply { window?.setDimAmount(0.3f) }
    }


    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (dialogType) {
            "WayPoint_New" -> {

                view.waypoint_btn_save.setOnClickListener { onSaveWayPoint() }
                view.waypoint_btn_cancel.setOnClickListener { onCancelWayPoint() }

                // images
                view.waypoint_media_image_delete_btn.alpha = 0.5f
                view.waypoint_media_image_delete_btn.isEnabled = false
                view.waypoint_media_image_add_btn.setOnClickListener { imageService.showPictureDialog() }
                view.waypoint_media_image_delete_btn.setOnClickListener { imageService.deleteImage() }

                waypoint_media_imageview.visibility = View.GONE

                // video
                view.waypoint_media_video_delete_btn.alpha = 0.5f
                view.waypoint_media_video_delete_btn.isEnabled = false

                view.waypoint_media_video_add_btn.setOnClickListener { videoService.showVideoDialog() }

                waypoint_media_videoview.visibility = View.GONE

                // audio
                view.waypoint_media_audio_delete_btn.alpha = 0.5f
                view.waypoint_media_audio_delete_btn.isEnabled = false

                waypoint_media_audioview.visibility = View.GONE
            }

            "WayPoint" -> {
                // text content
                view.findViewById<TextView>(R.id.waypoint_header).text =
                    trackWayPointModel.wayPointName
                view.findViewById<TextView>(R.id.waypoint_description).text =
                    trackWayPointModel.description

                // Fill images section
                if (trackWayPointModel.wayPointImages.isNotEmpty()) {
                    imageService.showImages(trackWayPointModel.wayPointImages)
                } else {
                    waypoint_show_media_images.visibility = View.GONE
                }

                // Videos
                if (videoPathList.isEmpty()) {
                    waypoint_show_media_video.visibility = View.GONE
                }

                // Audios
                if (audioPathList.isEmpty()) {
                    waypoint_show_media_audio.visibility = View.GONE
                }

                // edit waypoint
                waypoint_btn_edit.setOnClickListener {
                    Log.i(OSM_LOG, "Waypoint edit button click")
                    editWayPoint()
                }
                // delete waypoint
                waypoint_btn_delete.setOnClickListener {
                    Log.i(OSM_LOG, "Waypoint delete button")
                    deleteWayPoint()
                }
            }

            "WayPoint_Edit" -> {
                // text content
                view.findViewById<TextView>(R.id.waypoint_edit_header).text =
                    trackWayPointModel.wayPointName
                view.findViewById<TextView>(R.id.waypoint_edit_description).text =
                    trackWayPointModel.description

                // images
                if (trackWayPointModel.wayPointImages.isNotEmpty()) {
                    imageService.showImagesInEditMode(trackWayPointModel)
                }

                view.waypoint_media_image_add_btn.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet addImage button")
                    imageService.showPictureDialog()
                }
                view.waypoint_media_image_delete_btn.setOnClickListener {
                    Log.i(OSM_LOG, "OsmBottomSheet deleteImage button")
                    imageService.deleteImage()
                }

                waypoint_media_image_delete_btn.isEnabled = false
                waypoint_media_image_delete_btn.alpha = 0.5f

                // video
                waypoint_media_video_delete_btn.isEnabled = false
                waypoint_media_video_delete_btn.alpha = 0.5f
                waypoint_media_videoview.visibility = View.GONE

                // audio
                waypoint_media_audio_delete_btn.isEnabled = false
                waypoint_media_audio_delete_btn.alpha = 0.5f
                waypoint_media_audioview.visibility = View.GONE


                waypoint_edit_save.setOnClickListener {
                    onUpdateWayPoint()
                }
            }
        }
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

        if (requestCode == WAYPOINT_IMAGEFROMGALLERY) {
            if (data != null) {
                try {
                    val imageView = imageService.newImageView()
                    view?.waypoint_media_imageview?.addView(imageView)

                    imageView.maxHeight = 160
                    imageView.setImageURI(data.data)
                    imageView.setOnLongClickListener {

                        Log.i(OSM_LOG, "Long touch on image")
                        if (imageView.alpha == 1.0f) {
                            imageView.alpha = 0.5f
                            imageService.updateImagesToDelete(imageView, true)
                        } else {
                            imageView.alpha = 1.0f
                            imageService.updateImagesToDelete(imageView, false)
                        }

                        true
                    }
                    view?.waypoint_media_imageview?.visibility = View.VISIBLE

                    val p = data.data?.path as String
                    Log.i(OSM_LOG, "Path: $p")

                    if (data.data is Uri) {
                        Log.i(OSM_LOG, "Uri: ${data.data}")

                        // get image uri
                        val projection = arrayOf(MediaStore.Images.Media._ID)
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

        if (requestCode == WAYPOINT_VIDEOFROMGALLERY) {
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
                        val thumbnailView = videoThumbnailView()

                        val thumbnail = getVideoThumbnail(vid)
                        thumbnailView.setImageBitmap(thumbnail)
                        thumbnailView.layoutParams = ViewGroup.LayoutParams(160, 120)
                        waypoint_media_videoview.addView(thumbnailView)
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to load video", Toast.LENGTH_LONG).show()
                }
            }
        }


        if (requestCode == WAYPOINT_IMAGEFROMCAMERA && resultCode == Activity.RESULT_OK) {

            try {
                val bitmap = BitmapFactory.decodeStream(
                    requireContext().contentResolver.openInputStream(imageService.currentImagePath.toUri())
                )

                val imageView = imageService.newImageView()
                imageView.setImageBitmap(bitmap)

                waypoint_media_imageview.visibility = View.VISIBLE
                view?.waypoint_media_imageview?.addView(imageView)

                imagePathList.add(imageService.currentImagePath)

            } catch (e: IOException) {

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



    /**
     *
     */
    private fun editWayPoint() {
        mListener?.onEditWaypoint(trackWayPointModel, this)
    }


    fun displayImage(imagePath: String) {
        mListener?.onImageClick( trackWayPointModel.wayPointImages )
    }


    fun loadImageGallery(picHolder: PicHolder, position: Int, pics: ArrayList<PictureFacer>) {
        mListener?.onImageClickRecyclerView(picHolder, position, pics)
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
                    imagePathList,
                    videoPathList,
                    audioPathList
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

    /**
     * Update TrackWayPointModel and send to listener
     *
     */
    private fun onUpdateWayPoint() {
        if (waypoint_edit_header.text.toString().isNotEmpty()) {
            trackWayPointModel.wayPointName = waypoint_edit_header.text.toString()
            trackWayPointModel.description = waypoint_edit_description.text.toString()
            trackWayPointModel.wayPointImages = imagePathList
            trackWayPointModel.wayPointVideos = videoPathList
            trackWayPointModel.wayPointAudios = audioPathList

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

    private fun deleteWayPoint() {
        mListener?.onDeleteWaypoint(trackWayPointModel, this)

    }

    private fun onCancelWayPoint() {
        this.dismiss()
    }

    /**
     * Interface to communicate with dialog listener
     */
    interface OsmDialogListener {
        fun onItemClick(item: String)
        fun onSaveWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onEditWaypoint(trackWayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onUpdateWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onDeleteWaypoint(wayPointModel: TrackWayPointModel, dialog: OsmBottomSheet)
        fun onImageClick(wayPointImages : List<String>)
        fun onImageClickRecyclerView(picHolder: PicHolder, position: Int, pics: ArrayList<PictureFacer>)
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