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

class AppointmentDetailsActivity : AppCompatActivity() {

    // UI components
    private lateinit var ivPatientProfile: ImageView
    private lateinit var ivPatientGovId: ImageView
    private lateinit var tvPatientName: TextView
    private lateinit var chipAppointmentStatus: com.google.android.material.chip.Chip
    private lateinit var tvAppointmentDate: TextView
    private lateinit var tvAppointmentTime: TextView
    private lateinit var tvConsultationType: TextView
    private lateinit var btnStartConsultation: Button
    private lateinit var btnReschedule: Button
    private lateinit var btnComplete: MaterialButton

    // Medical log UI components
    private lateinit var etDiagnosis: TextInputEditText
    private lateinit var etDoctorNotes: TextInputEditText
    private lateinit var btnUploadFile: MaterialButton
    private lateinit var tvSelectedFile: TextView
    private lateinit var btnSave: MaterialButton

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // File Uri & Name
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    // Appointment
    private var appointment: Appointment? = null

    // File picker
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

        // Get appointment data
        appointment = intent.getParcelableExtra("appointment_data")
        if (appointment != null) {
            Log.d(TAG, "Received appointment with ID: ${appointment?.appointmentId}")
            displayAppointmentDetails(appointment!!)
            setupButtonListeners(appointment!!)
            loadPatientGovId(appointment!!.patientId)
        } else {
            Log.w(TAG, "No appointment data received!")
            showNoAppointmentData()
        }
    }

    private fun initViews() {
        ivPatientProfile = findViewById(R.id.iv_patient_profile)
        ivPatientGovId = findViewById(R.id.iv_patient_gov_id)
        tvPatientName = findViewById(R.id.tv_patient_name)
        chipAppointmentStatus = findViewById(R.id.chip_appointment_status)
        tvAppointmentDate = findViewById(R.id.tv_appointment_date)
        tvAppointmentTime = findViewById(R.id.tv_appointment_time)
        tvConsultationType = findViewById(R.id.tv_consultation_type)
        btnStartConsultation = findViewById(R.id.btn_start_consultation)
        btnReschedule = findViewById(R.id.btn_reschedule)
        btnComplete = findViewById(R.id.btn_complete)

        etDiagnosis = findViewById(R.id.etDiagnosis)
        etDoctorNotes = findViewById(R.id.etDoctorNotes)
        btnUploadFile = findViewById(R.id.btnUploadFile)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupButtonListeners(appointment: Appointment) {
        btnStartConsultation.setOnClickListener { startConsultation(appointment) }
        btnReschedule.setOnClickListener { openRescheduleDialog(appointment) }
        btnUploadFile.setOnClickListener { filePickerLauncher.launch("application/pdf,image/*") }
        btnSave.setOnClickListener { saveMedicalLog() }
        btnComplete.setOnClickListener { confirmCompleteAppointment(appointment) }
    }

    private fun displayAppointmentDetails(appointment: Appointment) {
        tvPatientName.text = appointment.patientName
        chipAppointmentStatus.text = appointment.status
        tvAppointmentDate.text = appointment.date
        tvAppointmentTime.text = appointment.time
        tvConsultationType.text = appointment.mode

        if (!appointment.patientId.isNullOrEmpty()) {
            fetchUserImage(appointment.patientId)
        } else {
            ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
        }

        // Hide consultation button if in-person
        btnStartConsultation.visibility =
            if (appointment.mode.equals("in_person", ignoreCase = true)) View.GONE else View.VISIBLE
    }

    private fun loadPatientGovId(patientId: String?) {
        if (patientId.isNullOrEmpty()) return
        db.collection("users").document(patientId)
            .get()
            .addOnSuccessListener { doc ->
                val govIdUrl = doc.getString("goverment_or_phealtID")
                if (!govIdUrl.isNullOrEmpty()) {
                    loadImage(govIdUrl, ivPatientGovId)
                } else {
                    ivPatientGovId.setImageResource(R.drawable.cuteperson)
                }
            }
            .addOnFailureListener {
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

    private fun saveMedicalLog() {
        val diagnosis = etDiagnosis.text?.toString()?.trim()
        val doctorNotes = etDoctorNotes.text?.toString()?.trim()

        if (diagnosis.isNullOrEmpty()) {
            etDiagnosis.error = "Diagnosis is required"
            return
        }
        if (doctorNotes.isNullOrEmpty()) {
            etDoctorNotes.error = "Doctor notes are required"
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        if (selectedFileUri != null) {
            uploadFileToStorage(selectedFileUri!!) { uploadedUrl ->
                saveMedicalLogToFirestore(diagnosis, doctorNotes, uploadedUrl)
            }
        } else {
            saveMedicalLogToFirestore(diagnosis, doctorNotes, null)
        }
    }

    private fun uploadFileToStorage(fileUri: Uri, onComplete: (String?) -> Unit) {
        val appointmentId = appointment?.appointmentId ?: return onComplete(null)
        val filename = selectedFileName ?: fileUri.lastPathSegment ?: "medical_log_file"
        val ref = storage.reference.child("medical_logs/$appointmentId/$filename")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri -> onComplete(uri.toString()) }
                    .addOnFailureListener { onComplete(null) }
            }
            .addOnFailureListener { onComplete(null) }
    }

    private fun saveMedicalLogToFirestore(diagnosis: String, doctorNotes: String, fileUrl: String?) {
        val appointmentId = appointment?.appointmentId ?: return
        val patientId = appointment?.patientId ?: return
        val doctorId = auth.currentUser?.uid ?: return

        val medicalLogRef = db.collection("medical_logs").document()
        val medicalLogId = medicalLogRef.id

        val medicalLog = hashMapOf(
            "medicalLogId" to medicalLogId,
            "patientName" to (appointment?.patientName ?: ""),
            "appointmentDate" to (appointment?.date ?: ""),
            "appointmentId" to appointmentId,
            "patientId" to patientId,
            "doctorId" to doctorId,
            "diagnosis" to diagnosis,
            "doctorNotes" to doctorNotes,
            "fileUrl" to fileUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "Complete"
        )

        medicalLogRef.set(medicalLog).addOnSuccessListener {
            db.collection("appointments").document(appointmentId)
                .update("medicalLogId", medicalLogId)
                .addOnSuccessListener {
                    Toast.makeText(this, "Medical log saved", Toast.LENGTH_SHORT).show()
                    clearMedicalLogForm()
                    resetSaveButton()
                }
                .addOnFailureListener { resetSaveButton() }
        }.addOnFailureListener { resetSaveButton() }
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
            Toast.makeText(this, "Select outcome", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedRadio = findViewById<RadioButton>(selectedId)
        val outcome = selectedRadio.text.toString()

        AlertDialog.Builder(this)
            .setTitle("Complete Appointment")
            .setMessage("Mark appointment as $outcome?")
            .setPositiveButton("Yes") { dialog, _ ->
                completeAppointment(appointment, outcome)
                dialog.dismiss()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun completeAppointment(appointment: Appointment, outcome: String) {
        db.collection("appointments").document(appointment.appointmentId)
            .update("status", outcome)
            .addOnSuccessListener {
                chipAppointmentStatus.text = outcome
                Toast.makeText(this, "Appointment marked as $outcome", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DoctorDashboardActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
    }

    private fun openRescheduleDialog(appointment: Appointment) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(year, month, day)
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                val newDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                val newTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
                updateAppointmentDateTime(appointment, newDate, newTime)
            }
            TimePickerDialog(this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
        DatePickerDialog(this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateAppointmentDateTime(appointment: Appointment, newDate: String, newTime: String) {
        db.collection("appointments").document(appointment.appointmentId)
            .update(mapOf("date" to newDate, "time" to newTime, "status" to "Rescheduled"))
            .addOnSuccessListener {
                tvAppointmentDate.text = newDate
                tvAppointmentTime.text = newTime
                chipAppointmentStatus.text = "Rescheduled"
                Toast.makeText(this, "Appointment rescheduled", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startConsultation(appointment: Appointment) {
        val patientId = appointment.patientId
        val patientName = appointment.patientName
        val currentUserId = auth.currentUser?.uid ?: ""
        val config = ZegoUIKitPrebuiltCallInvitationConfig()
        ZegoUIKitPrebuiltCallService.init(application, AppConstant.appId, AppConstant.appSign, currentUserId, "Doctor", config)

        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("targetUserId", patientId)
            putExtra("targetUserName", patientName)
            putExtra("appointmentId", appointment.appointmentId)
            putExtra("appointment_data", appointment)
        }
        startActivity(intent)
    }

    private fun showNoAppointmentData() {
        ivPatientProfile.setImageResource(R.drawable.ic_user_place_holder)
        tvPatientName.text = "No appointment data"
        chipAppointmentStatus.text = ""
        tvAppointmentDate.text = ""
        tvAppointmentTime.text = ""
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

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    companion object {
        private const val TAG = "AppointmentDetailsActivity"
    }
}
