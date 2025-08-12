package com.example.mediconnect.patient

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

// Activity para ipakita ang estado ng consultation at maghintay hanggang matapos
class ConsultationStart : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar    // Progress bar para sa loading state
    private lateinit var waitingMessage: TextView    // TextView para ipakita ang status message
    private lateinit var btnMedicalLog: Button       // Button para pumunta sa Medical Logs screen
    private val db = FirebaseFirestore.getInstance() // Firestore database instance
    private val auth = FirebaseAuth.getInstance()    // Firebase Authentication instance

    private var canUpdateUI = false  // Flag para kontrolin kung pwede nang mag-update ng UI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_start)  // I-set ang layout ng activity

        // Kunin ang mga views mula sa layout
        progressBar = findViewById(R.id.progressBar)
        waitingMessage = findViewById(R.id.waitingMessage)
        btnMedicalLog = findViewById(R.id.btnMedicalLog)

        // Kapag pinindot ang Medical Log button, pumunta sa MedicalLogs activity
        btnMedicalLog.setOnClickListener {
            val intent = Intent(this, MedicalLogs::class.java)
            startActivity(intent)
        }

        // Ipakita agad ang loading screen habang naghihintay
        showWaiting()

        // Itakda ang 1 minutong delay bago payagang mag-update ang UI mula waiting papuntang finished
        Handler(Looper.getMainLooper()).postDelayed({
            canUpdateUI = true
        }, 15_000) // 60,000 milliseconds = 1 minuto

        // Simulan ang pakikinig sa Firestore para sa consultation status update
        listenForConsultationStatus()
    }

    // Function para mag-listen sa Firestore collection para malaman kung tapos na ang consultation
    private fun listenForConsultationStatus() {
        val patientId = auth.currentUser?.uid ?: return  // Kunin ang kasalukuyang user ID, kung wala, itigil

        db.collection("appointments")                    // Access sa "appointments" collection
            .whereEqualTo("patientId", patientId)       // Filter: appointments ng kasalukuyang pasyente
            .whereEqualTo("status", "ongoing")          // Filter: appointments na ongoing pa
            .addSnapshotListener { snapshot, error ->   // Mag-subscribe sa real-time changes
                if (error != null) return@addSnapshotListener  // Kung may error, wag mag-proceed

                if (snapshot != null && !snapshot.isEmpty) {  // Kung may data
                    val appointment = snapshot.documents[0]    // Kunin ang unang dokumento
                    val consultationStatus = appointment.getString("consultationStatus") ?: ""  // Kunin ang status

                    if (consultationStatus == "finished") {    // Kapag finished na ang consultation
                        if (canUpdateUI) {
                            onConsultationFinished()            // Tawagin ang function para ipakita na tapos na
                        } else {
                            showWaiting()                      // Hindi pa pwede mag-update, ipakita loading pa rin
                        }
                    } else {
                        showWaiting()                            // Hindi pa tapos, ipakita loading
                    }
                }
            }
    }

    // Ipakita ang loading indicator at status message na naghihintay pa
    private fun showWaiting() {
        progressBar.visibility = View.VISIBLE              // Ipakita ang progress bar
        waitingMessage.text = "Waiting for consultation to finish..." // I-update ang status text
        btnMedicalLog.visibility = View.GONE               // Itago ang medical log button habang naghihintay
    }

    // Ipakita na tapos na ang consultation at ipakita ang button para pumunta sa medical logs
    private fun onConsultationFinished() {
        progressBar.visibility = View.GONE                 // Itago ang progress bar
        waitingMessage.text = "Consultation finished."     // I-update ang status text na tapos na
        btnMedicalLog.visibility = View.VISIBLE            // Ipakita ang medical log button
    }
}
