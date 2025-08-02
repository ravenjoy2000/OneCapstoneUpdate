package com.example.mediconnect.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.doctor.DoctorDashboardActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ===== FULLSCREEN =====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // ===== CUSTOM FONT =====
        val tvAppName = findViewById<TextView>(R.id.tv_app_name)
        val typeface = Typeface.createFromAsset(assets, "Billy Bounce.otf")
        tvAppName.typeface = typeface

        // ===== DELAY THEN CHECK USER =====
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUserID = FireStoreClass().getCurrentUserID()

            if (currentUserID.isNotEmpty()) {
                FireStoreClass().getCurrentUserRole { role ->
                    when (role) {
                        "doctor" -> startActivity(Intent(this, DoctorDashboardActivity::class.java))
                        "patient" -> startActivity(Intent(this, MainActivity::class.java))
                        else -> startActivity(Intent(this, IntroActivity::class.java)) // fallback
                    }
                    finish()
                }
            } else {
                startActivity(Intent(this, IntroActivity::class.java))
                finish()
            }
        }, 2500)
    }
}
