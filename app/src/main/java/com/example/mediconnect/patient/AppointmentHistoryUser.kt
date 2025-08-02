package com.example.mediconnect.patient

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.models.Appointment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppointmentHistoryUser : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val historyList = mutableListOf<Appointment>()
    private lateinit var adapter: AppointmentHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_history_user)

        setupActionBar()
        setupRecyclerView()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        loadAppointmentHistory(userId)
        monitorLateAppointments(userId)
        checkCancellationLimit(userId)
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = getString(R.string.my_appointment_title)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_appointment_history)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppointmentHistoryAdapter(historyList)
        recyclerView.adapter = adapter
    }

    private fun loadAppointmentHistory(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("completed", "cancelled", "late", "no_show"))
            .get()
            .addOnSuccessListener { documents ->
                historyList.clear()
                for (doc in documents) {
                    val appointment = Appointment(
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        mode = doc.getString("mode") ?: "",
                        status = doc.getString("status") ?: "",
                        doctorName = doc.getString("doctorName") ?: "Unknown",
                        reason = doc.getString("appointmentReason") ?: "No reason provided",
                        note = doc.getString("notes") ?: "",
                        location = doc.getString("location") ?: "N/A",
                        bookedAt = doc.getTimestamp("bookedAt")?.toDate()
                    )
                    historyList.add(appointment)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to load appointment history", e)
                Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun monitorLateAppointments(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereEqualTo("status", "confirmed")
            .get()
            .addOnSuccessListener { documents ->
                val currentTime = Calendar.getInstance().time
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                for (doc in documents) {
                    try {
                        val date = doc.getString("date") ?: continue
                        val time = doc.getString("time") ?: continue
                        val appointmentDateTime = sdf.parse("$date $time") ?: continue

                        val difference = currentTime.time - appointmentDateTime.time
                        if (difference > 15 * 60 * 1000) {
                            db.collection("appointments").document(doc.id)
                                .update("status", "late")
                                .addOnSuccessListener {
                                    Log.d("AppointmentStatus", "Marked late: ${doc.id}")
                                    Toast.makeText(
                                        this,
                                        "You are marked late. Please approach the front desk or reschedule.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    } catch (e: ParseException) {
                        Log.e("DateParseError", "Failed to parse appointment datetime", e)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to monitor late appointments", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkCancellationLimit(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereEqualTo("status", "cancelled")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() >= 3) {
                    val restrictedUntil =
                        Timestamp(Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))
                    db.collection("users").document(userId)
                        .update("bookingRestrictedUntil", restrictedUntil)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "You have canceled 3 appointments today. Booking is disabled for 48 hours.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
                Log.e("FirestoreError", "Failed to check cancellation limit")
            }
    }
}