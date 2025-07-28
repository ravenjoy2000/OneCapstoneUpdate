package com.example.mediconnect.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RescheduleActivity : AppCompatActivity() {

    private lateinit var tvNewDate: TextView
    private lateinit var tvNewTime: TextView
    private lateinit var btnConfirm: Button

    private lateinit var appointmentId: String
    private lateinit var doctorId: String

    private val db = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reschedule)

        tvNewDate = findViewById(R.id.tvNewDate)
        tvNewTime = findViewById(R.id.tvNewTime)
        btnConfirm = findViewById(R.id.btnConfirmReschedule)

        // Get appointmentId and doctorId from Intent
        appointmentId = intent.getStringExtra("appointmentId") ?: ""
        doctorId = intent.getStringExtra("doctorId") ?: ""

        tvNewDate.setOnClickListener {
            showDatePicker()
        }

        tvNewTime.setOnClickListener {
            showTimePicker()
        }

        btnConfirm.setOnClickListener {
            if (tvNewDate.text == "Choose Date" || tvNewTime.text == "Choose Time") {
                Toast.makeText(this, "Please select new date and time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            rescheduleAppointment()
        }
    }

    private fun showDatePicker() {
        val now = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)

                val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek != Calendar.MONDAY && dayOfWeek != Calendar.WEDNESDAY && dayOfWeek != Calendar.FRIDAY) {
                    Toast.makeText(this, "Rescheduling is only allowed on Monday, Wednesday, and Friday.", Toast.LENGTH_LONG).show()
                    return@DatePickerDialog
                }

                calendar.set(year, month, day)
                tvNewDate.text = dateFormat.format(calendar.time)
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )

        // Limit selection from today up to 6 months
        datePickerDialog.datePicker.minDate = now.timeInMillis
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 6)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }


    private fun showTimePicker() {
        if (tvNewDate.text == "Choose Date") {
            Toast.makeText(this, "Please choose a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        val allowedSlots = listOf("2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        val timeOptions = allowedSlots.toTypedArray()

        db.collection("appointments")
            .whereEqualTo("date", tvNewDate.text.toString())
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                val bookedTimes = documents.mapNotNull { it.getString("timeSlot") }

                val availableTimes = timeOptions.filterNot { bookedTimes.contains(it) }

                if (availableTimes.isEmpty()) {
                    Toast.makeText(this, "All slots are already booked for that day.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Select Time Slot")
                builder.setItems(availableTimes.toTypedArray()) { _, which ->
                    tvNewTime.text = availableTimes[which]
                }
                builder.show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check booked slots.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun rescheduleAppointment() {
        val newDate = tvNewDate.text.toString()
        val newTime = tvNewTime.text.toString()

        val updateData = mapOf(
            "date" to newDate,
            "timeSlot" to newTime, // important
            "status" to "rescheduled_once",
            "rescheduledAt" to Date()
        )

        db.collection("appointments")
            .document(appointmentId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment rescheduled.", Toast.LENGTH_SHORT).show()

                val notification = mapOf(
                    "to" to doctorId,
                    "from" to FirebaseAuth.getInstance().currentUser?.uid,
                    "type" to "reschedule",
                    "message" to "A patient has rescheduled their appointment.",
                    "timestamp" to Date()
                )
                db.collection("notifications").add(notification)

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Reschedule failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

}
