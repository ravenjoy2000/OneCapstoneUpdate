package com.example.mediconnect.patient

import com.example.mediconnect.patient_adapter.DayOfWeekValidator
import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.models.Booking
import com.example.mediconnect.patient_adapter.FutureDateValidator
import com.example.mediconnect.patient_adapter.ReminderReceiver
import com.example.mediconnect.patient_adapter.TimeSlotAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class appointment : BaseActivity() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnBookNow: Button
    private lateinit var rbInPerson: RadioButton
    private lateinit var rbTeleconsult: RadioButton
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var etReason: EditText
    private lateinit var tvDoctorPhone: TextView
    private lateinit var tvDoctorAddress: TextView
    private lateinit var spinnerDoctor: Spinner

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedDate: String? = null
    private var selectedTimeSlot: String = ""
    private var selectedMode: String = ""
    private var selectedDoctorName: String? = null
    private var selectedDoctorId: String? = null
    private var selectedDoctorLimit: Int = 15 // default

    private val doctorMap = mutableMapOf<String, DoctorInfo>()

    private val allowedTimeSlots = listOf(
        "8:00 AM", "8:30 AM", "9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
        "5:00 PM", "5:30 PM", "6:00 PM", "6:30 PM", "7:00 PM", "7:30 PM"
    )

    data class DoctorInfo(val id: String, val phone: String, val address: String, val maxPatientsPerDay: Int)

    companion object {
        private const val TAG = "AppointmentActivity"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

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
        requestNotificationPermission()

        val userId = auth.currentUser?.uid.orEmpty()
        checkIfUserHasActiveAppointment(userId) { hasActive ->
            if (hasActive) disableBookingUI()
            else setupBookingUI(userId)
        }

        fetchDoctors()
    }

    private fun initViews() {
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        btnSelectDate = findViewById(R.id.btn_select_date)
        btnBookNow = findViewById(R.id.btn_book_now)
        rbInPerson = findViewById(R.id.rb_in_person)
        rbTeleconsult = findViewById(R.id.rb_teleconsult)
        rvTimeSlots = findViewById(R.id.rv_time_slots)
        etReason = findViewById(R.id.et_reason)
        tvDoctorPhone = findViewById(R.id.tv_doctor_phone)
        tvDoctorAddress = findViewById(R.id.tv_doctor_address)
        spinnerDoctor = findViewById(R.id.spinner_doctor)

        rvTimeSlots.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchDoctors() {
        val doctorNames = mutableListOf<String>()

        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val name = doc.getString("name") ?: continue
                    val id = doc.id
                    val phone = doc.getString("phone") ?: "Not provided"
                    val address = doc.getString("clinicAddress") ?: "Not provided"
                    val maxPatients = doc.getLong("maxPatientsPerDay")?.toInt() ?: 15

                    doctorMap[name] = DoctorInfo(id, phone, address, maxPatients)
                    doctorNames.add(name)
                }

                if (doctorNames.isEmpty()) doctorNames.add("No doctors available")

                val doctorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doctorNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                spinnerDoctor.adapter = doctorAdapter

                spinnerDoctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedDoctorName = doctorNames[position]
                        doctorMap[selectedDoctorName]?.let {
                            selectedDoctorId = it.id
                            selectedDoctorLimit = it.maxPatientsPerDay
                            tvDoctorPhone.text = "Phone: ${it.phone}"
                            tvDoctorAddress.text = "Address: ${it.address}"
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        selectedDoctorName = null
                        selectedDoctorId = null
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBookingUI(userId: String) {

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            // ✅ Set start = today
            val start = calendar.timeInMillis

            // ✅ Set end = 6 months from now
            calendar.add(Calendar.MONTH, 6)
            val end = calendar.timeInMillis

            val constraints = CalendarConstraints.Builder()
                .setStart(start)
                .setEnd(end)
                .setValidator(
                    CompositeDateValidator.allOf(
                        listOf(
                            FutureDateValidator(), // Only future dates
                            DayOfWeekValidator(    // Only Mon, Wed, Sat
                                setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.SATURDAY)
                            )
                        )
                    )
                )
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Appointment Date")
                .setCalendarConstraints(constraints)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val cal = Calendar.getInstance().apply { timeInMillis = selection }
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

                tvSelectedDate.text = "Selected Date: $selectedDate"

                fetchBookedSlotsForDate(selectedDate!!) { bookedSlots ->
                    val availableSlots = filterPastSlots(selectedDate!!, allowedTimeSlots)
                    rvTimeSlots.adapter = TimeSlotAdapter(
                        availableSlots,
                        bookedSlots,
                        false,
                        selectedDate!!
                    ) { slot ->
                        selectedTimeSlot = slot
                    }
                }
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }




        btnBookNow.setOnClickListener {
            val reason = etReason.text.toString().trim()

            if (reason.isEmpty() || selectedDate == null || selectedTimeSlot.isEmpty() ||
                (!rbInPerson.isChecked && !rbTeleconsult.isChecked)
            ) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedMode = if (rbInPerson.isChecked) "in_person" else "teleconsult"

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Anonymous"
                    val doctorInfo = doctorMap[selectedDoctorName]

                    if (doctorInfo == null) {
                        Toast.makeText(this, "Invalid doctor selected", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 🔑 Check daily patient limit per doctor
                    db.collection("appointments")
                        .whereEqualTo("doctorId", doctorInfo.id)
                        .whereEqualTo("date", selectedDate)
                        .whereIn("status", listOf("booked", "rescheduled_once"))
                        .get()
                        .addOnSuccessListener { appointments ->
                            if (appointments.size() >= doctorInfo.maxPatientsPerDay) {
                                Toast.makeText(this, "Daily limit reached for this doctor.", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            val appointmentRef = db.collection("appointments").document()
                            val appointmentId = appointmentRef.id

                            val booking = Booking(
                                appointmentId = appointmentId,
                                patientId = userId,
                                patientName = userName,
                                doctorId = doctorInfo.id,
                                doctorName = selectedDoctorName ?: "Unassigned",
                                date = selectedDate!!,
                                timeSlot = selectedTimeSlot,
                                mode = selectedMode,
                                status = "booked",
                                timestamp = System.currentTimeMillis(),
                                reason = reason,
                                doctorPhone = doctorInfo.phone,
                                doctorAddress = doctorInfo.address
                            )

                            appointmentRef.set(booking)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()

                                    // Schedule reminders
                                    scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 30)
                                    scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 0)

                                    // ✅ Open Gmail composer with patient’s email pre-filled
                                    val patientEmail = auth.currentUser?.email ?: ""
                                    val subject = "Appointment Confirmation"
                                    val body = """
                                      Hi $userName,

                                      Your appointment with Dr. $selectedDoctorName has been booked.
                                      Date: $selectedDate
                                      Time: $selectedTimeSlot
                                      Mode: $selectedMode

                                      Thank you for using MediConnect!
                                      """.trimIndent()

                                    // Build proper mailto URI with recipient
                                    val uri = Uri.parse("mailto:$patientEmail")
                                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, subject)
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }

                                    try {
                                        startActivity(Intent.createChooser(intent, "Send email via"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
                                    }


                                    startActivity(Intent(this, MyAppointment::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show()
                                }



                        }
                }
        }
    }



    private fun filterPastSlots(date: String, slots: List<String>): List<String> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (date != today) return slots

        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return slots.filter {
            val slotTime = sdf.parse(it)
            val slotCalendar = Calendar.getInstance().apply { time = slotTime!! }
            now.before(slotCalendar)
        }
    }

    private fun scheduleAlarm(date: String, time: String, mode: String, minutesBefore: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        val dateTime = sdf.parse("$date $time") ?: return
        val triggerTime = dateTime.time - (minutesBefore * 60 * 1000)

        val message = if (minutesBefore > 0) {
            "Reminder: Your appointment is in $minutesBefore minutes ($mode)."
        } else {
            "It's time for your appointment now ($mode)."
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("notification_message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            minutesBefore,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            Toast.makeText(this, "Enable exact alarms for reminders", Toast.LENGTH_LONG).show()
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d(TAG, "Alarm set for ${Date(triggerTime)}")
    }

    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { onResult(!it.isEmpty) }
            .addOnFailureListener { onResult(false) }
    }

    private fun fetchBookedSlotsForDate(date: String, onResult: (List<String>) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("date", date)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { onResult(it.mapNotNull { doc -> doc.getString("timeSlot") }) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    private fun disableBookingUI() {
        btnSelectDate.isEnabled = false
        rbInPerson.isEnabled = false
        rbTeleconsult.isEnabled = false
        btnBookNow.isEnabled = false
        tvSelectedDate.text = "You already have an active appointment."
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
        supportActionBar?.title = getString(R.string.my_appointment_title)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
