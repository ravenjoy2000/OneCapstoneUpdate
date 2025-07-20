package com.example.mediconnect.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mediconnect.R

class MyProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_profile)

        setupActionBar()

    }


    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_my_profile_activity)) // Gamitin ang toolbar bilang ActionBar

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Ipakita ang ← (back/up) button

        supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24) // Palitan ang icon ng drawer menu

        supportActionBar?.title = resources.getString(R.string.my_profile_title) // Palitan ang title ng drawer menu

        val toolbar_my_profile_activity = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_my_profile_activity)
        toolbar_my_profile_activity.setNavigationOnClickListener {
            onBackPressed()
        }


    }


}