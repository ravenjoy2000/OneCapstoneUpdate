package com.example.mediconnect.models

data class MedicalLog(
    val date: String = "",
    val status: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    var rating: Float = 0f,
    var comment: String = ""
)
