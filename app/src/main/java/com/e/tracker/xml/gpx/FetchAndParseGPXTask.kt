package com.e.tracker.xml.gpx


import android.os.AsyncTask
import com.e.tracker.xml.gpx.domain.Gpx
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class FetchAndParseGPXTask( val mGpxUrl: String, listener: GpxFetchedAndParsed) :
    AsyncTask<Void?, Void?, Gpx?>() {

    val mListener: GpxFetchedAndParsed
    val mParser: GPXParser = GPXParser()

//    override protected fun doInBackground(vararg unused: Void): Gpx? {
//        var parsedGpx: Gpx? = null
//        try {
//            val url = URL(mGpxUrl)
//            val client =
//                url.openConnection() as HttpURLConnection
//            val input : InputStream = BufferedInputStream(client.inputStream)
//            parsedGpx = mParser.parse(input)
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } catch (e: XmlPullParserException) {
//            e.printStackTrace()
//        }
//        return parsedGpx
//    }

    override fun onPostExecute(gpx: Gpx?) {
        mListener.onGpxFetchedAndParsed(gpx)
    }

    init {
        mListener = listener
    }

    override fun doInBackground(vararg p0: Void?): Gpx? {
        var parsedGpx: Gpx? = null

        try {
            val url = URL(mGpxUrl)
            val client = url.openConnection() as HttpURLConnection
            val input : InputStream = BufferedInputStream(client.inputStream)
            parsedGpx = mParser.parse(input)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }

        return parsedGpx
    }
}