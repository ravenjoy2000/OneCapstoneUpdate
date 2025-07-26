package com.example.mediconnect.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.example.mediconnect.adapters.TimeSlotAdapter

// ... (package and imports unchanged)

class appointment : BaseActivity() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnBookNow: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbInPerson: RadioButton
    private lateinit var rbTeleconsult: RadioButton
    private lateinit var rvTimeSlots: RecyclerView

    private lateinit var etReason: EditText


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedDate: String? = null
    private var selectedTimeSlot: String = ""
    private var selectedMode: String = ""

    private val timeSlots = listOf("8:00 AM", "10:00 AM", "1:00 PM", "3:00 PM")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        setupActionBar()

        supportActionBar?.title = getString(R.string.my_appointment_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvSelectedDate = findViewById(R.id.tv_selected_date)
        btnSelectDate = findViewById(R.id.btn_select_date)
        btnBookNow = findViewById(R.id.btn_book_now)
        radioGroup = findViewById(R.id.rg_appointment_mode)
        rbInPerson = findViewById(R.id.rb_in_person)
        rbTeleconsult = findViewById(R.id.rb_teleconsult)
        rvTimeSlots = findViewById(R.id.rv_time_slots)
        etReason = findViewById(R.id.et_reason)

        rvTimeSlots.layoutManager = LinearLayoutManager(this)

        val userId = auth.currentUser?.uid ?: ""

        checkIfUserHasActiveAppointment(userId) { hasActive ->
            if (hasActive) {
                // Disable booking UI
                btnSelectDate.isEnabled = false
                rbInPerson.isEnabled = false
                rbTeleconsult.isEnabled = false
                btnBookNow.isEnabled = false

                tvSelectedDate.text = "You already have an active appointment."
                Toast.makeText(this, "You already have an active appointment. Booking is disabled.", Toast.LENGTH_LONG).show()
            } else {
                setupBookingUI(userId) // Enable and setup date/time selection
            }
        }
    }

    private fun setupBookingUI(userId: String) {
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDateFormatted = sdf.format(selectedCalendar.time)
                selectedDate = selectedDateFormatted
                tvSelectedDate.text = "Selected Date: $selectedDateFormatted"

                fetchBookedSlotsForDate(selectedDateFormatted) { bookedSlots ->
                    val adapter = TimeSlotAdapter(timeSlots, bookedSlots, false, selectedDateFormatted) { selectedTime ->
                        selectedTimeSlot = selectedTime
                    }

                    rvTimeSlots.adapter = adapter
                }

            }, year, month, day)

            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            val maxDate = Calendar.getInstance()
            maxDate.add(Calendar.MONTH, 6)
            datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

            datePickerDialog.show()
        }

        btnBookNow.setOnClickListener {

            val reason = etReason.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please provide a reason for the appointment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedMode = when {
                rbInPerson.isChecked -> "in_person"
                rbTeleconsult.isChecked -> "teleconsult"
                else -> ""
            }

            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedMode.isEmpty()) {
                Toast.makeText(this, "Please select appointment mode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTimeSlot.isEmpty()) {
                Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Anonymous"

                    val booking = Booking(
                        patientId = userId,
                        patientName = userName,
                        date = selectedDate!!,
                        timeSlot = selectedTimeSlot,
                        mode = selectedMode,
                        status = "booked",
                        timestamp = System.currentTimeMillis(),
                        reason = reason // 👈 Pass it here

                    )

                    db.collection("appointments")
                        .add(booking)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MyAppointment::class.java)
                            startActivity(intent)
                            finish()
                        }

                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        if (toolbar != null) {
            setSupportActionBar(toolbar)

            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
                actionBar.title = getString(R.string.my_appointment_title)
            }

            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            Log.e("ToolbarSetup", "Toolbar not found. Check your layout file.")
        }
    }

    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    private fun fetchBookedSlotsForDate(date: String, onResult: (List<String>) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("date", date)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                val bookedTimes = documents.mapNotNull { it.getString("timeSlot") }
                Log.d("BookedSlots", "Booked on $date: $bookedTimes")
                onResult(bookedTimes)
            }
            .addOnFailureListener {
                Log.e("BookedSlots", "Failed to fetch", it)
                onResult(emptyList())
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
