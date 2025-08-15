package com.example.mediconnect.patient

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsultationStart : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var waitingMessage: TextView
    private lateinit var btnMedicalLog: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var canUpdateUI = false
    private var consultationFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_start)

        progressBar = findViewById(R.id.progressBar)
        waitingMessage = findViewById(R.id.waitingMessage)
        btnMedicalLog = findViewById(R.id.btnMedicalLog)

        // Button click will only work when consultationFinished is true
        btnMedicalLog.setOnClickListener {
            if (consultationFinished) {
                showConfirmationDialog()
            }
        }

        // Make button visible but initially disabled
        btnMedicalLog.visibility = View.VISIBLE
        btnMedicalLog.isEnabled = false
        btnMedicalLog.alpha = 0.5f // visual cue for disabled state

        showWaiting()

        // Allow UI update after 15s delay
        Handler(Looper.getMainLooper()).postDelayed({
            canUpdateUI = true
        }, 15_000)

        listenForConsultationStatus()
    }

    private fun listenForConsultationStatus() {
        val patientId = auth.currentUser?.uid ?: return

        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "ongoing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val appointment = snapshot.documents[0]
                    val consultationStatus = appointment.getString("consultationStatus") ?: ""

                    if (consultationStatus == "finished") {
                        if (canUpdateUI) {
                            onConsultationFinished()
                        } else {
                            showWaiting()
                        }
                    } else {
                        showWaiting()
                    }
                }
            }
    }

    private fun showWaiting() {
        progressBar.visibility = View.VISIBLE
        waitingMessage.text = "Waiting for consultation to finish..."
        consultationFinished = false
        btnMedicalLog.isEnabled = false
        btnMedicalLog.alpha = 0.5f
    }

    private fun onConsultationFinished() {
        progressBar.visibility = View.GONE
        waitingMessage.text = "Consultation finished."
        consultationFinished = true
        btnMedicalLog.isEnabled = true
        btnMedicalLog.alpha = 1f
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("View Medical Log")
            .setMessage("Are you sure you want to view your medical log for this consultation?")
            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                val intent = Intent(this, MedicalLogs::class.java)
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }
}
