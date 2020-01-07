package com.e.tracker.track

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import android.os.Environment
import java.io.File

/**
 * Access to tracks from external storage
 */
class Tracks {

    fun getTracksFromExternalStorage(
        context: Context,
        showHiddenFiles: Boolean = false,
        fileType: String = "gpx",
        onlyFiles: Boolean = true ) : List<File> {

        var filesList = mutableListOf<File>()
        var fileList = mutableListOf<File>()

        // get path's to external storage directoryies
        val state = Environment.getExternalStorageState()
        if( Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state ) {
            val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)

            for ( path in externalFilesDirs) {
                fileList.add(path)
            }
        }

        // now walk each directory and get all files of fileType
        if (fileList.isNotEmpty()) {

            for ( dir in fileList ) {
                val directory = dir

                val filesInDirectory = directory.walk(FileWalkDirection.TOP_DOWN)
                    .filter { showHiddenFiles || !it.name.startsWith(".")}
                    .filter { onlyFiles && it.isFile }
                    .filter { fileType.isNotEmpty() && it.extension.equals(fileType, ignoreCase = true)}
                    .toList()

                filesList.addAll(filesInDirectory)
            }
        }
        return filesList
    }

}