package com.example.mediconnect.patient

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
        val tvPreviousDate: TextView = view.findViewById(R.id.tv_previous_date) // ✅ Must exist in XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.tvDoctorName.text = "Dr. ${appointment.doctorName}"
        holder.tvStatus.text = "Status: ${formatStatus(appointment.status)}"
        holder.tvDate.text = "Date: ${appointment.date}"
        holder.tvTime.text = "Time: ${appointment.time}"
        holder.tvMode.text = "Mode: ${appointment.mode}"
        holder.tvLocation.text = "Location: ${appointment.location}"
        holder.tvNote.text = "Note: ${appointment.note}"

        holder.tvReason.text = if (appointment.reason.isNotBlank())
            "Reason: ${appointment.reason}" else ""

        if (appointment.previousDate.isNotBlank()) {
            holder.tvPreviousDate.visibility = View.VISIBLE
            holder.tvPreviousDate.text = "Rescheduled from: ${appointment.previousDate}"
        } else {
            holder.tvPreviousDate.visibility = View.GONE
        }

        holder.tvStatus.setTextColor(
            when (appointment.status.lowercase()) {
                "cancelled" -> holder.itemView.context.getColor(android.R.color.holo_red_dark)
                "rescheduled", "rescheduled_once" -> holder.itemView.context.getColor(android.R.color.holo_orange_dark)
                "completed" -> holder.itemView.context.getColor(android.R.color.holo_green_dark)
                "no_show" -> holder.itemView.context.getColor(android.R.color.darker_gray)
                else -> holder.itemView.context.getColor(android.R.color.black)
            }
        )
    }

    override fun getItemCount(): Int = appointments.size

    private fun formatStatus(status: String): String {
        return status.replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
    }
}