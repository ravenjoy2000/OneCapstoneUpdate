package com.example.mediconnect.patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog

class MedicalLogAdapter(private val logs: List<MedicalLog>) :
    RecyclerView.Adapter<MedicalLogAdapter.LogViewHolder>() {

    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.logDate)
        val status: TextView = view.findViewById(R.id.logStatus)
        val diagnosis: TextView = view.findViewById(R.id.logDiagnosis)
        val notes: TextView = view.findViewById(R.id.logNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.date.text = log.date
        holder.status.text = log.status
        holder.diagnosis.text = log.diagnosis
        holder.notes.text = log.notes
    }

    override fun getItemCount(): Int = logs.size
}