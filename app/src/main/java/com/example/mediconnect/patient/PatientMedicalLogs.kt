package com.example.mediconnect.patient

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PatientMedicalLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyLogsText: View
    private lateinit var progressBar: ProgressBar
    private val logsList = mutableListOf<MedicalLog>()
    private lateinit var adapter: MedicalLogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_logs)

        recyclerView = findViewById(R.id.logsRecyclerView)
        emptyLogsText = findViewById(R.id.emptyLogsText)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MedicalLogsAdapter(this, logsList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            Log.e("MedicalLogs", "No logged in user")
            showEmptyState()
            return
        }

        progressBar.visibility = View.VISIBLE

        FirebaseFirestore.getInstance()
            .collection("medical_logs")
            .whereEqualTo("patientId", currentUserId)
            .whereEqualTo("status", "Complete")
            .addSnapshotListener { snapshots, e ->
                progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e("MedicalLogs", "Firestore error: ", e)
                    showEmptyState()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d("MedicalLogs", "No logs found for patientId=$currentUserId")
                    showEmptyState()
                    return@addSnapshotListener
                }

                logsList.clear()

                val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm:ss a z", Locale.getDefault())

                snapshots.forEach { doc ->
                    val timestamp: Timestamp? = doc.getTimestamp("timestamp")

                    if (timestamp == null) {
                        Log.w("MedicalLogs", "Skipping log with no timestamp")
                        return@forEach
                    }

                    // Format the timestamp exactly as Firestore shows
                    val dateString = sdf.format(timestamp.toDate())

                    logsList.add(
                        MedicalLog(
                            medicalLogId = doc.getString("medicalLogId") ?: "",
                            patientName = doc.getString("patientName") ?: "",
                            appointmentDate = timestamp,
                            diagnosis = doc.getString("diagnosis") ?: "",
                            doctorNotes = doc.getString("doctorNotes") ?: "",
                            status = doc.getString("status") ?: "",
                            date = dateString,  // Display formatted timestamp
                            doctorName = doc.getString("doctorName") ?: "",
                            doctorId = doc.getString("doctorId") ?: "",
                            patientId = doc.getString("patientId") ?: "",
                            appointmentId = doc.getString("appointmentId") ?: "",
                            appointmentTime = doc.getString("appointmentTime") ?: "",
                            appointmentDay = null,
                            appointmentMonth = null,
                            appointmentYear = null,
                            appointmentHour = null,
                            appointmentMinute = null
                        )
                    )
                }

                // Sort descending by timestamp
                logsList.sortByDescending { it.appointmentDate?.toDate() }

                Log.d("MedicalLogs", "Loaded ${logsList.size} logs")
                if (logsList.isEmpty()) showEmptyState() else showList()
                adapter.notifyDataSetChanged()
            }
    }

    private fun showEmptyState() {
        emptyLogsText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showList() {
        emptyLogsText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
