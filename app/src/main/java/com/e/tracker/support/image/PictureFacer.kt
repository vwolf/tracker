package com.e.tracker.support.image

import android.os.Parcel
import android.os.Parcelable

class PictureFacer(
    var pictureName: String? = null,
    var picturePath: String? = null,
    var pictureSize: String? = null,
    var imageUri: String? = null) : Parcelable {



    var selected = false
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()) {}


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pictureName)
        parcel.writeString(picturePath)
        parcel.writeString(pictureSize)
        parcel.writeString(imageUri)
    }

    override fun describeContents(): Int {
        return 0
    }


    companion object {
        @JvmField
        val CREATOR = object:  Parcelable.Creator<PictureFacer>
        {
            override fun createFromParcel(parcel: Parcel): PictureFacer {
                return PictureFacer(parcel)
            }

            override fun newArray(size: Int): Array<PictureFacer?> {
                return arrayOfNulls(size)
            }
        }
    }

}
//class PictureFacer : Parcelable {
//
//    var pictureName: String? = null
//    var picturePath: String? = null
//    var pictureSize: String? = null
//    var imageUri: String? = null
//    var selected = false
//
//    constructor() {}
//
//    constructor(
//        pictureName: String?,
//        picturePath: String?,
//        pictureSize: String?,
//        imageUri: String?
//    ) {
//        this.pictureName = pictureName
//        this.picturePath = picturePath
//        this.pictureSize = pictureSize
//        this.imageUri = imageUri
//    }
//
//    constructor(parcel: Parcel) : this(
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readString()
//    )
//
//
//    override fun writeToParcel(parcel: Parcel?, flags: Int) {
//        parcel.writeString(pictureName)
//        parcel.writeString(picturePath)
//        parcel.writeString(pictureSize)
//        parcel.writeString(imageUri)
//    }
//
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR: Parcelable.Creator<PictureFacer> {
//        override fun createFromParcel(parcel: Parcel?): PictureFacer {
//            return PictureFacer(parcel)
//        }
//
//        override fun newArray(size: Int): Array<PictureFacer> {
//            return arrayOfNulls(size)
//        }
//    }
//
//}