package com.example.mediconnect.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment

class DoctorAppointmentAdapter(
    private val appointments: List<Appointment>,
    private val onItemClick: (Appointment) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPatientName: TextView = view.findViewById(R.id.doctor_patient_name)
        val tvDate: TextView = view.findViewById(R.id.doctor_date)
        val tvTime: TextView = view.findViewById(R.id.doctor_time)
        val tvStatus: TextView = view.findViewById(R.id.doctor_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.doctor_item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]

        holder.tvPatientName.text = appointment.patientName
        holder.tvDate.text = appointment.date
        holder.tvTime.text = appointment.time
        holder.tvStatus.text = appointment.status

        holder.itemView.setOnClickListener {
            onItemClick(appointment)
        }
    }

    override fun getItemCount(): Int = appointments.size
}
