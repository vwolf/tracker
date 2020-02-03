package com.e.tracker.Support.image

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.e.tracker.R
import com.e.tracker.osm.OSM_LOG
import kotlinx.android.synthetic.main.activity_image_display.*

class ImageDisplay : AppCompatActivity() {

    lateinit var imagePath: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_display)

        // IMAGESPATH is an arrayList<String> with image paths
        val bundle = intent.extras
        //if (bundle?.getString("PATH") != null) {
        val  aimagePath = bundle?.getStringArrayList("IMAGESPATH")

        if (aimagePath != null) {
            imagePath = aimagePath
        }

        Log.i(OSM_LOG, "ImageDisplay.imagePath: $imagePath")
        loadImage(imagePath[0])

        //}
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {


        return super.onCreateView(name, context, attrs)
    }


    fun loadImage(path: String) {
        Log.i(OSM_LOG, "loadImage path: $path")

        val bitmap = BitmapFactory.decodeStream( contentResolver.openInputStream(path.toUri()))

        image_view.setImageBitmap(bitmap)
    }
}