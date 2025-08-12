package com.example.mediconnect.patient

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog

// Activity para ipakita ang listahan ng medical logs ng pasyente
class MedicalLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView       // RecyclerView para sa mga medical logs
    private lateinit var emptyLogsText: View               // TextView na ipapakita kapag walang logs
    private val logsList = mutableListOf<MedicalLog>()    // Listahan ng medical logs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_logs)    // I-set ang layout ng activity

        recyclerView = findViewById(R.id.logsRecyclerView) // Kunin ang RecyclerView mula sa layout
        emptyLogsText = findViewById(R.id.emptyLogsText)   // Kunin ang empty message view

        recyclerView.layoutManager = LinearLayoutManager(this) // I-set ang layout manager para vertical list

        // Gumawa ng adapter gamit ang kasalukuyang list ng logs at i-assign sa RecyclerView
        val adapter = MedicalLogsAdapter(this, logsList)
        recyclerView.adapter = adapter

        loadLogs()                                         // Tawagin ang function para i-load ang medical logs
    }

    private fun loadLogs() {
        // Magdagdag ng sample na MedicalLog sa listahan
        logsList.add(
            MedicalLog(
                patientName = "John Doe",                  // Pangalan ng pasyente
                appointmentDate = "2025-08-11",             // Petsa ng appointment
                diagnosis = "Common Cold",                   // Diagnosis
                notes = "Patient should rest and hydrate.",// Mga notes para sa pasyente
                status = "Completed",                        // Status ng appointment
                doctorNotes = "Prescribed medicine for 5 days", // Notes ng doktor
                date = "2025-08-11",                         // Petsa ng log entry
                doctorName = "Dr. Maria Pineda",             // Pangalan ng doktor
                doctorId = "doc123",                          // ID ng doktor
                patientId = "pat456",                         // ID ng pasyente
                appointmentId = "app789",                      // ID ng appointment
                appointmentTime = "3:00 PM",                   // Oras ng appointment
                appointmentDay = "11",                         // Araw ng appointment
                appointmentMonth = "08",                       // Buwan ng appointment
                appointmentYear = "2025",                      // Taon ng appointment
                appointmentHour = "15",                        // Oras (24-hour format)
                appointmentMinute = "00"                       // Minuto ng appointment
            )
        )

        // Ipakita ang empty text kapag walang laman ang listahan, kung hindi ay ipakita ang RecyclerView
        if (logsList.isEmpty()) {
            emptyLogsText.visibility = View.VISIBLE         // Ipakita ang "walang logs" na text
            recyclerView.visibility = View.GONE             // Itago ang RecyclerView
        } else {
            emptyLogsText.visibility = View.GONE            // Itago ang "walang logs" na text
            recyclerView.visibility = View.VISIBLE          // Ipakita ang RecyclerView na may laman
        }
    }
}
