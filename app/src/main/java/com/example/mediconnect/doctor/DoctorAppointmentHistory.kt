package com.example.mediconnect.doctor

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.doctor_adapter.DoctorAppointmentHistoryAdapter
import com.example.mediconnect.models.Appointment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorAppointmentHistory : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private val appointmentList = mutableListOf<Appointment>()
    private val filteredList = mutableListOf<Appointment>()
    private lateinit var adapter: DoctorAppointmentHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_appointment_history)

        // Fullscreen mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()

        searchEditText = findViewById(R.id.etSearch)
        rvAppointments = findViewById(R.id.rvAppointments)

        adapter = DoctorAppointmentHistoryAdapter(filteredList) { appointment ->
            Toast.makeText(this, "Reschedule ${appointment.patientName}", Toast.LENGTH_SHORT).show()
        }
        rvAppointments.adapter = adapter

        setupSearch()
        loadAppointmentsFromFirestore()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAppointments(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterAppointments(query: String) {
        val lowerQuery = query.lowercase()
        filteredList.clear()
        filteredList.addAll(
            appointmentList.filter {
                it.patientName?.lowercase()?.contains(lowerQuery) == true ||
                        it.status?.lowercase()?.contains(lowerQuery) == true
            }
        )
        adapter.notifyDataSetChanged()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_doctor_Appointemnt_history))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = resources.getString(R.string.Doctor_Appointment_History)
        }

        findViewById<Toolbar>(R.id.toolbar_doctor_Appointemnt_history)
            .setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadAppointmentsFromFirestore() {
        val doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { snapshot ->
                appointmentList.clear()
                for (doc in snapshot.documents) {
                    val appointment = doc.toObject(Appointment::class.java)
                    if (appointment != null &&
                        appointment.status in listOf("Late", "No-show", "Complete", "Cancelled")
                    ) {
                        appointmentList.add(appointment)
                    }
                }
                appointmentList.sortByDescending { it.dateTime }

                // Initialize filtered list
                filteredList.clear()
                filteredList.addAll(appointmentList)

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }
}
