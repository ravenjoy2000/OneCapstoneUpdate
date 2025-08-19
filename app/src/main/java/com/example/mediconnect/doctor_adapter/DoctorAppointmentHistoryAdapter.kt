package com.example.mediconnect.doctor_adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoctorAppointmentHistoryAdapter (
    private val appointments: List<Appointment>,
    private val onRescheduleClick: (Appointment) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentHistoryAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvAppointmentDate: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnReschedule: Button = itemView.findViewById(R.id.btnReschedule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun getItemCount() = appointments.size

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.tvPatientName.text = appointment.patientName

        // Format dateTime
        if (appointment.dateTime != 0L) {
            val date = Date(appointment.dateTime)
            val format = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            holder.tvAppointmentDate.text = format.format(date)
        } else {
            holder.tvAppointmentDate.text = "${appointment.date} ${appointment.time}"
        }

        holder.tvStatus.text = appointment.status

        // Show reschedule only if cancelled or late
        holder.btnReschedule.visibility =
            if (appointment.status in listOf("Late", "Cancelled")) View.VISIBLE else View.GONE

        holder.btnReschedule.setOnClickListener {
            onRescheduleClick(appointment)
        }

        // Change color based on status
        holder.tvStatus.setTextColor(
            when (appointment.status) {
                "Complete" -> Color.GREEN
                "Late" -> Color.YELLOW
                "No-show" -> Color.RED
                "Cancelled" -> Color.GRAY
                else -> Color.BLACK
            }
        )
    }

}