package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem

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
        when (val item = items[position]) {
            is AppointmentListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is AppointmentListItem.AppointmentItem -> (holder as AppointmentViewHolder).bind(item.appointment)
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
        private val tvAppointmentDate: TextView = itemView.findViewById(R.id.doctor_date)
        private val tvAppointmentTime: TextView = itemView.findViewById(R.id.doctor_time)

        fun bind(appointment: Appointment) {
            tvPatientName.text = appointment.patientName
            tvAppointmentDate.text = appointment.date  // Optional; grouped already by date
            tvAppointmentTime.text = appointment.time

            itemView.setOnClickListener {
                onItemClick(appointment)
            }
        }
    }
}