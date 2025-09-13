package com.example.mediconnect.patient

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.databinding.ActivityRescheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RescheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRescheduleBinding

    private lateinit var appointmentId: String
    private lateinit var doctorId: String

    private val db = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRescheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fullscreen (hide status bar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Get extras
        appointmentId = intent.getStringExtra("appointmentId") ?: ""
        doctorId = intent.getStringExtra("doctorId") ?: ""

        // Toolbar setup
        setSupportActionBar(binding.toolbarReschedule)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = getString(R.string.reschedule_title)
        }
        binding.toolbarReschedule.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Button actions
        binding.btnSelectDate.setOnClickListener { showDatePicker() }
        binding.btnSelectTime.setOnClickListener { showTimePicker() }
        binding.btnConfirmReschedule.setOnClickListener {
            if (binding.tvSelectedDate.text.contains("None") ||
                binding.tvSelectedTime.text.contains("None")
            ) {
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
                if (dayOfWeek != Calendar.MONDAY &&
                    dayOfWeek != Calendar.WEDNESDAY &&
                    dayOfWeek != Calendar.FRIDAY
                ) {
                    Toast.makeText(
                        this,
                        "Rescheduling is only allowed on Monday, Wednesday, and Friday.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@DatePickerDialog
                }

                calendar.set(year, month, day)
                binding.tvSelectedDate.text = "Selected Date: ${dateFormat.format(calendar.time)}"
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = now.timeInMillis
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 6)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        if (binding.tvSelectedDate.text.contains("None")) {
            Toast.makeText(this, "Please choose a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        val allowedSlots = listOf("2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        val selectedDate = binding.tvSelectedDate.text.toString()
            .replace("Selected Date: ", "")

        db.collection("appointments")
            .whereEqualTo("date", selectedDate)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                val bookedTimes = documents.mapNotNull { it.getString("timeSlot") }
                val availableTimes = allowedSlots.filterNot { bookedTimes.contains(it) }

                if (availableTimes.isEmpty()) {
                    Toast.makeText(
                        this,
                        "All slots are already booked for that day.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                AlertDialog.Builder(this)
                    .setTitle("Select Time Slot")
                    .setItems(availableTimes.toTypedArray()) { _, which ->
                        binding.tvSelectedTime.text =
                            "Selected Time: ${availableTimes[which]}"
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check booked slots.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rescheduleAppointment() {
        val newDate = binding.tvSelectedDate.text.toString().replace("Selected Date: ", "")
        val newTime = binding.tvSelectedTime.text.toString().replace("Selected Time: ", "")

        val updateData = mapOf(
            "date" to newDate,
            "timeSlot" to newTime,
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
