package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class FeedbackModel(
    val rating: Float = 0f,
    val comment: String = "",
    val patientName: String = "", // ✅ Comma added here
    val timestamp: Long? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Float::class.java.classLoader) as? Float ?: 0f,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(rating)
        parcel.writeString(comment)
        parcel.writeString(patientName)
        parcel.writeValue(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FeedbackModel> {
        override fun createFromParcel(parcel: Parcel): FeedbackModel {
            return FeedbackModel(parcel)
        }

        override fun newArray(size: Int): Array<FeedbackModel?> {
            return arrayOfNulls(size)
        }
    }
}
