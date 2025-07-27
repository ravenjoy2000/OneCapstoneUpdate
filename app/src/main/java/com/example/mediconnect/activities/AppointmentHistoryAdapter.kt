package com.example.mediconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment

class AppointmentHistoryAdapter(private val appointments: List<Appointment>) :
    RecyclerView.Adapter<AppointmentHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tv_doctor_name)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvMode: TextView = view.findViewById(R.id.tv_mode)
        val tvLocation: TextView = view.findViewById(R.id.tv_location)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val tvReason: TextView = view.findViewById(R.id.tv_reason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.tvDoctorName.text = "Dr. ${appointment.doctorName}"
        holder.tvStatus.text = "Status: ${appointment.status}"
        holder.tvDate.text = "Date: ${appointment.date}"
        holder.tvTime.text = "Time: ${appointment.time}"
        holder.tvMode.text = "Mode: ${appointment.mode}"
        holder.tvLocation.text = "Location: ${appointment.location}"
        holder.tvNote.text = "Note: ${appointment.note}"
        holder.tvReason.text = "Reason: ${appointment.reason}"
    }

    override fun getItemCount(): Int = appointments.size
}
