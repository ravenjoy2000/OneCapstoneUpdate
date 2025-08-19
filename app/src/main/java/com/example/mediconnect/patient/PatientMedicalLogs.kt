package com.example.mediconnect.patient

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PatientMedicalLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyLogsText: View
    private val logsList = mutableListOf<MedicalLog>()
    private lateinit var adapter: MedicalLogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_logs)

        recyclerView = findViewById(R.id.logsRecyclerView)
        emptyLogsText = findViewById(R.id.emptyLogsText)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MedicalLogsAdapter(this, logsList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("MedicalLogs", "No logged in user")
            showEmptyState()
            return
        }

        Log.d("MedicalLogs", "Loading logs for patientId=$currentUserId")

        FirebaseFirestore.getInstance()
            .collection("medicalLogs")
            .whereEqualTo("patientId", currentUserId)
            .whereEqualTo("status", "paid") // ✅ Only fetch 'paid' logs
            .orderBy("appointmentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("MedicalLogs", "Firestore error: ", e)
                    showEmptyState()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d("MedicalLogs", "No logs found (paid) for patientId=$currentUserId")
                    showEmptyState()
                    return@addSnapshotListener
                }

                logsList.clear()
                for (doc in snapshots) {
                    Log.d("MedicalLogs", "Log found: ${doc.data}")
                    val log = doc.toObject(MedicalLog::class.java)
                    logsList.add(log)
                }

                if (logsList.isEmpty()) {
                    showEmptyState()
                } else {
                    showList()
                }
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
