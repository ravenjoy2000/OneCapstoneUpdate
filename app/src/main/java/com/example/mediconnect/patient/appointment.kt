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
import android.view.ViewGroup
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

// Activity para sa booking ng appointment ng pasyente
class appointment  : BaseActivity() {

    // Declare mga UI components
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

    // Firebase instances para sa database at authentication
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variables para sa mga pinili ng user
    private var selectedDate: String? = null
    private var selectedTimeSlot: String = ""
    private var selectedMode: String = ""
    private var selectedDoctorName: String? = null
    private var selectedDoctorId: String? = null

    // Map para sa mga doktor info (id, phone, address)
    private val doctorMap = mutableMapOf<String, DoctorInfo>()
    // Allowed time slots para sa appointment booking
    private val allowedTimeSlots = listOf("1:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")

    // Data class para sa info ng doktor
    data class DoctorInfo(val id: String, val phone: String, val address: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        // Para itago ang status bar (fullscreen mode) depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()    // I-setup ang toolbar/action bar
        initViews()         // I-initialize ang mga UI components
        requestNotificationPermission()  // Hingiin ang permission para sa notifications kung kailangan

        val userId = auth.currentUser?.uid.orEmpty()  // Kunin ang kasalukuyang user id
        checkIfUserHasActiveAppointment(userId) { hasActive -> // Tsek kung may active appointment
            if (hasActive) disableBookingUI()   // Kung meron, i-disable ang booking UI
            else setupBookingUI(userId)          // Kung wala, i-setup ang booking UI
        }

        fetchDoctors()  // Kunin ang listahan ng mga doktor mula sa Firestore
    }

    // Initialize lahat ng view references
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

        rvTimeSlots.layoutManager = LinearLayoutManager(this)  // Set layout manager ng RecyclerView
    }

    // Kuhanin ang mga doktor mula sa Firestore at i-display sa Spinner
    private fun fetchDoctors() {
        val doctorNames = mutableListOf<String>()  // List para sa pangalan ng doktor

        db.collection("users")
            .whereEqualTo("role", "doctor")   // Kunin lang mga users na may role na doctor
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val name = doc.getString("name") ?: continue
                    val id = doc.id
                    val phone = doc.getString("phone") ?: "Not provided"
                    val address = doc.getString("clinicAddress") ?: "Not provided"
                    doctorMap[name] = DoctorInfo(id, phone, address)  // I-save sa map
                    doctorNames.add(name)   // Idagdag sa list ng pangalan
                }

                if (doctorNames.isEmpty()) doctorNames.add("No doctors available") // Kapag walang doktor

                val doctorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doctorNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                spinnerDoctor.adapter = doctorAdapter   // I-set ang adapter ng spinner

                // Listener kapag may napiling doktor sa spinner
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

    // Setup UI para sa booking, kasama ang date picker at booking button
    private fun setupBookingUI(userId: String) {
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, day)
                    }

                    val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek != Calendar.MONDAY &&
                        dayOfWeek != Calendar.WEDNESDAY &&
                        dayOfWeek != Calendar.FRIDAY
                    ) {
                        Toast.makeText(this, "Only Monday, Wednesday, and Friday are available.", Toast.LENGTH_SHORT).show()
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

            // Limit to 6 months ahead
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 6)
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis

            // Disable non-MWF days & change colors
            datePickerDialog.datePicker.setOnDateChangedListener { view, year, month, dayOfMonth ->
                val tempCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)

                if (dayOfWeek != Calendar.MONDAY &&
                    dayOfWeek != Calendar.WEDNESDAY &&
                    dayOfWeek != Calendar.FRIDAY
                ) {
                    // Move forward to next valid day
                    do {
                        tempCal.add(Calendar.DAY_OF_MONTH, 1)
                    } while (tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY &&
                        tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY &&
                        tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY)

                    view.updateDate(
                        tempCal.get(Calendar.YEAR),
                        tempCal.get(Calendar.MONTH),
                        tempCal.get(Calendar.DAY_OF_MONTH)
                    )
                }
            }

            datePickerDialog.show()
        }

        btnBookNow.setOnClickListener {
            val reason = etReason.text.toString().trim()

            // Validation
            if (reason.isEmpty() || selectedDate == null || selectedTimeSlot.isEmpty() ||
                (!rbInPerson.isChecked && !rbTeleconsult.isChecked)
            ) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedMode = if (rbInPerson.isChecked) "in_person" else "teleconsult"

            // Get user name from Firestore
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Anonymous"
                    val doctorInfo = doctorMap[selectedDoctorName]

                    // Create a Firestore doc reference first to get the ID
                    val appointmentRef = db.collection("appointments").document()
                    val appointmentId = appointmentRef.id

                    // Build booking object with the appointmentId included
                    val booking = Booking(
                        appointmentId = appointmentId,  // <-- now set
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

                    // Save booking with the known ID
                    appointmentRef.set(booking)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
                            scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 30) // 30 min before
                            scheduleAlarm(selectedDate!!, selectedTimeSlot, selectedMode, 0)  // at appointment time
                            startActivity(Intent(this, MyAppointment::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show()
                        }
                }
        }

    }

    // Mag-schedule ng alarm para sa paalala sa appointment
    private fun scheduleAlarm(date: String, time: String, mode: String, minutesBefore: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
        val dateTime = sdf.parse("$date $time") ?: return
        val triggerTime = dateTime.time - (minutesBefore * 60 * 1000) // Oras ng trigger

        // Mensahe para sa notification depende kung ilang minuto bago
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
            minutesBefore, // Unique request code para sa alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check kung pinapayagan ng system ang exact alarms (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))  // Buksan settings para payagan
            Toast.makeText(this, "Enable exact alarms for reminders", Toast.LENGTH_LONG).show()
            return
        }

        // I-set ang eksaktong alarm kahit naka-idle device
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("Reminder", "Alarm set for ${Date(triggerTime)}")
    }

    // Tsek kung may active appointment ang user (status booked o rescheduled_once)
    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { onResult(!it.isEmpty) }
            .addOnFailureListener { onResult(false) }
    }

    // Kunin ang mga booked time slots sa isang partikular na date
    private fun fetchBookedSlotsForDate(date: String, onResult: (List<String>) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("date", date)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { onResult(it.mapNotNull { doc -> doc.getString("timeSlot") }) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // I-disable ang booking UI kapag may active appointment na
    private fun disableBookingUI() {
        btnSelectDate.isEnabled = false
        rbInPerson.isEnabled = false
        rbTeleconsult.isEnabled = false
        btnBookNow.isEnabled = false
        tvSelectedDate.text = "You already have an active appointment."
    }

    // I-setup ang toolbar/action bar na may back button at title
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
        supportActionBar?.title = getString(R.string.my_appointment_title)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // Hilingin ang notification permission kung Android 13+
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    // Support sa back navigation
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
