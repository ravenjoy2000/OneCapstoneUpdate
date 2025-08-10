package com.example.mediconnect.doctor

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentDetailsActivity : AppCompatActivity() {

    private lateinit var ivPatientProfile: ImageView
    private lateinit var tvPatientName: TextView
    private lateinit var tvAppointmentStatus: TextView
    private lateinit var tvAppointmentDateTime: TextView
    private lateinit var tvConsultationType: TextView
    private lateinit var btnStartConsultation: Button
    private lateinit var btnReschedule: Button
    private lateinit var btnCancel: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_details)

        setFullscreenMode()
        initViews()

        val appointment = intent.getParcelableExtra<Appointment>("appointment_data")

        if (appointment != null) {
            tvPatientName.text = appointment.patientName
            tvAppointmentStatus.text = "Status: ${appointment.status}"
            tvAppointmentDateTime.text = "Date: ${appointment.date}\nTime: ${appointment.time}"
            tvConsultationType.text = "Consultation Mode: ${appointment.mode}"

            val patientId = appointment.patientId
            if (!patientId.isNullOrEmpty()) {
                fetchUserImage(patientId)
            } else {
                ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
            }

            // Set button listeners
            btnStartConsultation.setOnClickListener {
                startConsultation(appointment)
            }

            btnReschedule.setOnClickListener {
                rescheduleAppointment(appointment)
            }

            btnCancel.setOnClickListener {
                cancelAppointment(appointment)
            }

        } else {
            ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
            tvPatientName.text = "No appointment data"
            tvAppointmentStatus.text = ""
            tvAppointmentDateTime.text = ""
            tvConsultationType.text = ""

            btnStartConsultation.isEnabled = false
            btnReschedule.isEnabled = false
            btnCancel.isEnabled = false
        }
    }


    private fun fetchUserImage(userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val imageUrl = document.getString("image")
                    if (!imageUrl.isNullOrEmpty()) {
                        // Load image with Glide
                        Glide.with(this@AppointmentDetailsActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_user_place_holder) // while loading
                            .error(R.drawable.ic_user_place_holder) // if failed
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // cache for faster reload
                            .into(ivPatientProfile)
                    } else {
                        ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
                    }
                } else {
                    ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
                }
            }
            .addOnFailureListener {
                ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
            }
    }


    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun initViews() {
        ivPatientProfile = findViewById(R.id.iv_patient_profile)
        tvPatientName = findViewById(R.id.tv_patient_name)
        tvAppointmentStatus = findViewById(R.id.tv_appointment_status)
        tvAppointmentDateTime = findViewById(R.id.tv_appointment_date_time)
        tvConsultationType = findViewById(R.id.tv_consultation_type)
        btnStartConsultation = findViewById(R.id.btn_start_consultation)
        btnReschedule = findViewById(R.id.btn_reschedule)
        btnCancel = findViewById(R.id.btn_cancel)
    }


    private fun startConsultation(appointment: Appointment) {
        // TODO: Replace with actual consultation start logic
        // For example, open video call activity or Jitsi Meet room

        // Example placeholder:
        Toast.makeText(this, "Starting consultation with ${appointment.patientName}", Toast.LENGTH_SHORT).show()

        // Example if using Intent to open a video call activity:
        // val intent = Intent(this, VideoCallActivity::class.java)
        // intent.putExtra("appointment_id", appointment.id)
        // startActivity(intent)
    }

    private fun rescheduleAppointment(appointment: Appointment) {
        // TODO: Open a reschedule screen or dialog where you can pick new date/time

        // Example placeholder:
        Toast.makeText(this, "Open reschedule screen for ${appointment.patientName}", Toast.LENGTH_SHORT).show()

        // Example:
        // val intent = Intent(this, RescheduleActivity::class.java)
        // intent.putExtra("appointment_data", appointment)
        // startActivity(intent)
    }

    private fun cancelAppointment(appointment: Appointment) {

    }




}
