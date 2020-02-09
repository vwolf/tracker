package com.e.tracker.support.image

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.e.tracker.R


class RecyclerViewPagerImageIndicator(
    pictureList: ArrayList<PictureFacer>,
    pictureContx: Context,
    imageListerner: ImageIndicatorListener
) :
    RecyclerView.Adapter<IndicatorHolder>() {
    var pictureList: ArrayList<PictureFacer>
    var pictureContx: Context
    private val imageListerner: ImageIndicatorListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndicatorHolder {
        val inflater = LayoutInflater.from(parent.context)
        val cell = inflater.inflate(R.layout.indicator_holder, parent, false)
        return IndicatorHolder(cell)
    }

    override fun onBindViewHolder(holder: IndicatorHolder, position: Int) {
        val pic: PictureFacer = pictureList[position]
        holder.positionController.setBackgroundColor(
            if (pic.selected) {
                Color.parseColor("#00000000")
            } else {
                Color.parseColor("#8c000000")
            })
//            if (pic.selected.Color.parseColor("#00000000") else Color.parseColor(
//                "#8c000000"
//            )

        Glide.with(pictureContx)
            .load(pic.picturePath)
            .apply(RequestOptions().centerCrop())
            .into(holder.image)
        holder.image.setOnClickListener(View.OnClickListener {
            //holder.card.setCardElevation(5);
            pic.selected = true
            notifyDataSetChanged()
            imageListerner.onImageIndicatorClicked(position)
        })

    }

    override fun getItemCount(): Int {
        return pictureList.size
    }

    /**
     *
     * @param pictureList ArrayList of pictureFacer objects
     * @param pictureContx The Activity of fragment context
     * @param imageListerner Interface for communication between adapter and fragment
     */
    init {
        this.pictureList = pictureList
        this.pictureContx = pictureContx
        this.imageListerner = imageListerner
    }
}