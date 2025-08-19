package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class MedicalLog(
    val medicalLogId: String? = null,
    val patientName: String? = null,
    val appointmentDate: Timestamp? = null,
    val diagnosis: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val doctorNotes: String? = null,
    val date: String? = null,
    val doctorName: String? = null,
    val doctorId: String? = null,
    val patientId: String? = null,
    val appointmentId: String? = null,
    val appointmentTime: String? = null,
    val appointmentDay: String? = null,
    val appointmentMonth: String? = null,
    val appointmentYear: String? = null,
    val appointmentHour: String? = null,
    val appointmentMinute: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readLong().let {
            if (it == -1L) null else Timestamp(it, 0) // ✅ reconstruct Timestamp
        },
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(medicalLogId)
        parcel.writeString(patientName)
        parcel.writeLong(appointmentDate?.seconds ?: -1L) // ✅ safely write Timestamp as seconds
        parcel.writeString(diagnosis)
        parcel.writeString(notes)
        parcel.writeString(status)
        parcel.writeString(doctorNotes)
        parcel.writeString(date)
        parcel.writeString(doctorName)
        parcel.writeString(doctorId)
        parcel.writeString(patientId)
        parcel.writeString(appointmentId)
        parcel.writeString(appointmentTime)
        parcel.writeString(appointmentDay)
        parcel.writeString(appointmentMonth)
        parcel.writeString(appointmentYear)
        parcel.writeString(appointmentHour)
        parcel.writeString(appointmentMinute)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MedicalLog> {
        override fun createFromParcel(parcel: Parcel): MedicalLog = MedicalLog(parcel)
        override fun newArray(size: Int): Array<MedicalLog?> = arrayOfNulls(size)
    }
}
