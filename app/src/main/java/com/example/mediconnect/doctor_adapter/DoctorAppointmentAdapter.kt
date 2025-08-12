package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem

// Adapter para sa RecyclerView na nagpapakita ng list ng appointment kasama ang mga header
class DoctorAppointmentAdapter(
    private val items: List<AppointmentListItem>,               // Listahan ng header at appointment items
    private val onItemClick: (Appointment) -> Unit              // Lambda function kapag na-click ang appointment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0                        // Constant para sa header view type
        private const val TYPE_APPOINTMENT = 1                   // Constant para sa appointment item view type
    }

    // Ibalik ang view type depende sa item sa posisyon (header o appointment)
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AppointmentListItem.Header -> TYPE_HEADER
            is AppointmentListItem.AppointmentItem -> TYPE_APPOINTMENT
        }
    }

    // Gumawa ng ViewHolder base sa uri ng view (header o appointment)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.doctor_item_appointment_header, parent, false)  // Inflate header layout
                HeaderViewHolder(view)   // Return HeaderViewHolder
            }
            TYPE_APPOINTMENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.doctor_item_appointment, parent, false)         // Inflate appointment item layout
                AppointmentViewHolder(view)   // Return AppointmentViewHolder
            }
            else -> throw IllegalArgumentException("Invalid view type")  // Error kung invalid view type
        }
    }

    // Ibalik ang bilang ng mga items sa list
    override fun getItemCount() = items.size

    // I-bind ang data sa ViewHolder depende sa klase ng item (header o appointment)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AppointmentListItem.Header -> (holder as HeaderViewHolder).bind(item)               // Bind header data
            is AppointmentListItem.AppointmentItem -> (holder as AppointmentViewHolder).bind(item.appointment) // Bind appointment data
        }
    }

    // ViewHolder para sa header ng appointment list
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeaderLabel: TextView = itemView.findViewById(R.id.tv_header_label)   // TextView para sa label ng header
        private val tvHeaderDate: TextView = itemView.findViewById(R.id.tv_header_date)     // TextView para sa date ng header

        // I-bind ang header data sa mga views
        fun bind(header: AppointmentListItem.Header) {
            tvHeaderLabel.text = header.label   // Itakda ang label text
            tvHeaderDate.text = header.date     // Itakda ang date text
        }
    }

    // ViewHolder para sa bawat appointment item
    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tv_patient_name)     // TextView para sa pangalan ng pasyente
        private val tvAppointmentDate: TextView = itemView.findViewById(R.id.doctor_date)     // TextView para sa petsa ng appointment
        private val tvAppointmentTime: TextView = itemView.findViewById(R.id.doctor_time)     // TextView para sa oras ng appointment

        // I-bind ang appointment data sa mga views
        fun bind(appointment: Appointment) {
            tvPatientName.text = appointment.patientName     // Itakda ang pangalan ng pasyente
            tvAppointmentDate.text = appointment.date         // Itakda ang petsa (optional dahil naka-group na)
            tvAppointmentTime.text = appointment.time         // Itakda ang oras ng appointment

            // Itakda ang click listener para sa buong item
            itemView.setOnClickListener {
                onItemClick(appointment)   // Tawagin ang lambda function kapag na-click ang appointment
            }
        }
    }
}
