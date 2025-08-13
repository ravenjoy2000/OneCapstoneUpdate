package com.example.mediconnect.doctor

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DoctorAppointmentHistory : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private val appointmentList = mutableListOf<Appointment>()
    private lateinit var adapter: DoctorAppointmentHistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_appointment_history)

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


        rvAppointments = findViewById(R.id.rvAppointments)
        adapter = DoctorAppointmentHistoryAdapter(appointmentList) { appointment ->
            // Handle reschedule click
            Toast.makeText(this, "Reschedule ${appointment.patientName}", Toast.LENGTH_SHORT).show()
        }
        rvAppointments.adapter = adapter

        loadAppointmentsFromFirestore()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_doctor_Appointemnt_history))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)                          // I-enable ang back arrow
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24) // Icon ng back arrow
            title = resources.getString(R.string.Doctor_Appointment_History)  // Title ng toolbar
        }

        // Kapag pinindot ang back arrow, bumalik sa previous activity
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
                    if (appointment != null && appointment.status in listOf("Late", "No-show", "Complete", "Cancelled")) {
                        appointmentList.add(appointment)
                    }
                }
                appointmentList.sortByDescending { it.dateTime } // Works now
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }


    }
}