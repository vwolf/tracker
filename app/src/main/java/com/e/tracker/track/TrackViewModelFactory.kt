package com.e.tracker.track

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.e.tracker.database.TrackCoordDatabaseDao
import com.e.tracker.database.TrackDatabaseDao
import java.lang.IllegalArgumentException



class TrackViewModelFactory (
    private val dataSource: TrackDatabaseDao,
    private val coordsSource: TrackCoordDatabaseDao,
    private val application: Application) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>) : T {
        if (modelClass.isAssignableFrom(TrackViewModel::class.java)) {
            return TrackViewModel(dataSource, coordsSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}