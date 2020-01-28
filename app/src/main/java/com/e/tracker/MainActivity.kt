package com.e.tracker

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.e.tracker.Support.*
import com.e.tracker.Support.Permissions
import com.e.tracker.databinding.ActivityMainBinding
import com.e.tracker.osm.OSM_LOG


class MainActivity : AppCompatActivity(){
    // , TrackEditDialogFragment.TrackEditDialogListener
    //override fun onDialogCancelClick(dialog: DialogFragment) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Permission stuff, not ready yet
        //Permissions(applicationContext, this).requestReadWritePermission()

//        val mediaPermissions =  arrayOf( Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO )
//        Permissions(applicationContext, this).requestPermissions(mediaPermissions, PERMISSIONS_REQUEST_CAMERA_AUDIO)

        val storagePermissions = arrayOf( Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        Permissions(applicationContext, this).requestPermissions(storagePermissions, PERMISSIONS_REQUEST_READWRITE_EXTERNAL)

        getLocalImagePaths()
    }

    // put the option on screen
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        menu?.findItem(R.id.menu_showWaypoints)?.isVisible = false
        //invalidateOptionsMenu()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_help -> {
                println("Menu item Help selected")
                return true
            }

            R.id.menu_showWaypoints -> {
                if (item.isChecked)
                    item.isChecked = false
                else
                    item.isChecked = true

                return true
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }

        return false
        //return super.onOptionsItemSelected(item)
    }

    fun getLocalImagePaths() {
        val result = mutableListOf<Uri>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        contentResolver.query(uri, projection, null, null, null)?.use {
            while (it.moveToNext()) {
                result.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        it.getLong(0)
                    )
                )
            }
        }
        Log.i(OSM_LOG, "getLocalImagePaths.result: ${result.size}")
        for (r in result) {
            Log.i(OSM_LOG, "$r")
        }
    }
}
