package com.e.tracker.osm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
//import androidx.media.app.NotificationCompat
import com.e.tracker.MainActivity
import com.e.tracker.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.lang.Exception
import java.lang.IllegalArgumentException


class LocationService {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var location : MutableLiveData<Location> = MutableLiveData()

    fun getInstance(appContext: Context) : FusedLocationProviderClient {
        if (fusedLocationProviderClient == null)
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)

        return fusedLocationProviderClient!!
    }

    fun getLocation() : LiveData<Location> {
        fusedLocationProviderClient?.lastLocation?.addOnSuccessListener { loc: Location? ->
            location.value = loc
        }

        return location
    }

}

class LocationServiceTwo(val locationChangedSubscription: (Location) -> Unit) : LocationListener {

    private var locationManager : LocationManager ?= null

//    init {
//        if (locationManager == null) {
//            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        }
//    }


    override fun onLocationChanged(location : Location) {
        locationChangedSubscription(location)
    }

    override fun onProviderEnabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


//class LocationTrackingService : Service() {
//
//    var locationManager: LocationManager? = null
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null
//    }
//
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        //super.onStartCommand(intent, flags, startId)
//
//
//        return START_STICKY
//    }
//
//
//    override fun onCreate() {
//        if (locationManager == null) {
//            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        }
//
//        try {
//            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL, DISTANCE, locationListeners[1])
//        } catch (e: SecurityException) {
//            Log.e(TAG, "Fail to request location update", e)
//        } catch (e: IllegalArgumentException) {
//            Log.e(TAG, "Network provider does not exist", e)
//        }
//
//        try {
//            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, locationListeners[0])
//        } catch (e: SecurityException) {
//            Log.e(TAG, "Fail to request location update", e)
//        } catch (e: IllegalArgumentException) {
//            Log.e(TAG, "GPS provider does not exist", e)
//        }
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (locationManager != null) {
//            for (locationListener in locationListeners) {
//                try {
//                    locationManager?.removeUpdates(locationListener)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Failed to remove location listeners")
//                }
//            }
//        }
//    }
//
//
//    companion object {
//        val TAG = "LocationTrackingService"
//        val INTERVAL = 1000L
//        val DISTANCE = 10.0f
//
//        val locationListeners = arrayOf(
//            LTRLocationListener(LocationManager.GPS_PROVIDER),
//            LTRLocationListener(LocationManager.NETWORK_PROVIDER)
//        )
//
//        fun startService(context: Context) {
//            val startIntent = Intent(context, LocationTrackingService::class.java)
//            //ContextCompat.startLocationTrackingService(context, startIntent)
//        }
//
//        fun stopService(context: Context) {}
//
//        class LTRLocationListener(provider: String) : android.location.LocationListener {
//
//            val lastLocation = Location(provider)
//
//            override fun onLocationChanged(location: Location?) {
//                lastLocation.set(location)
//                //lcs(lastLocation)
//            }
//
//            override fun onProviderDisabled(provider: String?) {
//            }
//
//            override fun onProviderEnabled(provider: String?) {
//            }
//
//            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//            }
//        }
//    }
//
//}

class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // do work on background thread
        val input = intent?.getStringExtra("inputExtra")
        createNotifictionChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service Kotlin Example")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_add_circle_black_24dp)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotifictionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)

            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }


    companion object {

        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }
}