package com.example.mediconnect.patient

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.mediconnect.patient_adapter.ReminderReceiver
import com.example.mediconnect.patient_adapter.TimeSlotAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class appointment  : BaseActivity() {

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

    private val doctorMap = mutableMapOf<String, DoctorInfo>()
    private val allowedTimeSlots = listOf("1:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")

    data class DoctorInfo(val id: String, val phone: String, val address: String)

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
                    doctorMap[name] = DoctorInfo(id, phone, address)
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
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek !in listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)) {
                        Toast.makeText(this, "Available only Mon/Wed/Fri.", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)
                    tvSelectedDate.text = "Selected Date: $selectedDate"
                    fetchBookedSlotsForDate(selectedDate!!) { bookedSlots ->
                        rvTimeSlots.adapter = TimeSlotAdapter(
                            allowedTimeSlots,
                            bookedSlots,
                            false,
                            selectedDate!!
                        ) { selectedTimeSlot = it }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 6)
            datePicker.datePicker.maxDate = calendar.timeInMillis
            datePicker.show()
        }

        btnBookNow.setOnClickListener {
            val reason = etReason.text.toString().trim()
            if (reason.isEmpty() || selectedDate == null || selectedTimeSlot.isEmpty() ||
                (!rbInPerson.isChecked && !rbTeleconsult.isChecked)
            ) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedMode = if (rbInPerson.isChecked) "in_person" else "teleconsult"

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Anonymous"
                    val doctorInfo = doctorMap[selectedDoctorName]

                    val booking = Booking(
                        patientId = userId,
                        patientName = userName,
                        doctorId = doctorInfo?.id.orEmpty(),
                        doctorName = selectedDoctorName ?: "Unassigned",
                        date = selectedDate!!,
                        timeSlot = selectedTimeSlot,
                        mode = selectedMode,
                        status = "booked",
                        timestamp = System.currentTimeMillis(),
                        reason = reason,
                        doctorPhone = doctorInfo?.phone.orEmpty(),
                        doctorAddress = doctorInfo?.address.orEmpty()
                    )

                    db.collection("appointments").add(booking)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
                            scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 30) // 30 min before
                            scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 0)  // at time
                            startActivity(Intent(this, MyAppointment::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show()
                        }
                }
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
            minutesBefore, // unique request code
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
        Log.d("Reminder", "Alarm set for ${Date(triggerTime)}")
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
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
