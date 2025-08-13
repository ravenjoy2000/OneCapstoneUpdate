package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem
import java.util.Locale

// Adapter para sa RecyclerView na nagpapakita ng list ng appointment kasama ang mga header
class DoctorAppointmentAdapter(
    private val items: List<AppointmentListItem>,
    private val onItemClick: (Appointment) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APPOINTMENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AppointmentListItem.Header -> TYPE_HEADER
            is AppointmentListItem.AppointmentItem -> TYPE_APPOINTMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.doctor_item_appointment_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_APPOINTMENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.doctor_item_appointment, parent, false)
                AppointmentViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> {
                if (item is AppointmentListItem.Header) holder.bind(item)
            }
            is AppointmentViewHolder -> {
                if (item is AppointmentListItem.AppointmentItem) holder.bind(item.appointment)
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeaderLabel: TextView = itemView.findViewById(R.id.tv_header_label)
        private val tvHeaderDate: TextView = itemView.findViewById(R.id.tv_header_date)
        fun bind(header: AppointmentListItem.Header) {
            tvHeaderLabel.text = header.label
            tvHeaderDate.text = header.date
        }
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tv_patient_name)
        private val tvTime: TextView = itemView.findViewById(R.id.doctor_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.doctor_status)
        private val tvMode: TextView = itemView.findViewById(R.id.tv_mode) // Add this in your layout

        fun bind(appointment: Appointment) {
            tvPatientName.text = appointment.patientName
            tvTime.text = appointment.time
            tvStatus.text = appointment.status

            tvMode.text = when (appointment.mode.lowercase(Locale.getDefault())) {
                "in-person" -> "In-Person"
                "teleconsultation" -> "Teleconsultation"
                else -> appointment.mode
            }

            itemView.setOnClickListener { onItemClick(appointment) }
        }
    }
}
