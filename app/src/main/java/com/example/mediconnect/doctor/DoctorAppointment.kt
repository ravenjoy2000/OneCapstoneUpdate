package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DoctorAppointment : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCancel: Button
    private lateinit var checkBoxSelectAll: CheckBox
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
        setupRecyclerAutoRefresh()

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
                if (!appointmentAdapter.isMultiSelectMode()) {
                    val intent = Intent(this, AppointmentDetailsActivity::class.java)
                    intent.putExtra("appointment_data", appointment)
                    startActivity(intent)
                } else {
                    appointmentAdapter.toggleSelectionById(appointment.appointmentId)
                }
            },
            onSelectionChanged = { count ->
                // Update toast at selection
                Toast.makeText(this, "$count selected", Toast.LENGTH_SHORT).show()
                // Sync sa Select All checkbox
                checkBoxSelectAll.isChecked =
                    count == appointmentAdapter.getTotalAppointments() && count > 0
            }
        )

        recyclerView.adapter = appointmentAdapter
    }

    private fun setupButtons() {
        btnCancel = findViewById(R.id.btn_cancel)
        checkBoxSelectAll = findViewById(R.id.checkbox_select_all)

        // Select all checkbox
        checkBoxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                appointmentAdapter.selectAll()
            } else {
                appointmentAdapter.clearSelection()
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            val selected = appointmentAdapter.getSelectedAppointments()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select appointment(s) first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Cancellation")
            builder.setMessage("Are you sure you want to cancel the selected appointment(s)?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                var completed = 0
                val patientEmails = mutableListOf<String>()
                val patientNotifications = mutableListOf<Pair<String, String>>()

                for (appointmentId in selected) {
                    db.collection("appointments").document(appointmentId).get()
                        .addOnSuccessListener { doc ->
                            val patientEmail = doc.getString("patientEmail") ?: ""
                            val patientToken = doc.getString("patientToken") ?: ""
                            val appointmentDate = doc.getString("date") ?: ""
                            val appointmentTime = doc.getString("timeSlot") ?: ""

                            if (patientEmail.isNotEmpty()) {
                                patientEmails.add(patientEmail)
                            }
                            if (patientToken.isNotEmpty()) {
                                val message =
                                    "Your appointment on $appointmentDate at $appointmentTime has been cancelled. Please reschedule."
                                patientNotifications.add(patientToken to message)
                            }

                            cancelAppointment(appointmentId) {
                                completed++
                                if (completed == selected.size) {
                                    Toast.makeText(
                                        this,
                                        "Cancelled ${selected.size} appointment(s)",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    if (patientEmails.isNotEmpty()) {
                                        sendEmailToPatients(patientEmails)
                                    }
                                    for ((token, msg) in patientNotifications) {
                                        sendPushNotification(token, msg)
                                    }

                                    loadAppointmentsFromFirestore()
                                    appointmentAdapter.clearSelection()
                                    checkBoxSelectAll.isChecked = false
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Failed to fetch appointment data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun sendEmailToPatients(emails: List<String>) {
        val subject = "Appointment Cancellation Notice"
        val message = """
        Dear Patient,
        
        We regret to inform you that your appointment has been cancelled. Sorry, the doctor is not available at this time.
        
        Kindly reschedule at your convenience.
        
        Regards,
        Dr. Pineda
    """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPushNotification(patientToken: String, message: String) {
        Toast.makeText(
            this,
            "Notification would be sent to token: $patientToken\n$message",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupSwipeRefresh() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadAppointmentsFromFirestore()
        }
    }

    private fun setupRecyclerAutoRefresh() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy < 0 && !rv.canScrollVertically(-1)) {
                    loadAppointmentsFromFirestore()
                }
            }
        })
    }

    private fun loadAppointmentsFromFirestore() {
        db.collection("appointments")
            .whereEqualTo("status", "booked")
            .get()
            .addOnSuccessListener { documents ->
                val appointments = documents.map { doc ->
                    Appointment(
                        patientName = doc.getString("patientName") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        appointmentId = doc.getString("appointmentId") ?: "",
                        doctorName = doc.getString("doctorName") ?: "",
                        status = doc.getString("status") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("timeSlot") ?: "",
                        mode = doc.getString("mode") ?: "",
                        location = doc.getString("location") ?: "",
                        note = doc.getString("notes") ?: "",
                        reason = doc.getString("reason") ?: "",
                        previousDate = doc.getString("previousDate") ?: ""
                    )
                }

                val groupedList = groupAppointmentsByDate(appointments)
                appointmentAdapter.updateList(groupedList)
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
    }

    private fun groupAppointmentsByDate(appointments: List<Appointment>): List<AppointmentListItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val grouped = appointments.groupBy { it.date }
        val sortedDates = grouped.keys.sorted()

        val result = mutableListOf<AppointmentListItem>()
        for (date in sortedDates) {
            val label = if (date == today) "Today's Appointments"
            else {
                val dateObj = sdf.parse(date)
                val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(dateObj!!)
                "$dayName Appointments"
            }

            result.add(AppointmentListItem.Header(label, date))

            val sortedAppointments = grouped[date]?.sortedBy { it.time } ?: emptyList()
            for (appointment in sortedAppointments) {
                result.add(AppointmentListItem.AppointmentItem(appointment))
            }
        }
        return result
    }

    private fun cancelAppointment(appointmentId: String, onComplete: (() -> Unit)? = null) {
        val reason = "Doctor is unavailable"
        db.collection("appointments").document(appointmentId)
            .update(
                mapOf(
                    "status" to "Cancelled",
                    "cancelReason" to reason
                )
            )
            .addOnSuccessListener { onComplete?.invoke() }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to cancel $appointmentId", Toast.LENGTH_SHORT).show()
            }
    }
}
