package com.example.mediconnect.patient

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class appointment : BaseActivity() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnBookNow: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbInPerson: RadioButton

    private lateinit var tvDoctorPhone: TextView
    private lateinit var tvDoctorAddress: TextView

    private lateinit var rbTeleconsult: RadioButton
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var etReason: EditText

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedDate: String? = null
    private var selectedTimeSlot: String = ""
    private var selectedMode: String = ""

    private lateinit var spinnerDoctor: Spinner
    private var selectedDoctorName: String? = null

    private var selectedDoctorId: String? = null
    private val doctorMap = mutableMapOf<String, DoctorInfo>()

    private val allowedTimeSlots = listOf("1:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")

    data class DoctorInfo(val id: String, val phone: String, val address: String)

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

        requestNotificationPermission()

        val userId = auth.currentUser?.uid ?: ""

        checkIfUserHasActiveAppointment(userId) { hasActive ->
            if (hasActive) {
                btnSelectDate.isEnabled = false
                rbInPerson.isEnabled = false
                rbTeleconsult.isEnabled = false
                btnBookNow.isEnabled = false

                tvSelectedDate.text = "You already have an active appointment."
                Toast.makeText(this, "You already have an active appointment. Booking is disabled.", Toast.LENGTH_LONG).show()
            } else {
                setupBookingUI(userId)
            }
        }

        tvDoctorPhone = findViewById(R.id.tv_doctor_phone)
        tvDoctorAddress = findViewById(R.id.tv_doctor_address)
        spinnerDoctor = findViewById(R.id.spinner_doctor)

        fetchDoctors()
    }

    private fun fetchDoctors() {
        val doctorNames = mutableListOf<String>()

        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val name = doc.getString("name")
                    val id = doc.id
                    val phone = doc.getString("phone") ?: "Not provided"
                    val address = doc.getString("clinicAddress") ?: "Not provided"

                    if (!name.isNullOrBlank()) {
                        doctorMap[name] = DoctorInfo(id, phone, address)
                        doctorNames.add(name)
                    }
                }

                if (doctorNames.isEmpty()) doctorNames.add("No doctors available")

                val doctorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doctorNames)
                doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDoctor.adapter = doctorAdapter

                spinnerDoctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedDoctorName = doctorNames[position]
                        val doctorInfo = doctorMap[selectedDoctorName]
                        selectedDoctorId = doctorInfo?.id
                        tvDoctorPhone.text = "Phone: ${doctorInfo?.phone ?: "N/A"}"
                        tvDoctorAddress.text = "Address: ${doctorInfo?.address ?: "N/A"}"
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        selectedDoctorName = null
                        selectedDoctorId = null
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                Log.e("DoctorSpinner", "Error loading doctors", it)
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

                val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek != Calendar.MONDAY && dayOfWeek != Calendar.WEDNESDAY && dayOfWeek != Calendar.FRIDAY) {
                    Toast.makeText(this, "Clinic consultations are only available on Monday, Wednesday, and Friday.", Toast.LENGTH_LONG).show()
                    return@DatePickerDialog
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDateFormatted = sdf.format(selectedCalendar.time)
                selectedDate = selectedDateFormatted
                tvSelectedDate.text = "Selected Date: $selectedDateFormatted"

                fetchBookedSlotsForDate(selectedDateFormatted) { bookedSlots ->
                    val adapter = TimeSlotAdapter(allowedTimeSlots, bookedSlots, false, selectedDateFormatted) { selectedTime ->
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

                    val doctorInfo = doctorMap[selectedDoctorName]

                    val booking = Booking(
                        patientId = userId,
                        patientName = userName,
                        doctorId = doctorInfo?.id ?: "",
                        doctorName = selectedDoctorName ?: "Unassigned",
                        date = selectedDate!!,
                        timeSlot = selectedTimeSlot,
                        mode = selectedMode,
                        status = "booked",
                        timestamp = System.currentTimeMillis(),
                        reason = reason,
                        doctorPhone = doctorInfo?.phone ?: "",
                        doctorAddress = doctorInfo?.address ?: ""
                    )






                    db.collection("appointments")
                        .add(booking)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
                            sendAppointmentNotification(selectedDate!!, selectedTimeSlot, selectedMode)
                            scheduleAppointmentReminder(selectedDate!!, selectedTimeSlot, selectedMode)
                            startActivity(Intent(this, MyAppointment::class.java))
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
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            it.title = getString(R.string.my_appointment_title)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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

    private fun sendAppointmentNotification(date: String, time: String, mode: String) {
        val channelId = "appointment_channel"
        val channelName = "Appointment Notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for appointment booking notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.iconhearth)
            .setContentTitle("Appointment Booked")
            .setContentText("You're booked on $date at $time ($mode).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun scheduleAppointmentReminder(date: String, time: String, mode: String) {
        val sdfDateTime = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        val appointmentDateTime = sdfDateTime.parse("$date $time") ?: return

        val reminderTimeInMillis = appointmentDateTime.time - (30 * 60 * 1000)

        val intent = Intent(this, AppointmentReminderWorker::class.java).apply {
            putExtra("date", date)
            putExtra("time", time)
            putExtra("mode", mode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeInMillis,
                    pendingIntent
                )
            } else {
                // Request permission by opening settings
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intentSettings)
                Toast.makeText(this, "Enable exact alarm permission to receive reminders.", Toast.LENGTH_LONG).show()
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTimeInMillis,
                pendingIntent
            )
        }

        Log.d("Reminder", "Scheduled 30-min reminder at ${Date(reminderTimeInMillis)}")
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
