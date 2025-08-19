package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mediconnect.R
import com.example.mediconnect.doctor_adapter.DoctorAppointmentAdapter
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem
import com.example.mediconnect.patient.RescheduleActivity
import com.google.firebase.firestore.FirebaseFirestore

class DoctorAppointment : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCancel: Button
    private lateinit var btnReschedule: Button
    private lateinit var appointmentAdapter: DoctorAppointmentAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_appointment)

        hideStatusBar()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupSwipeRefresh()

        loadAppointmentsFromFirestore()
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Appointments"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_appointments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        appointmentAdapter = DoctorAppointmentAdapter(
            items = emptyList(),
            onAppointmentClick = { appointment ->
                Toast.makeText(this, "Clicked: ${appointment.patientName}", Toast.LENGTH_SHORT).show()
            },
            onSelectionChanged = { count ->
                Toast.makeText(this, "$count selected", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = appointmentAdapter
    }

    private fun setupButtons() {
        btnCancel = findViewById(R.id.btn_cancel)
        btnReschedule = findViewById(R.id.btn_reschedule)

        // Cancel button
        btnCancel.setOnClickListener {
            val selected = appointmentAdapter.getSelectedAppointments()
            when {
                selected.isEmpty() -> {
                    Toast.makeText(this, "Select an appointment first", Toast.LENGTH_SHORT).show()
                }
                selected.size > 1 -> {
                    Toast.makeText(this, "Please select only one appointment to cancel", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val selectedId = selected.first()
                    cancelAppointment(selectedId)
                }
            }
        }

        // Reschedule button
        btnReschedule.setOnClickListener {
            val selected = appointmentAdapter.getSelectedAppointments()
            when {
                selected.isEmpty() -> {
                    Toast.makeText(this, "Select an appointment first", Toast.LENGTH_SHORT).show()
                }
                selected.size > 1 -> {
                    Toast.makeText(this, "Please select only one appointment to reschedule", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val selectedId = selected.first()
                    val intent = Intent(this, RescheduleActivity::class.java)
                    intent.putExtra("appointmentId", selectedId)
                    startActivity(intent)
                }
            }
        }
    }

    /** 🔹 Enable pull-to-refresh */
    private fun setupSwipeRefresh() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadAppointmentsFromFirestore()
        }
    }

    /** 🔹 Load appointments from Firestore */
    /** 🔹 Load only Booked appointments from Firestore */
    private fun loadAppointmentsFromFirestore() {
        db.collection("appointments")
            .whereEqualTo("status", "booked")   // ✅ Only booked ones
            .get()
            .addOnSuccessListener { documents ->
                val newItems = mutableListOf<AppointmentListItem>()

                for (doc in documents) {
                    val appointment = Appointment(
                        appointmentId = doc.id,
                        patientName = doc.getString("patientName") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        mode = doc.getString("mode") ?: "",
                        status = doc.getString("status") ?: "",
                        cancellationReason = doc.getString("cancelReason") ?: ""
                    )
                    newItems.add(AppointmentListItem.AppointmentItem(appointment))
                }

                appointmentAdapter.updateList(newItems)
                swipeRefresh.isRefreshing = false // stop the loader
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
    }


    /** 🔹 Cancel appointment and save reason */
    private fun cancelAppointment(appointmentId: String) {
        val reason = "Doctor is unavailable" // TODO: replace with dialog input
        db.collection("appointments").document(appointmentId)
            .update(
                mapOf(
                    "status" to "Cancelled",
                    "cancelReason" to reason
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                loadAppointmentsFromFirestore() // refresh
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show()
            }
    }
}
