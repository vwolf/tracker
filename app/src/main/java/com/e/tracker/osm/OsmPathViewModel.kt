package com.e.tracker.osm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Job

class OsmPathViewModel (application: Application) : AndroidViewModel(application) {

    private var viewModelJob = Job()
}