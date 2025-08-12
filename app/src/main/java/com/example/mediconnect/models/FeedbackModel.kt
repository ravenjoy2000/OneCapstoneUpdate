package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa Feedback na pwedeng i-pass sa intents gamit Parcelable
data class FeedbackModel(
    val rating: Float = 0f,              // Rating na ibinigay ng pasyente (0 hanggang 5)
    val comment: String = "",            // Komento o feedback ng pasyente
    val patientName: String = "",        // Pangalan ng pasyente na nagbigay ng feedback
    val timestamp: Long? = null          // Timestamp kung kailan ginawa ang feedback (optional)
) : Parcelable {

    // Constructor para i-create ang object mula sa Parcel (Parcelable implementation)
    constructor(parcel: Parcel) : this(
        parcel.readValue(Float::class.java.classLoader) as? Float ?: 0f,  // Basa ng rating mula sa parcel
        parcel.readString() ?: "",                                        // Basa ng comment mula sa parcel
        parcel.readString() ?: "",                                        // Basa ng patientName mula sa parcel
        parcel.readValue(Long::class.java.classLoader) as? Long          // Basa ng timestamp mula sa parcel (nullable)
    )

    // Isinusulat ang mga fields papunta sa Parcel para maipasa sa Intent/Bundle
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(rating)           // Isinusulat ang rating
        parcel.writeString(comment)         // Isinusulat ang comment
        parcel.writeString(patientName)     // Isinusulat ang patientName
        parcel.writeValue(timestamp)        // Isinusulat ang timestamp (nullable)
    }

    // Required override para sa Parcelable, wala itong special contents kaya 0 lang
    override fun describeContents(): Int = 0

    // Companion object para sa Parcelable.Creator na gumagawa ng FeedbackModel mula sa Parcel
    companion object CREATOR : Parcelable.Creator<FeedbackModel> {
        override fun createFromParcel(parcel: Parcel): FeedbackModel {
            return FeedbackModel(parcel)    // Gumagawa ng FeedbackModel mula sa Parcel
        }

        override fun newArray(size: Int): Array<FeedbackModel?> {
            return arrayOfNulls(size)       // Gumagawa ng array ng FeedbackModel na nullable
        }
    }
}
