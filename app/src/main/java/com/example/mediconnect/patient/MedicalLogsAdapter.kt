package com.example.mediconnect.patient

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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


import java.io.IOException

class MedicalLogsAdapter(
    private val context: Context,
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

        // Disable button if payment is pending
        holder.btnDownloadPdf.isEnabled = log.status?.lowercase() != "payment_pending"
        holder.btnDownloadPdf.alpha = if (holder.btnDownloadPdf.isEnabled) 1f else 0.5f

        // Set click listener only once
        holder.btnDownloadPdf.setOnClickListener {
            savePdfToDownloads(context, log)
        }

    }

    override fun getItemCount(): Int = logs.size

    private fun savePdfToDownloads(context: Context, log: MedicalLog) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var y = 50f
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Medical Report", 50f, y, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        y += 30
        canvas.drawText("Patient: ${log.patientName}", 50f, y, paint)
        y += 20
        canvas.drawText("Doctor: ${log.doctorName}", 50f, y, paint)
        y += 20
        canvas.drawText("Date: ${log.appointmentDate?.toDate()}", 50f, y, paint)
        y += 20
        canvas.drawText("Time: ${log.appointmentTime}", 50f, y, paint)
        y += 20
        canvas.drawText("Status: ${log.status}", 50f, y, paint)
        y += 20
        canvas.drawText("Diagnosis: ${log.diagnosis}", 50f, y, paint)
        y += 20
        canvas.drawText("Doctor Notes: ${log.doctorNotes}", 50f, y, paint)

        pdfDocument.finishPage(page)

        val fileName = "MedicalLog_${log.appointmentId ?: System.currentTimeMillis()}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ use MediaStore
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MedicalLogs")
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(context, "PDF saved to Downloads/MedicalLogs", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to create PDF file", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 and below: use traditional File API
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val folder = File(downloadsDir, "MedicalLogs")
                if (!folder.exists()) folder.mkdirs()

                val file = File(folder, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                Toast.makeText(context, "PDF saved to Downloads/MedicalLogs", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }


}
