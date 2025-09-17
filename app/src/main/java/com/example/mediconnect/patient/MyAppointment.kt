package com.example.mediconnect.patient

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mediconnect.R
import com.example.mediconnect.patient_adapter.AppointmentReminderUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

class MyAppointment : AppCompatActivity() {

    // Declare UI views
    private lateinit var tvDoctorName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvPreviousDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvMode: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvAppointmentReason: TextView
    private lateinit var tvService: TextView
    private lateinit var tvPrice: TextView

    private lateinit var btnCancel: Button
    private lateinit var btnReschedule: Button
    private lateinit var btnStartConsultation: Button

    private var alreadyReloaded = false  // To prevent multiple reloads of UI

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var appointmentId: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_appointment)

        // Hide status bar depending on SDK version
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
        initViews()

        // Start button hidden initially
        btnStartConsultation.visibility = Button.GONE

        // Show button after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            btnStartConsultation.visibility = Button.VISIBLE
        }, 10000)

        loadAppointment()

        btnCancel.setOnClickListener { showCancelDialog() }
        btnReschedule.setOnClickListener { goToReschedule() }
        btnStartConsultation.setOnClickListener { startConsultation() }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_my_appointment)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
                title = getString(R.string.my_appointment_title)
            }
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            Log.e("SetupActionBar", "Toolbar not found in layout.")
        }
    }

    private fun initViews() {
        tvDoctorName = findViewById(R.id.tv_doctor_name)
        tvStatus = findViewById(R.id.tv_status)
        tvDate = findViewById(R.id.tv_date)
        tvPreviousDate = findViewById(R.id.tv_previous_date)
        tvTime = findViewById(R.id.tv_time)
        tvMode = findViewById(R.id.tv_mode)
        tvLocation = findViewById(R.id.tv_location)
        tvNotes = findViewById(R.id.tv_notes)
        tvAppointmentReason = findViewById(R.id.tv_appointment_reason)
        tvService = findViewById(R.id.tv_service)
        tvPrice = findViewById(R.id.tv_price)

        btnCancel = findViewById(R.id.btn_cancel)
        btnReschedule = findViewById(R.id.btn_reschedule)
        btnStartConsultation = findViewById(R.id.btn_start_consultation)
    }

    @SuppressLint("SetTextI18n")
    private fun loadAppointment() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("appointments")
            .whereEqualTo("patientId", currentUserId)
            .whereIn("status", listOf("booked", "rescheduled", "rescheduled_once"))
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No appointment found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = documents.documents[0]
                appointmentId = doc.id

                val status = doc.getString("status")
                val cancelReason = doc.getString("cancelReason")
                updateAppointmentUI(status, cancelReason)

                val isModifiable = when (status?.lowercase()) {
                    "cancelled", "completed", "no_show", "late" -> false
                    else -> true
                }
                btnCancel.isEnabled = isModifiable
                btnReschedule.isEnabled = isModifiable
                btnStartConsultation.isEnabled = isModifiable

                // Doctor info
                loadDoctorInfo(doc.getString("doctorId"))

                // Location & Contact
                // Location & Contact
                val location = doc.getString("doctorAddress") ?: DEFAULT_ADDRESS
                val contact = doc.getString("doctorPhone") ?: DEFAULT_PHONE
                tvLocation.text = "Location: $location"
                // append PhilHealth reminder sa notes
                tvNotes.text = "Contact: $contact\nBring your PhilHealth ID"


                // Appointment details
                tvAppointmentReason.text = "Reason: ${doc.getString("reason") ?: "--"}"
                tvDate.text = "Date: ${doc.getString("date") ?: "--"}"
                tvTime.text = "Time: ${doc.getString("timeSlot") ?: "--"}"
                tvMode.text = "Mode: ${doc.getString("mode") ?: "--"}"

                // Service & Price
                tvService.text = "Service: ${doc.getString("service") ?: "General Check-up"}"
                val priceValue = doc.getDouble("servicePrice") ?: 0.0
                tvPrice.text = "Price: ₱${String.format("%.2f", priceValue)}"

                // Previous date
                doc.getString("previousDate")?.let {
                    tvPreviousDate.text = "Rescheduled from: $it"
                    tvPreviousDate.visibility = TextView.VISIBLE
                } ?: run { tvPreviousDate.visibility = TextView.GONE }

                // Reminders
                val appointmentDate = doc.getString("date")
                val timeSlot = doc.getString("timeSlot")
                if (!appointmentDate.isNullOrBlank() && !timeSlot.isNullOrBlank()) {
                    checkAndScheduleReminders(appointmentDate, timeSlot)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointment.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDoctorInfo(doctorId: String?) {
        if (doctorId.isNullOrBlank()) {
            tvDoctorName.text = "Doctor"
            return
        }
        db.collection("users").document(doctorId)
            .get()
            .addOnSuccessListener {
                val name = it.getString("name") ?: "Doctor"
                val phone = it.getString("phone") ?: DEFAULT_PHONE
                val address = it.getString("clinicAddress") ?: DEFAULT_ADDRESS
                tvDoctorName.text = name
                // append PhilHealth reminder
                tvNotes.text = "Contact: $phone\nBring your PhilHealth ID"
                tvLocation.text = "Location: $address"
            }
            .addOnFailureListener { tvDoctorName.text = "Doctor" }

    }

    private fun updateAppointmentUI(statusRaw: String?, cancelReason: String?) {
        val status = statusRaw?.lowercase() ?: "booked"

        if ((status in listOf("cancelled", "late", "no_show", "completed", "rescheduled")) && !alreadyReloaded) {
            alreadyReloaded = true
            recreate()
            return
        }

        tvStatus.text = when (status) {
            "cancelled" -> if (!cancelReason.isNullOrBlank()) "Status: Cancelled\nReason: $cancelReason" else "Status: Cancelled"
            "rescheduled", "rescheduled_once" -> "Status: Rescheduled"
            "completed" -> "Status: Completed"
            "late" -> "Status: Late"
            "no_show" -> "Status: No Show"
            else -> "Status: Booked"
        }

        tvStatus.setTextColor(
            ContextCompat.getColor(this, when (status) {
                "cancelled" -> android.R.color.holo_red_dark
                "rescheduled", "rescheduled_once" -> android.R.color.holo_orange_dark
                "late" -> android.R.color.holo_orange_light
                "no_show" -> android.R.color.holo_red_light
                else -> android.R.color.holo_green_dark
            })
        )
    }

    private fun checkAndScheduleReminders(appointmentDate: String, timeSlot: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                AppointmentReminderUtil.scheduleAppointmentReminders(this, appointmentDate, timeSlot)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Allow Appointment Reminders")
                    .setMessage("To receive timely notifications for your appointment, please allow this app to schedule exact alarms.")
                    .setPositiveButton("Allow") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                    .setNegativeButton("No, thanks", null)
                    .show()
            }
        } else {
            AppointmentReminderUtil.scheduleAppointmentReminders(this, appointmentDate, timeSlot)
        }
    }

    private fun startConsultation() {
        if (appointmentId == null) {
            Toast.makeText(this, "No appointment to start.", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("appointments").document(appointmentId!!)
            .get()
            .addOnSuccessListener { doc ->
                val intent = Intent(this, ConsultationStart()::class.java)
                intent.putExtra("appointmentId", appointmentId)
                intent.putExtra("doctorId", doc.getString("doctorId") ?: "")
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointment info.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToReschedule() {
        if (appointmentId == null) {
            Toast.makeText(this, "No appointment to reschedule.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, RescheduleActivity::class.java)
        intent.putExtra("appointmentId", appointmentId)
        startActivity(intent)
    }

    // Cancel logic (unchanged)
    private fun showCancelDialog() { /* same as before */ }
    private fun cancelAppointment(reason: String) { /* same as before */ }
    private fun showRebookDialog() { /* same as before */ }

    companion object {
        private const val DEFAULT_ADDRESS = "Pineda Medical Clinic 206 Paulette St. Josefa Subv. Malabanias, Angeles City, Pampanga"
        private const val DEFAULT_PHONE = "0961-053-9277"
    }
}
