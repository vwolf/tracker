package com.e.tracker.support.image

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.e.tracker.R


class PicHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var picture: ImageView

//    constructor(parcel: Parcel) : this (
//        parcel.writeValue(picture)
//    )

    init {
        picture = itemView.findViewById(R.id.image)
    }

//    override fun writeToParcel(dest: Parcel?, flags: Int) {
//        dest?.writeValue(picture)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }

//    companion object {
//        @JvmField
//        val CREATOR = object : Parcelable.Creator<PicHolder> {
//            override fun createFromParcel(source: Parcel): PicHolder {
//                return PicHolder(source)
//            }
//        }
//
//    }
}


//class PictureHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//    var picture: ImageView
//
//    constructor(itemView: View) : this {
//
//    }
////    init {
////        picture = itemView.findViewById(R.id.image)
////    }
//}

