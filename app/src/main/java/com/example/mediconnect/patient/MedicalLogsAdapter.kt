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

        // ✅ Disable button if payment is pending
        if (log.status?.lowercase() == "payment_pending") {
            holder.btnDownloadPdf.isEnabled = false
            holder.btnDownloadPdf.alpha = 0.5f
        } else {
            holder.btnDownloadPdf.isEnabled = true
            holder.btnDownloadPdf.alpha = 1f
            holder.btnDownloadPdf.setOnClickListener {
                createPdf(log)
            }
        }

        holder.btnDownloadPdf.setOnClickListener {
            savePdfToDownloads(context, log)
        }

    }

    override fun getItemCount(): Int = logs.size

    private fun createPdf(log: MedicalLog) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                textSize = 12f
            }

            var y = 20f
            canvas.drawText("Medical Log", 10f, y, titlePaint)
            y += 20f
            canvas.drawText("===================", 10f, y, textPaint)
            y += 20f

            val lines = listOf(
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
                canvas.drawText(line, 10f, y, textPaint)
                y += 20f
            }

            pdfDocument.finishPage(page)

            val fileName = "MedicalLog_${log.appointmentId ?: System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage save (Android 10+)
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MedicalLogs")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
                Toast.makeText(context, "PDF saved to Downloads/MedicalLogs", Toast.LENGTH_LONG).show()

            } else {
                // Legacy save for Android 9 and below
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MedicalLogs"
                )
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }

            pdfDocument.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()
        }
    }

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
        canvas.drawText("Date: ${log.appointmentDate}", 50f, y, paint)
        y += 20
        canvas.drawText("Status: ${log.status}", 50f, y, paint)
        y += 20
        canvas.drawText("Diagnosis: ${log.diagnosis}", 50f, y, paint)
        y += 20
        canvas.drawText("Doctor Notes: ${log.doctorNotes}", 50f, y, paint)

        pdfDocument.finishPage(page)

        val fileName = "MedicalLog_${log.appointmentDate}.pdf"

        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MedicalLogs")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }
            Toast.makeText(context, "PDF saved to Downloads/MedicalLogs", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
