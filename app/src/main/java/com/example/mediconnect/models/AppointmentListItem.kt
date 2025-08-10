package com.example.mediconnect.models

sealed  class AppointmentListItem{
    data class Header(val label: String, val date: String) : AppointmentListItem()
    data class AppointmentItem(val appointment: Appointment) : AppointmentListItem()
}