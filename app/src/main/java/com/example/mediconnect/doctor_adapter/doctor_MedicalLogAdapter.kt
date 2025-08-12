package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog

// Adapter para ipakita ang listahan ng medical logs sa RecyclerView
class doctor_MedicalLogAdapter(private var logs: MutableList<MedicalLog>) :
    RecyclerView.Adapter<doctor_MedicalLogAdapter.LogViewHolder>() {

    // ViewHolder class para hawakan ang views sa bawat item
    inner class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPatientName: TextView = view.findViewById(R.id.tv_patient_name) // TextView para sa pangalan ng pasyente
        val tvDate: TextView = view.findViewById(R.id.tv_appointment_date)   // TextView para sa petsa ng appointment
        val tvDiagnosis: TextView = view.findViewById(R.id.tv_diagnosis)     // TextView para sa diagnosis
        val tvNotes: TextView = view.findViewById(R.id.tv_notes)             // TextView para sa notes
        val tvStatus: TextView = view.findViewById(R.id.tv_status)           // TextView para sa status ng appointment
    }

    // Gumagawa ng bagong ViewHolder at ini-inflate ang layout ng item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.doctor_item_medical_log, parent, false)  // I-inflate ang custom item layout
        return LogViewHolder(view)
    }

    // Ibinibind ang data sa ViewHolder base sa posisyon
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]  // Kunin ang medical log sa kasalukuyang posisyon

        // Itakda ang pangalan ng pasyente sa TextView
        holder.tvPatientName.text = log.patientName

        // Ipakita ang petsa ng appointment sa TextView
        holder.tvDate.text = "Date: ${log.appointmentDate}"

        // Ipakita ang diagnosis sa TextView
        holder.tvDiagnosis.text = "Diagnosis: ${log.diagnosis}"

        // Ipakita ang notes sa TextView
        holder.tvNotes.text = "Notes: ${log.notes}"

        // Ipakita ang status ng appointment, default ay "Unknown" kung wala
        holder.tvStatus.text = "Status: ${log.status ?: "Unknown"}"

        // Optional: Baguhin ang kulay ng status base sa value nito para mas madali makita
        when (log.status?.lowercase()) {
            "completed" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.green))    // Berde kung completed
            "cancelled" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.red))      // Pula kung cancelled
            "rescheduled" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.orange)) // Kahel kung rescheduled
            "late", "no show" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.purple_500)) // Purple kung late o no show
            else -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.black))           // Itim bilang default
        }
    }

    // Ibalik ang dami ng mga logs sa listahan
    override fun getItemCount(): Int = logs.size

    // I-update ang list ng medical logs at i-notify ang adapter na may pagbabago
    fun updateList(newList: List<MedicalLog>) {
        logs.clear()          // Linisin ang dati nang list
        logs.addAll(newList)  // Idagdag ang bagong list
        notifyDataSetChanged() // Sabihan ang RecyclerView na mag-refresh ng data
    }
}
