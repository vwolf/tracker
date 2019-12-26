package com.e.tracker.xml.gpx

import com.e.tracker.xml.gpx.domain.Gpx

interface GpxFetchedAndParsed {
    fun onGpxFetchedAndParsed(gpx: Gpx?)
}