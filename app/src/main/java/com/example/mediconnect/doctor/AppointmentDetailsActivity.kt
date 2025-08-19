package com.example.mediconnect.doctor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mediconnect.R
import com.example.mediconnect.models.AppConstant
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.MedicalLog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import java.text.SimpleDateFormat
import java.util.*

private lateinit var ivPatientGovId: ImageView

class AppointmentDetailsActivity : AppCompatActivity() {

    // UI components
    private lateinit var ivPatientProfile: ImageView
    private lateinit var tvPatientName: TextView
    private lateinit var tvAppointmentStatus: TextView
    private lateinit var tvAppointmentDateTime: TextView
    private lateinit var tvConsultationType: TextView
    private lateinit var btnStartConsultation: Button
    private lateinit var btnReschedule: Button

    // Medical log UI components
    private lateinit var etDiagnosis: TextInputEditText
    private lateinit var etDoctorNotes: TextInputEditText
    private lateinit var btnUploadFile: MaterialButton
    private lateinit var tvSelectedFile: TextView
    private lateinit var btnSave: MaterialButton

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // File Uri & Name for uploaded medical log file
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    // Appointment object passed via intent
    private var appointment: Appointment? = null

    private lateinit var btnComplete: MaterialButton


    // Activity result launcher for picking PDF or Image files
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedFileUri = uri
                selectedFileName = getFileNameFromUri(uri)
                tvSelectedFile.text = selectedFileName ?: "Selected file"
            } else {
                tvSelectedFile.text = "No file selected"
                selectedFileUri = null
                selectedFileName = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_details)
        setFullscreenMode()
        initViews()

        appointment = intent.getParcelableExtra("appointment_data")

        if (appointment != null) {
            Log.d(TAG, "Received appointment with ID: ${appointment?.appointmentId}")
            displayAppointmentDetails(appointment!!)
            setupButtonListeners(appointment!!)
        } else {
            Log.w(TAG, "No appointment data received!")
            showNoAppointmentData()
        }


        // Initialize views
        tvPatientName = findViewById(R.id.tv_patient_name)
        ivPatientProfile = findViewById(R.id.iv_patient_profile)
        ivPatientGovId = findViewById(R.id.iv_patient_gov_id)

        // Get appointment ID from intent
        val patientId = appointment?.patientId ?: return

        // Load details
        loadAppointmentDetails(patientId)
    }

    private fun loadAppointmentDetails(patientId: String) {
        db.collection("users").document(patientId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val govIdImageUrl = doc.getString("goverment_or_phealtID")
                    if (!govIdImageUrl.isNullOrEmpty()) {
                        loadImage(govIdImageUrl, ivPatientGovId)
                    } else {
                        Log.w(TAG, "Government/PhilHealth ID image URL is missing for patient: $patientId")
                        ivPatientGovId.setImageResource(R.drawable.cuteperson) // fallback
                    }
                } else {
                    Log.w(TAG, "No document found for patientId: $patientId")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading patient details", e)
                ivPatientGovId.setImageResource(R.drawable.cuteperson)
            }
    }


    private fun loadImage(url: String?, imageView: ImageView) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.cuteperson)
                .error(R.drawable.cuteperson)
                .into(imageView)
        }
    }


    private fun initViews() {
        btnComplete = findViewById(R.id.btn_complete)
        ivPatientProfile = findViewById(R.id.iv_patient_profile)
        tvPatientName = findViewById(R.id.tv_patient_name)
        tvAppointmentStatus = findViewById(R.id.tv_appointment_status)
        tvAppointmentDateTime = findViewById(R.id.tv_appointment_date_time)
        tvConsultationType = findViewById(R.id.tv_consultation_type)
        btnStartConsultation = findViewById(R.id.btn_start_consultation)
        btnReschedule = findViewById(R.id.btn_reschedule)

        etDiagnosis = findViewById(R.id.etDiagnosis)
        etDoctorNotes = findViewById(R.id.etDoctorNotes)
        btnUploadFile = findViewById(R.id.btnUploadFile)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupButtonListeners(appointment: Appointment) {
        btnStartConsultation.setOnClickListener {
            startConsultation(appointment)
        }
        btnReschedule.setOnClickListener {
            openRescheduleDialog(appointment)
        }


        btnUploadFile.setOnClickListener {
            filePickerLauncher.launch("application/pdf,image/*")
        }

        btnSave.setOnClickListener {
            saveMedicalLog()
        }

        btnComplete.setOnClickListener {
            confirmCompleteAppointment(appointment)
        }

    }


    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun displayAppointmentDetails(appointment: Appointment) {
        tvPatientName.text = appointment.patientName
        tvAppointmentStatus.text = "Status: ${appointment.status}"
        tvAppointmentDateTime.text = "Date: ${appointment.date}\nTime: ${appointment.time}"
        tvConsultationType.text = "Consultation Mode: ${appointment.mode}"

        if (!appointment.patientId.isNullOrEmpty()) {
            fetchUserImage(appointment.patientId)
        } else {
            ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
        }

        // ✅ Hide consultation button if in_person
        if (appointment.mode.equals("in_person", ignoreCase = true)) {
            btnStartConsultation.visibility = View.GONE
        } else {
            btnStartConsultation.visibility = View.VISIBLE
        }
    }

    private fun saveMedicalLog() {
        val diagnosis = etDiagnosis.text?.toString()?.trim()
        val doctorNotes = etDoctorNotes.text?.toString()?.trim()

        if (diagnosis.isNullOrEmpty()) {
            etDiagnosis.error = "Diagnosis is required"
            etDiagnosis.requestFocus()
            return
        }

        if (doctorNotes.isNullOrEmpty()) {
            etDoctorNotes.error = "Doctor notes are required"
            etDoctorNotes.requestFocus()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        if (selectedFileUri != null) {
            uploadFileToStorage(selectedFileUri!!) { uploadedUrl ->
                if (uploadedUrl != null) {
                    saveMedicalLogToFirestore(diagnosis, doctorNotes, uploadedUrl)
                } else {
                    Toast.makeText(this, "File upload failed", Toast.LENGTH_LONG).show()
                    resetSaveButton()
                }
            }
        } else {
            saveMedicalLogToFirestore(diagnosis, doctorNotes, null)
        }
    }


    private fun uploadFileToStorage(fileUri: Uri, onComplete: (String?) -> Unit) {
        val appointmentId = appointment?.appointmentId
        if (appointmentId.isNullOrEmpty()) {
            Log.e(TAG, "uploadFileToStorage failed: appointmentId is null or empty")
            onComplete(null)
            return
        }

        val filename = selectedFileName ?: fileUri.lastPathSegment ?: "medical_log_file"
        val ref = storage.reference.child("medical_logs/$appointmentId/$filename")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { uri -> onComplete(uri.toString()) }
                    .addOnFailureListener {
                        Log.e(TAG, "Failed to get download URL after upload")
                        onComplete(null)
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "File upload failed: ${it.message}")
                onComplete(null)
            }
    }

    private fun saveMedicalLogToFirestore(
        diagnosis: String,
        doctorNotes: String,
        fileUrl: String?
    ) {
        val appointmentId = appointment?.appointmentId
        val patientId = appointment?.patientId
        val doctorId = auth.currentUser?.uid

        if (appointmentId.isNullOrEmpty() || patientId.isNullOrEmpty() || doctorId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid appointment or user data", Toast.LENGTH_LONG).show()
            resetSaveButton()
            return
        }

        // Generate a new medical log document ID
        val medicalLogRef = db.collection("medical_logs").document()
        val medicalLogId = medicalLogRef.id

        // Create medical log data with patient name, date, and doctor notes
        val medicalLog = hashMapOf(
            "medicalLogId" to medicalLogId,
            "patientName" to (appointment?.patientName ?: ""),  // ✅ store patient name
            "appointmentDate" to (appointment?.date ?: ""),      // ✅ store appointment date
            "appointmentId" to appointmentId,
            "patientId" to patientId,
            "doctorId" to doctorId,
            "diagnosis" to diagnosis,
            "doctorNotes" to doctorNotes,                        // ✅ notes already stored
            "fileUrl" to fileUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "PaymentPending"
        )

        // Save medical log to Firestore
        medicalLogRef.set(medicalLog)
            .addOnSuccessListener {
                // Link medical log ID to appointment
                db.collection("appointments").document(appointmentId)
                    .update("medicalLogId", medicalLogId)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Medical log saved and linked successfully", Toast.LENGTH_LONG).show()
                        clearMedicalLogForm()
                        resetSaveButton()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Log saved but linking failed: ${e.message}", Toast.LENGTH_LONG).show()
                        resetSaveButton()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save medical log: ${e.message}", Toast.LENGTH_LONG).show()
                resetSaveButton()
            }
    }


    private fun clearMedicalLogForm() {
        etDiagnosis.text?.clear()
        etDoctorNotes.text?.clear()
        selectedFileUri = null
        selectedFileName = null
        tvSelectedFile.text = "No file selected"
    }

    private fun resetSaveButton() {
        btnSave.isEnabled = true
        btnSave.text = "Save Medical Log"
    }


    private fun confirmCompleteAppointment(appointment: Appointment) {

        val selectedId = findViewById<RadioGroup>(R.id.rgAppointmentOutcome).checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an appointment outcome", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = findViewById<RadioButton>(selectedId)
        val outcome = selectedRadioButton.text.toString()

        AlertDialog.Builder(this)
            .setTitle("Complete Appointment")
            .setMessage("Mark this appointment with ${appointment.patientName} as ${outcome}?")
            .setPositiveButton("Yes") { dialog, _ ->
                completeAppointment(appointment)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun completeAppointment(appointment: Appointment) {
        val selectedId = findViewById<RadioGroup>(R.id.rgAppointmentOutcome).checkedRadioButtonId

        if (selectedId == -1) {
            Toast.makeText(this, "Please select an appointment outcome", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = findViewById<RadioButton>(selectedId)
        val outcome = selectedRadioButton.text.toString()

        db.collection("appointments").document(appointment.appointmentId)
            .update("status", outcome)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment marked as $outcome", Toast.LENGTH_SHORT).show()
                tvAppointmentStatus.text = "Status: $outcome"
                this.appointment = appointment.copy(status = outcome)

                // ✅ Go back to dashboard and refresh
                val intent = Intent(this, DoctorDashboardActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update status: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun openRescheduleDialog(appointment: Appointment) {
        val calendar = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)

            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                val newDateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                val newTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)

                AlertDialog.Builder(this)
                    .setTitle("Confirm Reschedule")
                    .setMessage("Reschedule appointment to:\n$newDateStr at $newTimeStr?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        updateAppointmentDateTime(appointment, newDateStr, newTimeStr)
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .show()
            }

            val is24Hour = false
            TimePickerDialog(this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24Hour).show()
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateAppointmentDateTime(appointment: Appointment, newDate: String, newTime: String) {
        db.collection("appointments").document(appointment.appointmentId)
            .update(
                mapOf(
                    "date" to newDate,
                    "time" to newTime,
                    "status" to "Rescheduled"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment rescheduled to $newDate at $newTime", Toast.LENGTH_LONG).show()

                tvAppointmentDateTime.text = "Date: $newDate\nTime: $newTime"
                tvAppointmentStatus.text = "Status: Rescheduled"

                this.appointment = appointment.copy(date = newDate, time = newTime, status = "Rescheduled")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reschedule: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startConsultation(appointment: Appointment) {
        val patientId = appointment.patientId
        val patientName = appointment.patientName

        val currentUserId = auth.currentUser?.uid ?: ""
        val currentUserName = "Doctor"

        val config = ZegoUIKitPrebuiltCallInvitationConfig()

        ZegoUIKitPrebuiltCallService.init(application, AppConstant.appId, AppConstant.appSign, currentUserId, currentUserName, config)

        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("targetUserId", patientId)
            putExtra("targetUserName", patientName)
            putExtra("currentUserId", currentUserId)
            putExtra("currentUserName", currentUserName)
            putExtra("appointmentId", appointment.appointmentId)
            putExtra("appointment_data", appointment)
        }

        startActivity(intent)
    }

    private fun showNoAppointmentData() {
        ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
        tvPatientName.text = "No appointment data"
        tvAppointmentStatus.text = ""
        tvAppointmentDateTime.text = ""
        tvConsultationType.text = ""

        btnStartConsultation.isEnabled = false
        btnReschedule.isEnabled = false
    }

    private fun fetchUserImage(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val imageUrl = doc?.getString("image")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_user_place_holder)
                        .error(R.drawable.ic_user_place_holder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivPatientProfile)
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


    companion object {
        private const val TAG = "AppointmentDetailsActivity"
    }
}
