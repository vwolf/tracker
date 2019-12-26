package com.e.tracker.Support

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat



const val PERMISSIONS_REQUEST_INTERNET = 100
const val PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE = 101

const val PERMISSIONS_REQUEST_READ_EXTERNAL = 103
const val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104
const val PERMISSIONS_REQUEST_READWRITE_EXTERNAL = 105

const val PERMISSIONS_REQUEST_RECORD_AUDIO = 106
const val PERMISSIONS_REQUEST_CAMERA = 107
const val PERMISSIONS_REQUEST_CAMERA_AUDIO = 109

const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 110


class Permissions(val context: Context, val activity: AppCompatActivity) :  ActivityCompat.OnRequestPermissionsResultCallback {


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            println("Permission granted: $requestCode")
        } else {
            println("Permission not granted: $requestCode")
        }

    }


    fun requestReadWritePermission() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            println("Permission denied READ_EXTERNAL_STORAGE")
            makeRequest(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_READ_EXTERNAL)
        } else {
            println("Permission granted READ_EXTERNAL_STORAGE")
        }
    }


    /**
     * Request multiply permissions
     *
     * @param permissionsToRequest
     * @param requestCode
     */
    fun requestPermissions(permissionsToRequest: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode)
    }

    /**
     * Request single permission
     *
     * @param requestPermission
     * @param requestCode see top of file for values
     */
    private fun makeRequest(requestPermission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(requestPermission),
            requestCode)
    }


}