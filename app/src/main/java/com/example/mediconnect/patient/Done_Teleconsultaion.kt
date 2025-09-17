package com.example.mediconnect.patient

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R

class Done_Teleconsultaion : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_done_teleconsultaion)

        // 🔙 Back button
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            onBackPressed() // balik sa previous screen
        }

        // 💸 Payment buttons
        val btnGcash = findViewById<MaterialButton>(R.id.btn_gcash)
        val btnMaya = findViewById<MaterialButton>(R.id.btn_maya)
        val txtSkip = findViewById<TextView>(R.id.txt_skip_payment)

        btnGcash.setOnClickListener {
            // TODO: Ilagay mo dito yung GCash payment flow mo
            // Example: buksan yung GCash activity
            val intent = Intent(this, Gcash::class.java)
            startActivity(intent)
        }

        btnMaya.setOnClickListener {
            // TODO: Ilagay mo dito yung Maya payment flow mo
            val intent = Intent(this, Maya::class.java)
            startActivity(intent)
        }

        txtSkip.setOnClickListener {
            // Kapag skip, balik sa Main/Dashboard
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
