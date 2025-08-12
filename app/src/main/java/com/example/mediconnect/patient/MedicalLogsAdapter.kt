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

// Adapter para sa RecyclerView ng medical logs
class MedicalLogsAdapter(
    private val context: Context,            // Context na galing sa Activity
    private val logs: List<MedicalLog>      // Listahan ng mga medical log na ipapakita
) : RecyclerView.Adapter<MedicalLogsAdapter.LogViewHolder>() {

    // ViewHolder para i-handle ang bawat item ng medical log
    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)   // TextView para sa pangalan ng pasyente
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)     // TextView para sa pangalan ng doktor
        val tvAppointmentDate: TextView = itemView.findViewById(R.id.tvAppointmentDate) // TextView para sa petsa ng appointment
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)             // TextView para sa status ng appointment
        val tvDiagnosis: TextView = itemView.findViewById(R.id.tvDiagnosis)       // TextView para sa diagnosis
        val tvDoctorNotes: TextView = itemView.findViewById(R.id.tvDoctorNotes)   // TextView para sa notes ng doktor
        val btnDownloadPdf: Button = itemView.findViewById(R.id.btnDownloadPdf)   // Button para i-download ang PDF
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        // Inflate ang layout ng bawat item sa RecyclerView
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_log, parent, false)
        return LogViewHolder(view)   // Ibalik ang ViewHolder na may view na ito
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]     // Kunin ang kasalukuyang medical log sa position

        // I-set ang mga TextView gamit ang data ng medical log
        holder.tvPatientName.text = "Patient: ${log.patientName}"
        holder.tvDoctorName.text = "Doctor: ${log.doctorName}"
        holder.tvAppointmentDate.text = "Date: ${log.appointmentDate}"
        holder.tvStatus.text = "Status: ${log.status}"
        holder.tvDiagnosis.text = "Diagnosis: ${log.diagnosis}"
        holder.tvDoctorNotes.text = "Doctor Notes: ${log.doctorNotes}"

        // Kapag na-click ang download PDF button, tawagin ang createPdf function
        holder.btnDownloadPdf.setOnClickListener {
            createPdf(log)
        }
    }

    override fun getItemCount(): Int = logs.size   // Ibalik ang dami ng medical logs sa list

    // Function para gumawa ng PDF mula sa isang MedicalLog object
    private fun createPdf(log: MedicalLog) {
        try {
            val pdfDocument = PdfDocument()   // Gumawa ng bagong PDF document
            val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()  // I-define ang sukat ng page
            val page = pdfDocument.startPage(pageInfo)   // Simulan ang page
            val canvas = page.canvas                      // Kunin ang canvas para mag-drawing
            val paint = android.graphics.Paint().apply { textSize = 12f }   // Paint object na may text size 12

            var y = 20   // Starting Y position para sa text sa PDF
            // Listahan ng mga linya na isusulat sa PDF
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

            // Isulat ang bawat linya sa PDF canvas
            for (line in lines) {
                canvas.drawText(line, 10f, y.toFloat(), paint)
                y += 20   // Taasan ang Y position para sa susunod na linya
            }

            pdfDocument.finishPage(page)   // Tapusin ang page

            // Folder kung saan isi-save ang PDF (Downloads/MedicalLogs)
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MedicalLogs"
            )
            if (!directory.exists()) directory.mkdirs()   // Gumawa ng folder kung wala pa

            // File name ng PDF gamit ang appointmentId o timestamp kung wala
            val file = File(directory, "MedicalLog_${log.appointmentId ?: System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))   // Isulat ang PDF file sa storage
            pdfDocument.close()                            // Isara ang PDF document

            // Ipakita ang path ng na-save na PDF sa user
            Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()  // I-print ang error kung may problema sa paggawa ng PDF
            Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()  // Ipakita error message sa user
        }
    }
}
