package com.example.mediconnect.models

data class Reminder(
    val drugName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val nextReminder: String = ""   // Example: "Today at 8:00 PM"
)
