package com.e.tracker

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.e.tracker.Support.*
import com.e.tracker.Support.Permissions
import com.e.tracker.databinding.ActivityMainBinding
import com.e.tracker.track.TrackEditDialogFragment


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
//
        val storagePermissions = arrayOf( Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        Permissions(applicationContext, this).requestPermissions(storagePermissions, PERMISSIONS_REQUEST_READWRITE_EXTERNAL)


        val networkPermissions = arrayOf( Manifest.permission.ACCESS_NETWORK_STATE)
        Permissions(applicationContext, this).requestPermissions(networkPermissions, PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE)

        val gpsPermissions = arrayOf( Manifest.permission.ACCESS_FINE_LOCATION)
        Permissions(applicationContext, this).requestPermissions(gpsPermissions, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION )
    }

    // put the option on screen
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_help -> {
                println("Menu item Help selected")
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
}
