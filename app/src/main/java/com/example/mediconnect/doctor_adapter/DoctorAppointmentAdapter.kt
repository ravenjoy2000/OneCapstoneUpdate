package com.example.mediconnect.doctor_adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class DoctorAppointmentAdapter(
    private var items: List<AppointmentListItem>,
    private val onAppointmentClick: (Appointment) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedAppointments = mutableSetOf<String>()
    private var selectionMode = false

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
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.doctor_item_appointment_header, parent, false)
            )
            TYPE_APPOINTMENT -> AppointmentViewHolder(
                inflater.inflate(R.layout.doctor_item_appointment, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AppointmentListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is AppointmentListItem.AppointmentItem -> (holder as AppointmentViewHolder).bind(item.appointment)
        }
    }

    fun toggleSelectionById(appointmentId: String) {
        val index = items.indexOfFirst {
            it is AppointmentListItem.AppointmentItem && it.appointment.appointmentId == appointmentId
        }
        if (index != -1) {
            val item = items[index] as AppointmentListItem.AppointmentItem
            toggleSelection(item.appointment, index)
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
        private val card: MaterialCardView = itemView.findViewById(R.id.card_appointment)
        private val tvPatientName: TextView = itemView.findViewById(R.id.tv_patient_name)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvTime: TextView = itemView.findViewById(R.id.doctor_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.doctor_status)
        private val tvMode: TextView = itemView.findViewById(R.id.tv_mode)
        private val tvCancellationReason: TextView = itemView.findViewById(R.id.tv_cancellation_reason)

        fun bind(appointment: Appointment) {
            tvPatientName.text = appointment.patientName
            tvDate.text = appointment.date
            tvTime.text = appointment.time
            tvStatus.text = appointment.status

            tvMode.text = when (appointment.mode.lowercase(Locale.getDefault())) {
                "in-person" -> "In-Person"
                "teleconsult", "teleconsultation" -> "Teleconsultation"
                else -> appointment.mode
            }

            // Show cancellation reason only if cancelled
            if (appointment.status.equals("cancelled", ignoreCase = true) &&
                !appointment.cancellationReason.isNullOrBlank()
            ) {
                tvCancellationReason.visibility = View.VISIBLE
                tvCancellationReason.text = "Reason: ${appointment.cancellationReason}"
            } else {
                tvCancellationReason.visibility = View.GONE
            }

            // Selection highlight
            val isSelected = selectedAppointments.contains(appointment.appointmentId)
            card.strokeWidth = if (isSelected) 6 else 0
            card.strokeColor = Color.BLUE

            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (selectionMode) {
                        toggleSelection(appointment, pos)
                    } else {
                        onAppointmentClick(appointment)
                    }
                }
            }

            itemView.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    if (!selectionMode) {
                        selectionMode = true
                        toggleSelection(appointment, pos)
                    }
                }
                true
            }
        }
    }

    private fun toggleSelection(appointment: Appointment, position: Int) {
        if (selectedAppointments.contains(appointment.appointmentId)) {
            selectedAppointments.remove(appointment.appointmentId)
        } else {
            selectedAppointments.add(appointment.appointmentId)
        }

        if (selectedAppointments.isEmpty()) selectionMode = false
        onSelectionChanged(selectedAppointments.size)
        notifyItemChanged(position)
    }

    // ✅ Public functions for DoctorAppointment.kt
    fun selectAll() {
        selectedAppointments.clear()
        items.forEach { item ->
            if (item is AppointmentListItem.AppointmentItem) {
                selectedAppointments.add(item.appointment.appointmentId)
            }
        }
        selectionMode = selectedAppointments.isNotEmpty()
        notifyDataSetChanged()
        onSelectionChanged(selectedAppointments.size)
    }

    fun clearSelection() {
        selectedAppointments.clear()
        selectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getTotalAppointments(): Int {
        return items.count { it is AppointmentListItem.AppointmentItem }
    }

    fun getSelectedAppointments(): List<String> = selectedAppointments.toList()

    fun updateList(newItems: List<AppointmentListItem>) {
        items = newItems
        selectedAppointments.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun isMultiSelectMode(): Boolean = selectionMode
}
