package com.e.tracker.osm

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.e.tracker.MainActivity
import com.e.tracker.R
import java.lang.Exception


/**
 * Switch location tracking on/off using a Foreground Service
 *
 */
class LocationTrackingService : Service() {
    private val TAG = "LocationTrackingService"
    private var notification : Notification? = null
    var locationManager: LocationManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "LocationTrackingService onStartCommand, $startId")

        if (notification == null) {

        }
        if (intent == null) {
            return START_STICKY_COMPATIBILITY
        }

        startForeground(1, defaultNotification(applicationContext))
        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LocationTrackingService onCreate")

        if (locationManager == null) {
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        try {
            val ll0 = locationListeners[0]
            val ll1 = locationListeners[1]
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL, DISTANCE, locationListeners[1])
        } catch (e: SecurityException) {
            Log.e(TAG, "Fail to request location update", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Network provider does not exist", e)
        }

        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, locationListeners[0])
        } catch (e: SecurityException) {
            Log.e(TAG, "Fail to request location update", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "GPS provider does not exist", e)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "LocationTrackingService onDestroy")

        if (locationManager != null) {
            for (locationListener in locationListeners) {
                try {
                    locationManager?.removeUpdates(locationListener)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove location listener")
                }
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "LocationTrackingService onBind")
        return null
    }

    companion object {

        val TAG = "LocationTrackingService"
        var INTERVAL = 1000L
        var DISTANCE = 10.0f

        var currentContext: Context? = null


        var updatecallback : (Location) -> Unit = { lat: Location -> lat}

        val locationListeners = arrayOf(
            LTRLocationListener(LocationManager.GPS_PROVIDER),
            LTRLocationListener(LocationManager.NETWORK_PROVIDER)
        )

        fun startService(context: Context, callback: (Location) -> Unit) {
            updatecallback = callback

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            // get DISTANCE from preferences
            val distanceDefault = context.getString(R.string.gpx_tracking_default)
            val distance = prefs.getString("gpx_distance", distanceDefault)
            DISTANCE = distance.toFloat()
            // get INTERVAL from preferences
            val intervalDefault =  context.getString(R.string.gpx_interval_default)
            val interval = prefs.getString("gpx_interval", intervalDefault)
            INTERVAL = interval.toLong() * 1000

            val startIntent = Intent(context, LocationTrackingService::class.java)
            //startIntent.putExtra("extra", "extra")
           // startService(startIntent)
            currentContext = context
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationTrackingService::class.java)
            context.stopService(stopIntent)
        }



        fun defaultNotification(context: Context): Notification {

            createNotificationChannel(context)
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
            context,
            0, notificationIntent, 0
        )

            val notification = NotificationCompat.Builder(context, TAG)
                .setContentTitle(TAG)
                .build()

            return notification
        }


        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serviceChannel = NotificationChannel(
                    TAG, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                manager!!.createNotificationChannel(serviceChannel)
            }
        }


        class LTRLocationListener(provider: String) : LocationListener {
            val lastLocation = Location(provider)

            override fun onLocationChanged(location: Location?) {
                lastLocation.set(location)
                Log.i(TAG, "lastLocation: $lastLocation distance: $DISTANCE, interval: $INTERVAL")

                updatecallback( lastLocation )

                Toast.makeText(
                    currentContext,
                    "$lastLocation",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onProviderDisabled(provider: String?) {
            }

            override fun onProviderEnabled(provider: String?) {
                Toast.makeText(
                    currentContext,
                    "onProviderEnabled",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }
        }
    }
}

