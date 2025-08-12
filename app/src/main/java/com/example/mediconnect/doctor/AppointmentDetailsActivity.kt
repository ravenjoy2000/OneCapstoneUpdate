package com.example.mediconnect.doctor

import android.content.Intent
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
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.example.mediconnect.models.AppConstant

class AppointmentDetailsActivity : AppCompatActivity() {

    // UI components declaration
    private lateinit var ivPatientProfile: ImageView      // ImageView para sa profile picture ng pasyente
    private lateinit var tvPatientName: TextView          // TextView para sa pangalan ng pasyente
    private lateinit var tvAppointmentStatus: TextView    // TextView para sa status ng appointment
    private lateinit var tvAppointmentDateTime: TextView  // TextView para sa petsa at oras ng appointment
    private lateinit var tvConsultationType: TextView     // TextView para sa mode ng consultation
    private lateinit var btnStartConsultation: Button     // Button para simulan ang consultation (video call)
    private lateinit var btnReschedule: Button             // Button para i-reschedule ang appointment
    private lateinit var btnCancel: Button                  // Button para i-cancel ang appointment

    // Firebase Authentication at Firestore instances
    private val auth = FirebaseAuth.getInstance()          // Firebase Auth instance para sa kasalukuyang user (doctor)
    private val db = FirebaseFirestore.getInstance()       // Firestore database instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()                                   // Para edge-to-edge na display ng app
        setContentView(R.layout.activity_appointment_details)  // Itakda ang layout file para sa activity

        setFullscreenMode()                                  // Gawing fullscreen ang activity (tanggal status bar)
        initViews()                                          // I-bind ang mga UI components sa variables

        // Kunin ang appointment object na ipinasa sa intent (parcelable)
        val appointment = intent.getParcelableExtra<Appointment>("appointment_data")

        if (appointment != null) {
            // Ipakita ang detalye ng appointment sa UI
            tvPatientName.text = appointment.patientName
            tvAppointmentStatus.text = "Status: ${appointment.status}"
            tvAppointmentDateTime.text = "Date: ${appointment.date}\nTime: ${appointment.time}"
            tvConsultationType.text = "Consultation Mode: ${appointment.mode}"

            val patientId = appointment.patientId
            if (!patientId.isNullOrEmpty()) {
                fetchUserImage(patientId)    // Kunin at ipakita ang profile image ng pasyente mula Firestore
            } else {
                ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)  // Default placeholder image
            }

            // I-setup ang click listeners para sa mga buttons
            btnStartConsultation.setOnClickListener {
                startConsultation(appointment)    // Simulan ang video consultation
            }

            btnReschedule.setOnClickListener {
                rescheduleAppointment(appointment)  // Tawagin ang reschedule function
            }

            btnCancel.setOnClickListener {
                cancelAppointment(appointment)      // Tawagin ang cancel function
            }

        } else {
            // Kapag walang appointment data, ipakita default info at i-disable ang mga buttons
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

    // Function para kunin ang profile image ng pasyente mula sa Firestore
    private fun fetchUserImage(userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val imageUrl = document.getString("image")    // Kunin ang image URL mula sa document
                    if (!imageUrl.isNullOrEmpty()) {
                        // Load image gamit ang Glide library para sa efficient loading at caching
                        Glide.with(this@AppointmentDetailsActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_user_place_holder) // Placeholder habang naglo-load
                            .error(R.drawable.ic_user_place_holder)       // Ipakita kapag nag-fail mag-load
                            .diskCacheStrategy(DiskCacheStrategy.ALL)     // I-cache para mabilis ang susunod na load
                            .into(ivPatientProfile)
                    } else {
                        ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder) // Default image kapag walang URL
                    }
                } else {
                    ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)  // Default kapag walang document
                }
            }
            .addOnFailureListener {
                ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)  // Default kapag may error
            }
    }

    // Function para gawing fullscreen ang activity (tanggal status bar)
    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    // I-bind ang mga UI components gamit findViewById
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

    // Function para simulan ang video consultation gamit ang ZegoUIKitPrebuiltCallService
    private fun startConsultation(appointment: Appointment) {
        val patientId = appointment.patientId              // Kunin ang patient ID (UID)
        val patientName = appointment.patientName          // Kunin ang pangalan ng pasyente

        val currentUserId = auth.currentUser?.uid ?: ""    // Kunin ang doctor UID mula sa FirebaseAuth
        val currentUserName = "Doctor"                      // Pangalan ng doctor (pwedeng palitan o kunin dynamic)

        val config = ZegoUIKitPrebuiltCallInvitationConfig()  // Config para sa Zego call service

        // I-initialize ang Zego call service gamit ang app credentials at user info
        ZegoUIKitPrebuiltCallService.init(application, AppConstant.appId, AppConstant.appSign, currentUserId, currentUserName, config)

        // Gumawa ng intent para simulan ang VideoCallActivity (custom video call screen)
        val intent = Intent(this, VideoCallActivity::class.java)

        // Ipadala ang mga user info para malaman kung sino ang tatawagan at sino ang caller
        intent.putExtra("targetUserId", patientId)      // Sino ang tatawagan (pasyente)
        intent.putExtra("targetUserName", patientName)
        intent.putExtra("currentUserId", currentUserId)   // Caller (doctor)
        intent.putExtra("currentUserName", currentUserName)

        startActivity(intent)  // Simulan ang video call activity
    }

    // Placeholder function para sa reschedule ng appointment
    private fun rescheduleAppointment(appointment: Appointment) {
        // TODO: I-implement ang reschedule screen or dialog para pumili ng bagong petsa at oras

        Toast.makeText(this, "Open reschedule screen for ${appointment.patientName}", Toast.LENGTH_SHORT).show()
    }

    // Placeholder function para sa pagkansela ng appointment
    private fun cancelAppointment(appointment: Appointment) {
        // TODO: I-implement ang cancellation logic dito
    }
}
