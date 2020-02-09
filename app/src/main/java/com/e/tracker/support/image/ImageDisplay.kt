package com.e.tracker.support.image

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.e.tracker.R
import com.e.tracker.osm.OSM_LOG


/**
 * This activity get a list of image path's and display the images
 * inside [PictureBrowserFragment]
 *
 */
class ImageDisplay : AppCompatActivity(), ItemClickListener {

    lateinit var imagePath: ArrayList<String>
    lateinit var imageRecycler: RecyclerView
    lateinit var imageRecyclerAdapter: PictureAdapter

    var position = 0

    var load: ProgressBar? = null

    var allPictures = ArrayList<PictureFacer>()

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

        position = bundle.getInt("POSITION")

        var picsList = ArrayList<PictureFacer>()
        picsList = intent.getParcelableArrayListExtra("pics")
        for (item in picsList) {
            Log.i(OSM_LOG, "pics: ${item.picturePath}")
        }

        imageRecycler = findViewById(R.id.recycler)
        //imageRecycler = findViewById(R.id.indicatorRecycler)
        //imageRecycler.addItemDecoration(ViewGroup.MarginLayoutParams(this))
        imageRecycler.hasFixedSize()

        load = findViewById(R.id.loader)

        if (picsList.isNotEmpty()) {
//            load?.visibility = View.VISIBLE
//            imageRecycler.adapter = PictureAdapter(picsList, this@ImageDisplay, this)
//            load?.visibility = View.GONE
            loadImages(position, picsList)
        }

//        if (imagePath.isNotEmpty()) {
//            load?.visibility = View.VISIBLE
//            allPictures = getAllImages(imagePath)
//            imageRecycler.adapter = PictureAdapter(allPictures, this@ImageDisplay, this)
//            //imageRecyclerAdapter = PictureAdapter(allPictures, this@ImageDisplay, this)
//
//            //val pc = imageRecycler.findContainingViewHolder(findViewById(R.id.recycler))
//            load?.visibility = View.GONE
//
//           // loadImages(imageRecyclerAdapter.picHolder, 0, allPictures)
//        }


        Log.i(OSM_LOG, "ImageDisplay.imagePath: $imagePath")
        //loadImage(imagePath[0])


        //}
    }

    override fun onStart() {
        val imageRecyclerAdapter = imageRecycler.adapter
        super.onStart()
    }

    override fun onResume() {
        val imageRecyclerAdapter = imageRecycler
        super.onResume()
    }
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
//        var imageRecyclerTemp = PictureAdapter(allPictures, this@ImageDisplay, this)
//        val picHolder = imageRecyclerTemp.picHolder
//
//        imageRecycler.adapter = PictureAdapter(allPictures, this@ImageDisplay, this)



//        loadImages(picHolder, 0, allPictures)

        return super.onCreateView(name, context, attrs)
    }



    /**
     *
     * @param holder The ViewHolder for the clicked picture
     * @param position The position in the grid of the picture that was clicked
     * @param pics An ArrayList of all the items in the Adapter
     */
    override fun onPicClicked (
        holder: PicHolder,
        position: Int,
        pics: ArrayList<PictureFacer>
    ): Unit {
        val browser: PictureBrowserFragment =
            PictureBrowserFragment.newInstance(pics, position, this@ImageDisplay)
        // Note that we need the API version check here because the actual transition classes (e.g. Fade)
        // are not in the support library and are only available in API 21+. The methods we are calling on the Fragment
        // ARE available in the support library (though they don't do anything on API < 21)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //browser.setEnterTransition(new Slide());
            //browser.setExitTransition(new Slide());
            // uncomment this to use slide transition and comment the two lines below
            browser.enterTransition = Fade()
            browser.exitTransition = Fade()
        }
        supportFragmentManager
            .beginTransaction()
            .addSharedElement(holder.picture, position.toString() + "picture")
            .add(R.id.displayContainer, browser)
            .addToBackStack(null)
            .commit()
    }


    override fun onPicClicked(pictureFolderPath: String, folderName: String) {

    }

    /**
     * This Method gets all the images in the folder paths passed as a String to the method and returns
     * and ArrayList of pictureFacer a custom object that holds data of a given image
     *
     * @param imagePathList list with path to imgages to load
     */
    fun getAllImages(imagePathList: ArrayList<String>) : ArrayList<PictureFacer> {
        var images = ArrayList<PictureFacer>()

        for ( imagePath in imagePathList) {
            var pictureFacer = PictureFacer()
            pictureFacer.picturePath = imagePath

            images.add(pictureFacer)
        }
        return images
    }


    fun loadImages(holder: PicHolder, position: Int, pics: ArrayList<PictureFacer>) {
        Log.i(OSM_LOG, "loadImages position: $position")

        val browser: PictureBrowserFragment = PictureBrowserFragment.newInstance(pics, position, this@ImageDisplay)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            browser.enterTransition = Fade()
            browser.exitTransition = Fade()
        }

        supportFragmentManager
            .beginTransaction()
            .addSharedElement(holder.picture, position.toString() + "picture")
            .add(R.id.displayContainer, browser)
            .addToBackStack(null)
            .commit()

        //val bitmap = BitmapFactory.decodeStream( contentResolver.openInputStream(path.toUri()))
        //image_view.setImageBitmap(bitmap)
    }


    fun loadImages(position: Int, pics: ArrayList<PictureFacer>) {
        Log.i(OSM_LOG, "loadImages position: $position")
        val browser: PictureBrowserFragment = PictureBrowserFragment.newInstance(pics, position, this@ImageDisplay)

        browser.enterTransition = Fade()
        browser.exitTransition = Fade()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.displayContainer, browser)
            .commit()
    }


    fun getAllImagesInList(imageList: List<String>) {

        var images = arrayListOf<PictureFacer>()
    }
}