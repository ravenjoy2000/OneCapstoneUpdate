package com.example.mediconnect.patient

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog

class MedicalLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyLogsText: View
    private val logsList = mutableListOf<MedicalLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_logs)

        recyclerView = findViewById(R.id.logsRecyclerView)
        emptyLogsText = findViewById(R.id.emptyLogsText)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ Only pass the list now
        val adapter = MedicalLogsAdapter(this, logsList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        logsList.add(
            MedicalLog(
                patientName = "John Doe",
                appointmentDate = "2025-08-11",
                diagnosis = "Common Cold",
                notes = "Patient should rest and hydrate.",
                status = "Completed",
                doctorNotes = "Prescribed medicine for 5 days",
                date = "2025-08-11",
                doctorName = "Dr. Maria Pineda",
                doctorId = "doc123",
                patientId = "pat456",
                appointmentId = "app789",
                appointmentTime = "3:00 PM",
                appointmentDay = "11",
                appointmentMonth = "08",
                appointmentYear = "2025",
                appointmentHour = "15",
                appointmentMinute = "00"
            )
        )

        if (logsList.isEmpty()) {
            emptyLogsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyLogsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
