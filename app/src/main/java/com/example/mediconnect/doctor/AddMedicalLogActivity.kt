package com.example.mediconnect.doctor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddMedicalLogActivity : AppCompatActivity() {

    private lateinit var etDiagnosis: EditText
    private lateinit var etDoctorNotes: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance() // Initialize Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_medical_log)

        etDiagnosis = findViewById(R.id.etDiagnosis)
        etDoctorNotes = findViewById(R.id.etDoctorNotes)
        btnSave = findViewById(R.id.btnSave)

        val appointmentId = intent.getStringExtra("appointmentId") ?: ""
        val patientId = intent.getStringExtra("patientId") ?: ""
        val doctorId = intent.getStringExtra("doctorId") ?: ""

        btnSave.setOnClickListener {
            val diagnosis = etDiagnosis.text.toString().trim()
            val doctorNotes = etDoctorNotes.text.toString().trim()

            // Basic validation
            if (diagnosis.isEmpty()) {
                etDiagnosis.error = "Please enter diagnosis"
                etDiagnosis.requestFocus()
                return@setOnClickListener
            }
            if (doctorNotes.isEmpty()) {
                etDoctorNotes.error = "Please enter doctor notes"
                etDoctorNotes.requestFocus()
                return@setOnClickListener
            }

            // Disable button to avoid multiple clicks
            btnSave.isEnabled = false

            // Optional: Show a progress dialog if you have one, else skip
            // showProgressDialog("Saving medical log...")

            val log = hashMapOf(
                "appointmentId" to appointmentId,
                "patientId" to patientId,
                "doctorId" to doctorId,
                "diagnosis" to diagnosis,
                "doctorNotes" to doctorNotes,
                "date" to FieldValue.serverTimestamp(),
                "status" to "Completed"
            )

            db.collection("medical_logs")
                .add(log)
                .addOnSuccessListener {
                    // hideProgressDialog()
                    Toast.makeText(this, "Medical Log Saved", Toast.LENGTH_SHORT).show()
                    finish() // Close activity
                }
                .addOnFailureListener { e ->
                    // hideProgressDialog()
                    Toast.makeText(this, "Error saving log: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true // Re-enable on failure
                }
        }
    }
}
