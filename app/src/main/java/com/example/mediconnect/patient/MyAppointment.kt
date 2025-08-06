package com.example.mediconnect.patient

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mediconnect.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.*

class MyAppointment : AppCompatActivity() {

    // Views
    private lateinit var tvDoctorName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvPreviousDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvMode: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvAppointmentReason: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnReschedule: Button

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var appointmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_appointment)

        setupActionBar()
        initViews()
        loadAppointment()

        btnCancel.setOnClickListener { showCancelDialog() }
        btnReschedule.setOnClickListener { onRescheduleClicked() }
    }

    // -------------------- Setup & Initialization --------------------

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_my_appointment)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = getString(R.string.my_appointment_title)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
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
        btnCancel = findViewById(R.id.btn_cancel)
        btnReschedule = findViewById(R.id.btn_reschedule)
    }

    // -------------------- Load Appointment & Populate UI --------------------

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

                loadDoctorInfo(doc.getString("doctorId"))

                val location = doc.getString("doctorAddress") ?: DEFAULT_ADDRESS
                val contact = doc.getString("doctorPhone") ?: DEFAULT_PHONE
                tvLocation.text = "Location: $location"
                tvNotes.text = "Contact: $contact"

                tvAppointmentReason.text = "Reason: ${doc.getString("reason") ?: "No reason provided."}"
                tvDate.text = "Date: ${doc.getString("date") ?: "--"}"
                tvTime.text = "Time: ${doc.getString("timeSlot") ?: "--"}"
                tvMode.text = "Mode: ${doc.getString("mode") ?: "--"}"

                doc.getString("previousDate")?.let {
                    tvPreviousDate.text = "Rescheduled from: $it"
                    tvPreviousDate.visibility = TextView.VISIBLE
                } ?: run {
                    tvPreviousDate.visibility = TextView.GONE
                }

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
                tvNotes.text = "Contact: $phone"
                tvLocation.text = "Location: $address"
            }
            .addOnFailureListener {
                tvDoctorName.text = "Doctor"
            }
    }

    private fun updateAppointmentUI(statusRaw: String?, cancelReason: String?) {
        val status = statusRaw?.lowercase() ?: "booked"
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

    // -------------------- Alarm Reminders --------------------

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

    // -------------------- Cancel & Reschedule --------------------

    private fun onRescheduleClicked() {
        if (appointmentId == null) {
            Toast.makeText(this, "No appointment to reschedule.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("appointments").document(appointmentId!!)
            .get()
            .addOnSuccessListener { doc ->
                val intent = Intent(this, RescheduleActivity::class.java)
                intent.putExtra("appointmentId", appointmentId)
                intent.putExtra("doctorId", doc.getString("doctorId") ?: "")
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointment info.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCancelDialog() {
        if (appointmentId == null) {
            Toast.makeText(this, "No appointment to cancel.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf("Schedule conflict", "Not feeling well", "Transportation issue", "Changed my mind", "Other")
        AlertDialog.Builder(this)
            .setTitle("Select a reason for cancellation")
            .setItems(reasons) { _, which -> cancelAppointment(reasons[which]) }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun cancelAppointment(reason: String) {
        val userRef = db.collection("users").document(currentUserId!!)
        val appointmentRef = db.collection("appointments").document(appointmentId!!)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        userRef.get().addOnSuccessListener { userSnap ->
            val cancelInfo = userSnap.get("cancellationInfo") as? Map<*, *>
            val prevDate = cancelInfo?.get("date") as? String
            val currentCount = (cancelInfo?.get("count") as? Long)?.toInt() ?: 0

            if (prevDate == today && currentCount >= 3) {
                Toast.makeText(this, "⚠️ You’ve already cancelled 3 appointments today. Try again tomorrow.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            db.runTransaction { transaction ->
                val newCount = if (prevDate == today) currentCount + 1 else 1
                transaction.update(userRef, "cancellationInfo", mapOf(
                    "date" to today,
                    "count" to newCount,
                    "lastCancelledAt" to Timestamp.now()
                ))
                transaction.update(appointmentRef, mapOf(
                    "status" to "cancelled",
                    "cancelReason" to reason,
                    "cancelledAt" to Timestamp.now()
                ))
            }.addOnSuccessListener {
                Toast.makeText(this, "✅ Appointment cancelled successfully.", Toast.LENGTH_SHORT).show()
                showRebookDialog()
            }.addOnFailureListener {
                Log.e("CancelAppointment", "Transaction failed", it)
                Toast.makeText(this, "❌ Failed to cancel appointment: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }

        }.addOnFailureListener {
            Log.e("CancelAppointment", "User fetch failed", it)
            Toast.makeText(this, "❌ Error fetching user data. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRebookDialog() {
        AlertDialog.Builder(this)
            .setTitle("Book a new appointment?")
            .setMessage("Would you like to schedule another appointment now?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, appointment::class.java))
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val DEFAULT_ADDRESS = "Pineda Medical Clinic 206 Paulette St. Josefa Subv. Malabanias, Angeles City, Pampanga"
        private const val DEFAULT_PHONE = "0961-053-9277"
    }
}
