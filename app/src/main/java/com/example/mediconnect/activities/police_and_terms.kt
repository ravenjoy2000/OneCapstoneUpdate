package com.example.mediconnect.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.patient.MainActivity

class police_and_terms : AppCompatActivity() {

    private lateinit var checkboxAgree: CheckBox
    private lateinit var btnAccept: Button
    private lateinit var btnDecline: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SharedPreferences for saving acceptance
        prefs = getSharedPreferences("MediConnectPrefs", MODE_PRIVATE)

        // If already accepted before, skip this screen
        if (prefs.getBoolean("accepted_terms", false)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_police_and_terms)

        // Init views
        checkboxAgree = findViewById(R.id.checkbox_agree_terms)
        btnAccept = findViewById(R.id.btn_accept)
        btnDecline = findViewById(R.id.btn_decline)

        // Enable "Accept" only when checkbox is checked
        checkboxAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
        }

        // Accept button
        btnAccept.setOnClickListener {
            // Save acceptance in SharedPreferences
            prefs.edit().putBoolean("accepted_terms", true).apply()

            goToMain()
        }

        // Decline button
        btnDecline.setOnClickListener {
            Toast.makeText(
                this,
                "You must accept the terms to use MediConnect.",
                Toast.LENGTH_SHORT
            ).show()
            finishAffinity() // closes the app
        }
    }

    private fun goToMain() {
        // Go to your main dashboard (Patient/Doctor MainActivity)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
