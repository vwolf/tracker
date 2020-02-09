package com.e.tracker.osm.dialogs

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.transition.Fade
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.e.tracker.R
import com.e.tracker.database.TrackWayPointModel
import com.e.tracker.osm.OSM_LOG
import com.e.tracker.osm.WAYPOINT_IMAGEFROMCAMERA
import com.e.tracker.osm.WAYPOINT_IMAGEFROMGALLERY
import com.e.tracker.support.image.*
import kotlinx.android.synthetic.main.activity_image_display.*
import kotlinx.android.synthetic.main.waypoint_bottom_sheet.*
import kotlinx.android.synthetic.main.waypoint_image.*
import kotlinx.android.synthetic.main.waypoint_media_edit.*
import kotlinx.android.synthetic.main.waypoint_recycler_images.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Load image from gallery
 * Take image with camera and save to gallery
 *
 */
class ImageService  : ItemClickListener {

    lateinit var currentImagePath: String

    var imagesToDelete = mutableListOf<ImageView>()

    fun choosePhotoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
       // callingFragment.activity.startActivityForResult(galleryIntent, WAYPOINT_IMAGEFROMGALLERY)
        osmBottomSheet.startActivityForResult(galleryIntent, WAYPOINT_IMAGEFROMGALLERY)

    }


    fun newImageView() : ImageView {
        val imageView = ImageView(osmBottomSheet.requireContext())
        imageView.layoutParams = osmBottomSheet.waypoint_imageview_genric.layoutParams
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        return imageView
    }


    /**
     * Images when [OsmBottomSheet] in display mode
     */
    fun showImages( wayPointImages : List<String>) {

        showImagesInRecyclerView(wayPointImages)
//        for ( i in wayPointImages) {
//
//            val imageView = newImageView()
//            osmBottomSheet.waypoint_show_media_imageview.addView(imageView)
//
//            val bitmap = BitmapFactory.decodeStream(
//                osmBottomSheet.requireContext().contentResolver.openInputStream(i.toUri())
//            )
//            imageView.setImageBitmap(bitmap)
//
//            imageView.setOnClickListener {
//                imageBig(i)
//            }
//        }
    }

    fun showImagesInRecyclerView( wayPointImages: List<String>) {
        var imageRecycler = osmBottomSheet.waypoint_recycler
        imageRecycler.hasFixedSize()

        var images = ArrayList<PictureFacer>()
        if (wayPointImages.isNotEmpty()) {
            for ( imagePath in wayPointImages) {
                var pictureFacer = PictureFacer()
                pictureFacer.picturePath = imagePath

                images.add(pictureFacer)
            }
        }

        imageRecycler.adapter = PictureAdapter(images, osmBottomSheet.requireActivity().baseContext, this)
    }

    override fun onPicClicked(pictureFolderPath: String, folderName: String) {}


    override fun onPicClicked(holder: PicHolder, position: Int, pics: ArrayList<PictureFacer>) {
        Log.i(OSM_LOG, "ImageService onPicClicked: position: $position")
        osmBottomSheet.loadImageGallery(holder, position, pics )

//        val browser = PictureBrowserFragment.newInstance(pics, position, osmBottomSheet.requireActivity())
//        browser.enterTransition = Fade()
//        browser.exitTransition = Fade()
//
//        osmBottomSheet.requireActivity().supportFragmentManager
//            .beginTransaction()
//            .addSharedElement(holder.picture, position.toString() + "picture")
//            .add(R.id.displayContainer, browser)
//            .addToBackStack(null)
//            .commit()
    }

    /**
     * Images when [OsmBottomSheet] in edit mode
     *
     * @param trackWayPointModel
     */
    fun showImagesInEditMode( trackWayPointModel : TrackWayPointModel) {
        for ( i in trackWayPointModel.wayPointImages) {
            // new imageView, clone from layout
            val imageView = newImageView()

            osmBottomSheet.waypoint_media_imageview.addView(imageView)
            //waypoint_images_layout.addView(imageView)

            val bitmap = BitmapFactory.decodeStream(osmBottomSheet.requireContext().contentResolver.openInputStream(i.toUri()))
            imageView.setImageBitmap(bitmap)

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

            osmBottomSheet.imagePathList.add(i)
        }
    }


    fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(osmBottomSheet.requireContext())
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


    private fun takePhotoFromCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(osmBottomSheet.requireActivity().packageManager)?.also {
                var values = ContentValues(3)

                // create file where photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    null
                }

                values.put(MediaStore.Images.Media.DISPLAY_NAME, photoFile?.name)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                // path to gallery
                val insertUri = osmBottomSheet.requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                Log.i(OSM_LOG, "insertUri: $insertUri")
                currentImagePath = insertUri.toString()
                Log.i(OSM_LOG, "currentPhotoPath: $currentImagePath")

                // continue if file was succefully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        osmBottomSheet.requireContext(),
                        "com.e.tracker.fileprovider",
                        it
                    )

                    // to save image in gallery use insertUri Uri
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, insertUri)
                    osmBottomSheet.startActivityForResult(takePictureIntent, WAYPOINT_IMAGEFROMCAMERA)
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile() : File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = osmBottomSheet.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.i(OSM_LOG, "StorageDir: $storageDir, timeStamp: $timeStamp")

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentImagePath = absolutePath
            Log.i(OSM_LOG, "createImageFile.absolutPath: $currentImagePath")
        }
    }


    /**
     * Remove images in [imagesToDelete]
     * Set delete button state
     * Remove image view if no images
     *
     */
    fun deleteImage() {
//        val firstImageView = waypoint_media_images.getChildAt(0)
        for ( imageView in imagesToDelete) {
            val imageIdx = osmBottomSheet.waypoint_media_imageview.indexOfChild(imageView)
            osmBottomSheet.waypoint_media_imageview.removeView(imageView)
            if (osmBottomSheet.imagePathList.size >= imageIdx && imageIdx > 0) {
                osmBottomSheet.imagePathList.removeAt(imageIdx - 1)
            }
        }
        imagesToDelete.clear()
        setDeleteButtonState(null)

        if (osmBottomSheet.imagePathList.isEmpty()) {
            osmBottomSheet.waypoint_media_imageview.visibility = View.GONE
        }

    }

    /**
     *
     */
    fun updateImagesToDelete(image: ImageView, action: Boolean) {
        if (action) {
            imagesToDelete.add(image)
        } else {
            imagesToDelete.remove(image)
        }

        setDeleteButtonState(null)
    }


    private fun setDeleteButtonState(state: Boolean?) {
        if (state == null) {
            if (imagesToDelete.isNotEmpty()) {
                osmBottomSheet.waypoint_media_image_delete_btn.alpha = 1.0f
                osmBottomSheet.waypoint_media_image_delete_btn.isEnabled = true
            } else {
                osmBottomSheet.waypoint_media_image_delete_btn.alpha = 0.5f
                osmBottomSheet.waypoint_media_image_delete_btn.isEnabled = false
            }
        } else {
            if (state == true) {
                osmBottomSheet.waypoint_media_image_delete_btn.alpha = 1.0f
                osmBottomSheet.waypoint_media_image_delete_btn.isEnabled = true
            } else {
                osmBottomSheet.waypoint_media_image_delete_btn.alpha = 0.5f
                osmBottomSheet.waypoint_media_image_delete_btn.isEnabled = false
            }
        }
    }


    private fun imageBig(imagePath: String) {
        osmBottomSheet.displayImage(imagePath)
    }

    companion object {

        lateinit var osmBottomSheet : OsmBottomSheet

        fun getInstance(callingFragment: OsmBottomSheet) : ImageService {
            this.osmBottomSheet = callingFragment

            return ImageService()
        }
    }

}