package com.e.tracker.support.image

import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.e.tracker.R


class IndicatorHolder internal constructor(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var image: ImageView
    private val card: CardView
    var positionController: View

    init {
        image = itemView.findViewById(R.id.imageIndicator)
        card = itemView.findViewById(R.id.indicatorCard)
        positionController = itemView.findViewById(R.id.activeImage)
    }
}