package com.example.mediconnect.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment

class AppointmentHistoryAdapter(
    private val historyList: List<Appointment>
) : RecyclerView.Adapter<AppointmentHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tv_doctor_name)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvMode: TextView = view.findViewById(R.id.tv_mode)
        val tvLocation: TextView = view.findViewById(R.id.tv_location)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = historyList[position]
        holder.tvDoctorName.text = appointment.doctorName
        holder.tvStatus.text = appointment.status
        holder.tvDate.text = appointment.date
        holder.tvTime.text = appointment.time
        holder.tvMode.text = appointment.mode
        holder.tvLocation.text = appointment.location
        holder.tvNote.text = appointment.note
    }

    override fun getItemCount(): Int = historyList.size
}
