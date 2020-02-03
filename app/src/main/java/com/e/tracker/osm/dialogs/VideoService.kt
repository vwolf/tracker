package com.e.tracker.osm.dialogs

import android.app.AlertDialog
import android.content.Intent
import android.provider.MediaStore
import android.widget.VideoView
import com.e.tracker.osm.WAYPOINT_VIDEOFROMGALLERY


/**
 * Load video from movies
 * Record video with camera and add to movies
 *
 */
class VideoService {

    lateinit var currentVideoPath: String

    var videosToDelete = mutableListOf<VideoView>()

    /**
     * Dialog to choose where to get the video from
     *
     */
    fun showVideoDialog() {
        val videoDialog = AlertDialog.Builder(osmBottomSheet.requireContext())
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


    private fun chooseVideoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        osmBottomSheet.startActivityForResult(galleryIntent, WAYPOINT_VIDEOFROMGALLERY)
    }

    private fun takeVideoWithCamera() {}


    companion object {
        lateinit var osmBottomSheet: OsmBottomSheet

        fun getInstance(osmBottomSheet: OsmBottomSheet) : VideoService {
            this.osmBottomSheet = osmBottomSheet

            return VideoService()
        }
    }
}