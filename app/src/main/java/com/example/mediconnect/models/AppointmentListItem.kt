package com.example.mediconnect.models

// Sealed class na ginagamit bilang parent para sa iba't ibang item sa appointment list
sealed class AppointmentListItem {

    // Data class para sa header ng appointment list, may label at date na ipapakita
    data class Header(val label: String, val date: String) : AppointmentListItem()

    // Data class para sa isang appointment item, naglalaman ng Appointment object
    data class AppointmentItem(val appointment: Appointment) : AppointmentListItem()
}
