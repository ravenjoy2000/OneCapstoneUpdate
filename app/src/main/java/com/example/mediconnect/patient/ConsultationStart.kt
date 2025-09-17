package com.example.mediconnect.patient

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsultationStart : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var waitingMessage: TextView
    private lateinit var consultationStatus: TextView
    private lateinit var btnMedicalLog: Button
    private lateinit var btnDoneConsultation: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var appointmentId: String? = null  // track current appointment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_start)

        progressBar = findViewById(R.id.progressBar)
        waitingMessage = findViewById(R.id.waitingMessage)
        consultationStatus = findViewById(R.id.status_of_consultation)
        btnMedicalLog = findViewById(R.id.btnMedicalLog)
        btnDoneConsultation = findViewById(R.id.btnDoneConsultation)

        // Hide button initially
        btnMedicalLog.visibility = View.GONE

        showWaiting()
        listenForConsultationStatus()

        // handle Done Consultation button
        btnDoneConsultation.setOnClickListener {
            markConsultationAsDone()
        }
    }

    private fun listenForConsultationStatus() {
        val patientId = auth.currentUser?.uid ?: return

        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val appointment = snapshot.documents[0]
                    appointmentId = appointment.id  // save doc id

                    val status = appointment.getString("status") ?: ""
                    val mode = appointment.getString("mode") ?: "In Person"

                    // Update consultation type UI
                    updateModeUI(mode)

                    if (status.lowercase() == "completed") {
                        onConsultationComplete()
                    } else {
                        showWaiting()
                    }
                }
            }
    }

    private fun updateModeUI(mode: String) {
        consultationStatus.text = mode
        consultationStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (mode.equals("Teleconsultation", true)) android.R.color.holo_blue_dark
                else android.R.color.holo_green_dark
            )
        )
    }

    private fun showWaiting() {
        progressBar.visibility = View.VISIBLE
        waitingMessage.text = "⏳ Waiting for consultation to complete..."
        btnMedicalLog.visibility = View.GONE
    }

    private fun onConsultationComplete() {
        progressBar.visibility = View.GONE
        waitingMessage.text = "✅ Consultation complete. Please log your medication."
        btnMedicalLog.visibility = View.VISIBLE
        btnMedicalLog.isEnabled = true
        btnMedicalLog.alpha = 1f

        btnMedicalLog.setOnClickListener {
            showMedicationPrompt()
        }
    }

    private fun showMedicationPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Log Your Medication")
            .setMessage(
                "Your consultation is complete. Please log your prescribed medication (name, dosage, frequency) to ensure proper intake."
            )
            .setCancelable(false)
            .setPositiveButton("Log Now") { _: DialogInterface, _: Int ->
                val intent = Intent(this, MedicationLogActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun markConsultationAsDone() {
        if (appointmentId == null) return

        db.collection("appointments").document(appointmentId!!)
            .update("status", "completed")
            .addOnSuccessListener {
                // Navigate to Done_Teleconsultation activity
                val intent = Intent(this, Done_Teleconsultaion::class.java)
                intent.putExtra("appointmentId", appointmentId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                waitingMessage.text = "❌ Failed to mark consultation as done."
                waitingMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
    }
}
