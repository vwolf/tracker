package com.e.tracker.support.image

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat.setTransitionName
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.e.tracker.R
import com.e.tracker.generated.callback.OnClickListener
import com.e.tracker.osm.OSM_LOG


class PictureAdapter(
    pictureList: ArrayList<PictureFacer>,
    pictureContx: Context,
    picListerner: ItemClickListener
) :
    RecyclerView.Adapter<PicHolder>() {

    private val pictureList: ArrayList<PictureFacer>
    private val pictureContx: Context
    private val picListerner: ItemClickListener

    lateinit var picHolder: PicHolder


    override fun onCreateViewHolder(container: ViewGroup, position: Int): PicHolder {
        val inflater = LayoutInflater.from(container.context)
        val cell: View = inflater.inflate(R.layout.picture_holder_item, container, false)
        picHolder = PicHolder(cell)

        return picHolder
    }


    override fun onBindViewHolder(holder: PicHolder, position: Int) {
        val image: PictureFacer = pictureList[position]
        Glide.with(pictureContx)
            .load(image.picturePath)
            .apply(RequestOptions().centerCrop())
            .into(holder.picture)
        setTransitionName(
            holder.picture,
            position.toString() + "_image"
        )

        holder.picture.setOnClickListener {
            Log.i(OSM_LOG, "Click on picture")

            picListerner.onPicClicked(holder, position, pictureList)
        }

//        holder.picture.setOnClickListener(object : OnClickListener() {
//            override fun onClick(v: View?) {
//                picListerner.onPicClicked(holder, position, pictureList)
//            }
//        })
    }

    override fun getItemCount(): Int {
        return pictureList.size
    }

    /**
     *
     * @param pictureList ArrayList of pictureFacer objects
     * @param pictureContx The Activities Context
     * @param picListerner An interface for listening to clicks on the RecyclerView's items
     */
    init {
        this.pictureList = pictureList
        this.pictureContx = pictureContx
        this.picListerner = picListerner
    }
}