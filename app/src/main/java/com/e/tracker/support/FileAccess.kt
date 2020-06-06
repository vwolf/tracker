package com.e.tracker.support

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat
import java.io.File

/**
 * Get files from storage for this app
 *
 * @param context
 * @param showHiddenFiles
 * @param fileType
 * @param onlyFiles
 */
fun getTracksFromFilesDir(
    context: Context,
    showHiddenFiles: Boolean = false,
    fileType: String = "gpx",
    onlyFiles: Boolean = true): List<File> {

    var pathList = mutableListOf<File>()
    var filesList = mutableListOf<File>()

    // get path's to external storage directoryies
    val state = Environment.getExternalStorageState()
    if ( Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
        val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)

        for (filePath in externalFilesDirs) {
            pathList.add(filePath)
        }
    }

    // walk each directory and get all files of fileType
    if (pathList.isNotEmpty()) {
        for ( dir in pathList) {
            val dirToWalk = dir

            val filesInDirectory = dirToWalk.walk(FileWalkDirection.TOP_DOWN)
                .filter {showHiddenFiles || !it.name.startsWith(".")}
                .filter { onlyFiles && it.isFile }
                .filter { fileType.isNotEmpty() && it.extension.equals(fileType, ignoreCase = true) }
                .toList()

            filesList.addAll(filesInDirectory)
        }
    }
    return filesList
}


/**
 * Get files from external storage (sdcard)
 *
 * @param showHiddenFiles
 * @param fileType
 * @param onlyFiles
 */
fun getTracksFromStorage(
    directoryToSearch: String,
    showHiddenFiles: Boolean = false,
    fileType: String = "gpx",
    onlyFiles: Boolean = true): List<File>
 {

     var pathList = mutableListOf<File>()
     var filesList = mutableListOf<File>()

     if ( EnvironmentCompat.getStorageState(Environment.getExternalStorageDirectory()) == "mounted") {
         val sdCardPath = Environment.getExternalStorageDirectory().absolutePath
         val pathToFiles = "$sdCardPath/$directoryToSearch"

         if (File(pathToFiles).exists()) {
             val filesInPath = File(pathToFiles).listFiles()
             for ( entry in filesInPath ) {
                 val dirToWalk = entry

                 val filesInDirectory = dirToWalk.walk(FileWalkDirection.TOP_DOWN)
                     .filter {showHiddenFiles || !it.name.startsWith(".")}
                     .filter { onlyFiles && it.isFile }
                     .filter { fileType.isNotEmpty() && it.extension.equals(fileType, ignoreCase = true) }
                     .toList()

                 filesList.addAll(filesInDirectory)
             }
         }
     }

     return filesList
}