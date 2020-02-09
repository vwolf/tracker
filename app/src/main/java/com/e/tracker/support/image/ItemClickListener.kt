package com.e.tracker.support.image

interface ItemClickListener {

    fun onPicClicked(holder: PicHolder, position: Int, pics: ArrayList<PictureFacer>)
    fun onPicClicked(pictureFolderPath: String, folderName: String)
}