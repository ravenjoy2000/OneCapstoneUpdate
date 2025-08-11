package com.example.mediconnect.patient

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ConsultationStart : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var waitingMessage: TextView
    private lateinit var btnMedicalLog: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_start)

        progressBar = findViewById(R.id.progressBar)
        waitingMessage = findViewById(R.id.waitingMessage)
        btnMedicalLog = findViewById(R.id.btnMedicalLog)

        btnMedicalLog.setOnClickListener {
            val intent = Intent(this, MedicalLogs::class.java)
            startActivity(intent)
        }

        listenForConsultationStatus()
    }

    private fun listenForConsultationStatus() {
        val patientId = auth.currentUser?.uid ?: return

        // Example: Listening to consultation status in Firestore
        db.collection("appointments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "ongoing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val appointment = snapshot.documents[0]
                    val consultationStatus = appointment.getString("consultationStatus") ?: ""

                    if (consultationStatus == "finished") {
                        onConsultationFinished()
                    } else {
                        showWaiting()
                    }
                }
            }
    }

    private fun showWaiting() {
        progressBar.visibility = View.VISIBLE
        waitingMessage.text = "Waiting for consultation to finish..."
        btnMedicalLog.visibility = View.GONE
    }

    private fun onConsultationFinished() {
        progressBar.visibility = View.GONE
        waitingMessage.text = "Consultation finished."
        btnMedicalLog.visibility = View.VISIBLE
    }
}
