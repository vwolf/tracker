//package com.e.tracker.xml.gpx
//
//
//
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.TextView
//import com.e.tracker.R
//import devwolf.ontrack.fileIO.ARG_FILE
//import com.e.tracker.xml.gpx.domain.Gpx
//import com.e.tracker.xml.gpx.domain.Track
//import com.e.tracker.xml.gpx.domain.TrackSegment
//import kotlinx.android.synthetic.main.activity_gpxparser.*
//import java.io.File
//import java.io.InputStream
//import java.lang.Exception
//
//class GPXParserActivity : AppCompatActivity() {
//
//    val mParser : GPXParser = GPXParser()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_gpxparser)
//
//        val filePath: String = intent.getSerializableExtra(ARG_FILE) as String
//
//        val textView: TextView = findViewById(R.id.gpx_parser_textView)
//        textView.text = filePath
//
//        val startParsingButton : Button = findViewById(R.id.gpx_parser_start)
//
//        startParsingButton.setOnClickListener{
//            getInputStream(filePath)
//        }
//    }
//
//    fun getInputStream(filePath: String) : Boolean {
//        var inputStream: InputStream? = null
//
//        var parsedGpx : Gpx
//
//        try {
//            inputStream = File(filePath).inputStream()
//            parsedGpx = mParser.parse(inputStream)
//            logGpx(parsedGpx)
//
//        } catch (e: Exception) {
//            println(e.toString())
//        } finally {
//
//        }
//        return true
//    }
//
//
//    //
//    fun logGpx(parsedGpx: Gpx) {
//
//        var trackProperties : String = ""
//
//        val tracks = parsedGpx.tracks
//
//        trackProperties += "Tracks: ${tracks.size}\n"
//
//        for ( (index, track) in tracks.withIndex()) {
//            val t = track as Track
//            val trackName = t.trackName
//            trackProperties += "Track name: $trackName \n"
//            println("trackName of track $index: $trackName")
//
//            // segments?
//            val trksegList = t.trackSegments
//            for ((index, segment) in trksegList.withIndex()) {
//                val s = segment as TrackSegment
//                trackProperties += "Trackpoints: ${s.trackPoints.size}\n"
//            }
//
//        }
//
//        gpx_parser_track_info.text = trackProperties
//    }
//}
