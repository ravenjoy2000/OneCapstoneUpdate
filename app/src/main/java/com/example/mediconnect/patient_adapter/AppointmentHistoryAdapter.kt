package com.example.mediconnect.patient_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment

// Adapter para sa RecyclerView na nagpapakita ng appointment history
class AppointmentHistoryAdapter(private val appointments: List<Appointment>) :
    RecyclerView.Adapter<AppointmentHistoryAdapter.ViewHolder>() {

    // ViewHolder class para i-handle ang bawat item view
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tv_doctor_in_history_name) // Pangalan ng doktor
        val tvStatus: TextView = view.findViewById(R.id.tv_status)                     // Status ng appointment
        val tvDate: TextView = view.findViewById(R.id.tv_date)                         // Petsa ng appointment
        val tvTime: TextView = view.findViewById(R.id.tv_time)                         // Oras ng appointment
        val tvMode: TextView = view.findViewById(R.id.tv_mode)                         // Mode (in-person, teleconsult)
        val tvLocation: TextView = view.findViewById(R.id.tv_location)                 // Lugar ng appointment
        val tvNote: TextView = view.findViewById(R.id.tv_note)                         // Note ng appointment
        val tvReason: TextView = view.findViewById(R.id.tv_reason)                     // Reason kung bakit nag-cancel or reschedule
        val tvPreviousDate: TextView = view.findViewById(R.id.tv_previous_date)        // Dating petsa kung na-reschedule
    }

    // Inflate ang layout ng bawat item at gawing ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false) // Layout ng item
        return ViewHolder(view)
    }

    // I-bind ang data sa bawat ViewHolder base sa posisyon sa list
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position] // Kunin ang appointment data

        // I-format ang pangalan ng doktor; default sa "Dr. Penida" kung wala o unknown
        val doctorDisplayName = if (
            appointment.doctorName.isNullOrBlank() || appointment.doctorName.equals("unknown", ignoreCase = true)
        ) {
            "Dr. Penida"
        } else {
            " ${appointment.doctorName}"
        }

        holder.tvDoctorName.text = doctorDisplayName                          // Ipakita ang pangalan ng doktor
        holder.tvStatus.text = "Status: ${formatStatus(appointment.status)}"  // Ipakita ang status na naka-format
        holder.tvDate.text = "Date: ${appointment.date}"                      // Ipakita ang petsa
        holder.tvTime.text = "Time: ${appointment.time}"                      // Ipakita ang oras
        holder.tvMode.text = "Mode: ${appointment.mode}"                      // Ipakita ang mode ng appointment
        holder.tvLocation.text = "Location: ${appointment.location}"          // Ipakita ang lokasyon
        holder.tvNote.text = "Note: ${appointment.note}"                      // Ipakita ang note

        // Ipakita ang reason kung meron, kung wala walang ipapakitang text
        holder.tvReason.text = if (appointment.reason.isNotBlank())
            "Reason: ${appointment.reason}" else ""

        // Ipakita ang dating petsa kung na-reschedule; kung wala itago ang TextView
        if (appointment.previousDate.isNotBlank()) {
            holder.tvPreviousDate.visibility = View.VISIBLE
            holder.tvPreviousDate.text = "Rescheduled from: ${appointment.previousDate}"
        } else {
            holder.tvPreviousDate.visibility = View.GONE
        }

        // Baguhin ang kulay ng status text depende sa uri ng status
        holder.tvStatus.setTextColor(
            when (appointment.status.lowercase()) {
                "cancelled" -> holder.itemView.context.getColor(android.R.color.holo_red_dark)          // Pula kung cancelled
                "rescheduled", "rescheduled_once" -> holder.itemView.context.getColor(android.R.color.holo_orange_dark) // Orange kung rescheduled
                "completed" -> holder.itemView.context.getColor(android.R.color.holo_green_dark)        // Green kung tapos na
                "no_show" -> holder.itemView.context.getColor(android.R.color.darker_gray)              // Grey kung no-show
                else -> holder.itemView.context.getColor(android.R.color.black)                         // Itim bilang default
            }
        )
    }

    // Ibalik ang bilang ng mga item sa list
    override fun getItemCount(): Int = appointments.size

    // Helper function para i-format ang status string (unang letra uppercase at palitan ang underscore ng space)
    private fun formatStatus(status: String): String {
        return status.replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
    }
}
