package com.example.mediconnect.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class MyAppointment : AppCompatActivity() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvMode: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvAppointmentReason: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnReschedule: Button

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var appointmentId: String? = null
    private val cooldownMillis = 3 * 60 * 1000L // 3 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_appointment)

        setupActionBar()
        initViews()
        loadAppointment()

        btnCancel.setOnClickListener { showCancelDialog() }
        btnReschedule.setOnClickListener {
            Toast.makeText(this, "Reschedule feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

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
        tvTime = findViewById(R.id.tv_time)
        tvMode = findViewById(R.id.tv_mode)
        tvLocation = findViewById(R.id.tv_location)
        tvNotes = findViewById(R.id.tv_notes)
        tvAppointmentReason = findViewById(R.id.tv_appointment_reason)
        btnCancel = findViewById(R.id.btn_cancel)
        btnReschedule = findViewById(R.id.btn_reschedule)
    }

    @SuppressLint("SetTextI18n")
    private fun loadAppointment() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        checkCancellationLimit() // 🔁 Check button availability

        db.collection("appointments")
            .whereEqualTo("patientId", currentUserId)
            .whereIn("status", listOf("booked", "rescheduled"))
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    appointmentId = doc.id

                    val status = doc.getString("status") ?: "N/A"
                    val cancelReason = doc.getString("cancelReason")

                    val statusText = when (status.lowercase()) {
                        "cancelled" -> if (!cancelReason.isNullOrBlank())
                            "Status: Cancelled\nReason: $cancelReason"
                        else "Status: Cancelled"
                        "rescheduled" -> "Status: Rescheduled"
                        "completed" -> "Status: Completed"
                        "late" -> "Status: Late"
                        "no_show" -> "Status: No Show"
                        else -> "Status: Booked"
                    }

                    tvStatus.text = statusText
                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            this,
                            when (status) {
                                "cancelled" -> android.R.color.holo_red_dark
                                "rescheduled" -> android.R.color.holo_orange_dark
                                "late" -> android.R.color.holo_orange_light
                                "no_show" -> android.R.color.holo_red_light
                                else -> android.R.color.holo_green_dark
                            }
                        )
                    )

                    val doctorName = doc.getString("doctorName")
                    tvDoctorName.text = doctorName?.takeIf { it.isNotBlank() }
                        ?: "Dr. Francis Ivan G. Pineda"

                    val location = doc.getString("location")
                    tvLocation.text = "Location: ${location?.takeIf { it.isNotBlank() }
                        ?: "Pineda Medical Clinic 206 Paulette St. Josefa Subv. Malabanias, Angeles City, Pampanga"}"

                    val contactNote = doc.getString("notes")
                    tvNotes.text = "Contact: ${contactNote?.takeIf { it.isNotBlank() } ?: "0961-053-9277"}"

                    val appointmentReason = doc.getString("reason")
                    tvAppointmentReason.text = "Reason: ${appointmentReason ?: "No reason provided."}"

                    tvDate.text = "Date: ${doc.getString("date") ?: "--"}"
                    tvTime.text = "Time: ${doc.getString("timeSlot") ?: "--"}"
                    tvMode.text = "Mode: ${doc.getString("mode") ?: "--"}"
                } else {
                    Toast.makeText(this, "No appointment found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load appointment.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkCancellationLimit() {
        val userRef = db.collection("users").document(currentUserId!!)
        userRef.get().addOnSuccessListener { doc ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val cancelInfo = doc.get("cancellationInfo") as? Map<*, *>
            val cancelDate = cancelInfo?.get("date") as? String
            val cancelCount = (cancelInfo?.get("count") as? Long)?.toInt() ?: 0
            val lastCancelledAt = cancelInfo?.get("lastCancelledAt") as? Timestamp

            if (cancelDate == today && cancelCount >= 3 && lastCancelledAt != null) {
                val now = System.currentTimeMillis()
                val timeSinceLast = now - lastCancelledAt.toDate().time
                if (timeSinceLast < cooldownMillis) {
                    val remaining = (cooldownMillis - timeSinceLast) / 1000
                    btnCancel.isEnabled = false
                    btnReschedule.isEnabled = false
                    Toast.makeText(this, "Limit reached. Try again in $remaining seconds.", Toast.LENGTH_LONG).show()

                    btnCancel.postDelayed({
                        btnCancel.isEnabled = true
                        btnReschedule.isEnabled = true
                    }, cooldownMillis - timeSinceLast)
                }
            }
        }
    }

    private fun showCancelDialog() {
        if (appointmentId == null) {
            Toast.makeText(this, "No appointment to cancel.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf(
            "Schedule conflict",
            "Not feeling well",
            "Transportation issue",
            "Changed my mind",
            "Other"
        )

        AlertDialog.Builder(this)
            .setTitle("Select a reason for cancellation")
            .setItems(reasons) { _, which ->
                val selectedReason = reasons[which]
                cancelAppointment(selectedReason)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun cancelAppointment(reason: String) {
        val userRef = db.collection("users").document(currentUserId!!)
        val appointmentRef = db.collection("appointments").document(appointmentId!!)

        db.runTransaction { transaction ->
            val userSnap = transaction.get(userRef)
            val cancelInfo = userSnap.get("cancellationInfo") as? Map<*, *>
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val prevDate = cancelInfo?.get("date") as? String
            var count = (cancelInfo?.get("count") as? Long)?.toInt() ?: 0

            count = if (prevDate == today) count + 1 else 1

            val newInfo = mapOf(
                "date" to today,
                "count" to count,
                "lastCancelledAt" to Timestamp.now()
            )

            transaction.update(userRef, "cancellationInfo", newInfo)
            transaction.update(appointmentRef, mapOf(
                "status" to "cancelled",
                "cancelReason" to reason
            ))
        }.addOnSuccessListener {
            Toast.makeText(this, "Appointment cancelled.", Toast.LENGTH_SHORT).show()
            showRebookDialog()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to cancel appointment.", Toast.LENGTH_SHORT).show()
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
}
