package com.example.mediconnect.patient

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog
import java.io.File
import java.io.FileOutputStream

class MedicalLogsAdapter(
    private val context: Context, // now passed from the Activity
    private val logs: List<MedicalLog>
) : RecyclerView.Adapter<MedicalLogsAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvAppointmentDate: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDiagnosis: TextView = itemView.findViewById(R.id.tvDiagnosis)
        val tvDoctorNotes: TextView = itemView.findViewById(R.id.tvDoctorNotes)
        val btnDownloadPdf: Button = itemView.findViewById(R.id.btnDownloadPdf)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]

        holder.tvPatientName.text = "Patient: ${log.patientName}"
        holder.tvDoctorName.text = "Doctor: ${log.doctorName}"
        holder.tvAppointmentDate.text = "Date: ${log.appointmentDate}"
        holder.tvStatus.text = "Status: ${log.status}"
        holder.tvDiagnosis.text = "Diagnosis: ${log.diagnosis}"
        holder.tvDoctorNotes.text = "Doctor Notes: ${log.doctorNotes}"

        holder.btnDownloadPdf.setOnClickListener {
            createPdf(log)
        }
    }

    override fun getItemCount(): Int = logs.size

    private fun createPdf(log: MedicalLog) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint().apply { textSize = 12f }

            var y = 20
            val lines = listOf(
                "Medical Log",
                "===================",
                "Patient: ${log.patientName}",
                "Doctor: ${log.doctorName}",
                "Date: ${log.appointmentDate}",
                "Time: ${log.appointmentTime}",
                "Status: ${log.status}",
                "Diagnosis: ${log.diagnosis}",
                "Notes: ${log.notes}",
                "Doctor Notes: ${log.doctorNotes}"
            )

            for (line in lines) {
                canvas.drawText(line, 10f, y.toFloat(), paint)
                y += 20
            }

            pdfDocument.finishPage(page)

            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MedicalLogs"
            )
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, "MedicalLog_${log.appointmentId ?: System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
