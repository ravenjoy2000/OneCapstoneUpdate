package com.example.mediconnect.patient

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.appcompat.widget.Toolbar // ✅ Para sa custom toolbar setup


class RescheduleActivity : AppCompatActivity() {

    // Declare UI components
    private lateinit var tvNewDate: TextView     // Para sa bagong date display
    private lateinit var tvNewTime: TextView     // Para sa bagong time display
    private lateinit var btnConfirm: Button      // Button para i-confirm ang reschedule

    // Variables para sa appointment at doctor IDs
    private lateinit var appointmentId: String
    private lateinit var doctorId: String

    // Firebase Firestore instance
    private val db = FirebaseFirestore.getInstance()
    // Calendar instance para sa date/time manipulations
    private val calendar = Calendar.getInstance()
    // Format para sa petsa (e.g. 2023-08-11)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Format para sa oras (e.g. 02:00 PM)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reschedule)  // Itakda ang layout

        // Itago ang status bar depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // I-bind ang mga UI components sa layout
        tvNewDate = findViewById(R.id.tvNewDate)
        tvNewTime = findViewById(R.id.tvNewTime)
        btnConfirm = findViewById(R.id.btnConfirmReschedule)

        // Kunin ang appointmentId at doctorId mula sa Intent extras
        appointmentId = intent.getStringExtra("appointmentId") ?: ""
        doctorId = intent.getStringExtra("doctorId") ?: ""

        // Kapag pinindot ang tvNewDate, ipakita ang date picker
        tvNewDate.setOnClickListener {
            showDatePicker()
        }

        // Kapag pinindot ang tvNewTime, ipakita ang time picker
        tvNewTime.setOnClickListener {
            showTimePicker()
        }

        // Kapag pinindot ang confirm button, i-validate muna ang inputs
        btnConfirm.setOnClickListener {
            if (tvNewDate.text == "Choose Date" || tvNewTime.text == "Choose Time") {
                Toast.makeText(this, "Please select new date and time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            rescheduleAppointment()  // Kung valid, tawagin ang reschedule function
        }

        setupActionBar()  // I-setup ang toolbar na may back button
    }

    // I-setup ang custom action bar na may back navigation at title
    private fun setupActionBar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_reschedule)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)   // Ipakita ang back arrow
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)  // Icon ng back arrow
            title = getString(R.string.reschedule_title)  // Title mula sa strings.xml
        }
        // Kapag pinindot ang back arrow, bumalik sa previous screen
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }


    // Ipakita ang date picker dialog para pumili ng bagong date
    private fun showDatePicker() {
        val now = Calendar.getInstance()   // Kunin ang current date/time
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)  // I-set ang napiling petsa

                val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)
                // Limitahan lang sa Lunes, Miyerkules, at Biyernes ang pagpili
                if (dayOfWeek != Calendar.MONDAY && dayOfWeek != Calendar.WEDNESDAY && dayOfWeek != Calendar.FRIDAY) {
                    Toast.makeText(
                        this,
                        "Rescheduling is only allowed on Monday, Wednesday, and Friday.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@DatePickerDialog
                }

                // I-update ang calendar at UI ng napiling date
                calendar.set(year, month, day)
                tvNewDate.text = dateFormat.format(calendar.time)
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )

        // Limitahan ang pwedeng piliin mula ngayon hanggang 6 na buwan mula ngayon
        datePickerDialog.datePicker.minDate = now.timeInMillis
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 6)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()  // Ipakita ang dialog
    }


    // Ipakita ang time picker dialog gamit alert dialog na may mga available slots
    private fun showTimePicker() {
        // Siguraduhin na napili muna ang date bago pumili ng oras
        if (tvNewDate.text == "Choose Date") {
            Toast.makeText(this, "Please choose a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Allowed time slots para sa appointment
        val allowedSlots = listOf("2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        val timeOptions = allowedSlots.toTypedArray()

        // Kunin ang mga naka-book na slots mula sa Firestore para sa napiling petsa
        db.collection("appointments")
            .whereEqualTo("date", tvNewDate.text.toString())
            .whereIn("status", listOf("booked", "rescheduled_once"))  // I-consider lang booked at rescheduled_once status
            .get()
            .addOnSuccessListener { documents ->
                // Kolektahin ang mga oras na naka-book na
                val bookedTimes = documents.mapNotNull { it.getString("timeSlot") }

                // Hanapin ang mga oras na available pa
                val availableTimes = timeOptions.filterNot { bookedTimes.contains(it) }

                // Kapag wala nang available slots
                if (availableTimes.isEmpty()) {
                    Toast.makeText(this, "All slots are already booked for that day.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Ipakita ang dialog para pumili ng available time slot
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Select Time Slot")
                builder.setItems(availableTimes.toTypedArray()) { _, which ->
                    tvNewTime.text = availableTimes[which]  // I-set ang napiling oras
                }
                builder.show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check booked slots.", Toast.LENGTH_SHORT).show()
            }
    }


    // I-update ang appointment sa Firestore gamit ang bagong date at time
    private fun rescheduleAppointment() {
        val newDate = tvNewDate.text.toString()
        val newTime = tvNewTime.text.toString()

        // Ihanda ang data para i-update
        val updateData = mapOf(
            "date" to newDate,
            "timeSlot" to newTime,  // Mahalagang i-update ang oras ng appointment
            "status" to "rescheduled_once",  // Bagong status ng appointment
            "rescheduledAt" to Date()         // Timestamp kung kailan ni-reschedule
        )

        // I-update ang dokumento ng appointment sa Firestore
        db.collection("appointments")
            .document(appointmentId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment rescheduled.", Toast.LENGTH_SHORT).show()

                // Gumawa ng notification para sa doktor
                val notification = mapOf(
                    "to" to doctorId,
                    "from" to FirebaseAuth.getInstance().currentUser?.uid,
                    "type" to "reschedule",
                    "message" to "A patient has rescheduled their appointment.",
                    "timestamp" to Date()
                )
                db.collection("notifications").add(notification)  // I-save ang notification sa Firestore

                finish()  // Isara ang activity at bumalik sa previous screen
            }
            .addOnFailureListener {
                Toast.makeText(this, "Reschedule failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

}
