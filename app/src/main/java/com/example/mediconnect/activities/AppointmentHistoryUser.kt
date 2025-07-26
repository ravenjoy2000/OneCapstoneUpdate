package com.example.mediconnect.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentHistoryUser : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_history_user)


        setupActionBar()
        loadAppointmentHistory()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        if (toolbar != null) {
            setSupportActionBar(toolbar)

            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
                actionBar.title = getString(R.string.my_appointment_title)
            }

            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            Log.e("ToolbarSetup", "Toolbar not found. Check your layout file.")
        }
    }

    private fun loadAppointmentHistory() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_appointment_history)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("appointments")
                .whereEqualTo("patientId", userId)
                .whereIn("status", listOf("completed", "cancelled"))
                .get()
                .addOnSuccessListener { documents ->
                    val historyList = mutableListOf<Appointment>()
                    for (doc in documents) {
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val mode = doc.getString("mode") ?: ""
                        val status = doc.getString("status") ?: ""
                        historyList.add(Appointment(date, time, mode, status))
                    }

                    val adapter = AppointmentHistoryAdapter(historyList)
                    recyclerView.adapter = adapter
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error loading appointment history", e)
                }
        }
    }


}