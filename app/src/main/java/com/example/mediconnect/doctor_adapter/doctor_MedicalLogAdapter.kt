package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog

class doctor_MedicalLogAdapter(private var logs: MutableList<MedicalLog>) :
    RecyclerView.Adapter<doctor_MedicalLogAdapter.LogViewHolder>() {

    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPatientName: TextView = view.findViewById(R.id.tv_patient_name)
        val tvDate: TextView = view.findViewById(R.id.tv_appointment_date)
        val tvDiagnosis: TextView = view.findViewById(R.id.tv_diagnosis)
        val tvNotes: TextView = view.findViewById(R.id.tv_notes)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.doctor_item_medical_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.tvPatientName.text = log.patientName
        holder.tvDate.text = "Date: ${log.appointmentDate}"
        holder.tvDiagnosis.text = "Diagnosis: ${log.diagnosis}"
        holder.tvNotes.text = "Notes: ${log.notes}"
        holder.tvStatus.text = "Status: ${log.status ?: "Unknown"}"

        // Optional: color status safely
        when (log.status?.lowercase()) {
            "completed" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.green))
            "cancelled" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.red))
            "rescheduled" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.orange))
            "late", "no show" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.purple_500))
            else -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.black)) // default
        }
    }


    override fun getItemCount(): Int = logs.size

    fun updateList(newList: List<MedicalLog>) {
        logs.clear()
        logs.addAll(newList)
        notifyDataSetChanged()
    }
}