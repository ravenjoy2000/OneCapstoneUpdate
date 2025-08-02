package com.example.mediconnect.patient

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.MedicalLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MedicalLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: MedicalLogAdapter
    private val logs = mutableListOf<MedicalLog>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_logs)

        setupActionBar()

        recyclerView = findViewById(R.id.logsRecyclerView)
        emptyTextView = findViewById(R.id.emptyLogsText)

        adapter = MedicalLogAdapter(logs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchMedicalLogs()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_my_profile_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Medical Logs"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun fetchMedicalLogs() {
        val userId = auth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Optionally show a loading indicator here

        db.collection("medical_logs")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                logs.clear()
                for (document in result) {
                    val log = document.toObject(MedicalLog::class.java)
                    logs.add(log)
                }

                adapter.notifyDataSetChanged()

                emptyTextView.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("MedicalLogs", "Failed to load logs", e)
                Toast.makeText(this, "Failed to load logs: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}