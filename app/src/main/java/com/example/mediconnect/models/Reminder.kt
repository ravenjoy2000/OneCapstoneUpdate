package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class Reminder(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val times: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    // ✅ Helpers for cleaner UI code
    fun isTaken() = status == STATUS_TAKEN
    fun isMissed() = status == STATUS_MISSED
    fun isPending() = status == STATUS_PENDING

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: STATUS_PENDING,
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(dosage)
        parcel.writeString(frequency)
        parcel.writeString(times)
        parcel.writeString(status)
        parcel.writeLong(createdAt)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Reminder> {
        const val STATUS_PENDING = "pending"
        const val STATUS_TAKEN = "taken"
        const val STATUS_MISSED = "missed"

        override fun createFromParcel(parcel: Parcel): Reminder = Reminder(parcel)
        override fun newArray(size: Int): Array<Reminder?> = arrayOfNulls(size)
    }
}
