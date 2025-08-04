package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.IntroActivity
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.User
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDashboardActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_doctor_dashboard)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_doctor_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView = findViewById(R.id.nav_doctor_view)
        navigationView.setNavigationItemSelectedListener(this)

        handleBackPressBehavior()

        // Load doctor user data and then setup header
        FireStoreClass().loadUserData(this)
    }

    private fun handleBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    doubleBackToExit()
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_doctor_profile -> startActivity(Intent(this, DoctorProfile::class.java))
            R.id.nav_doctor_appointment -> startActivity(Intent(this, DoctorAppointment::class.java))
            R.id.nav_doctor_appointment_history -> startActivity(Intent(this, DoctorAppointmentHistory::class.java))
            R.id.nav_doctor_medical_log -> startActivity(Intent(this, DoctorMedicalLog::class.java))
            R.id.nav_doctor_feedback -> startActivity(Intent(this, DoctorFeedback::class.java))
            R.id.nav_doctor_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigationUserDetails(user: User) {
        val headerView = navigationView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.iv_doctor_image)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_doctor_user)

        // Load and show doctor photo
        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(imageView)

        // Set username
        tvUsername.text = user.name

        // Load booked appointments for current doctor
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            loadDoctorAppointments(currentUserId)
        }
    }

    private fun loadDoctorAppointments(doctorId: String) {
        val db = FirebaseFirestore.getInstance()
        showProgressDialog("Loading appointments...")

        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("status", "booked")
            .get()
            .addOnSuccessListener { documents ->
                hideProgressDialog()
                Log.d("DoctorAppointments", "Fetched ${documents.size()} documents.")

                if (documents.isEmpty) {
                    Toast.makeText(this, "No booked appointments found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val appointments = documents.map { doc ->
                    Appointment(
                        patientName = doc.getString("patientName") ?: "",
                        doctorName = doc.getString("doctorName") ?: "",
                        status = doc.getString("status") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("timeSlot") ?: "",
                        mode = doc.getString("mode") ?: "",
                        location = doc.getString("location") ?: "",
                        note = doc.getString("notes") ?: "",
                        reason = doc.getString("reason") ?: "",
                        previousDate = doc.getString("previousDate") ?: ""
                    )
                }

                setupRecyclerView(appointments)
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Log.e("DoctorAppointments", "Error: ${e.message}", e)
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView(appointments: List<Appointment>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_current_appointments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = DoctorAppointmentAdapter(appointments) { appointment ->
            val intent = Intent(this, AppointmentDetailsActivity::class.java)
            intent.putExtra("appointment_data", appointment)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
    }
}
