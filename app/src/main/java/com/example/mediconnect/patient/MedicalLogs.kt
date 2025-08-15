package com.example.mediconnect.patient

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MedicalLogs : AppCompatActivity() {

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
            showEmptyState()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("medicalLogs")
            .whereEqualTo("patientId", currentUserId)
            .whereEqualTo("status", "paid") // 🔹 Only fetch 'paid' logs from server
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    showEmptyState()
                    return@addSnapshotListener
                }

                logsList.clear()
                snapshots?.forEach { doc ->
                    logsList.add(doc.toObject(MedicalLog::class.java))
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
