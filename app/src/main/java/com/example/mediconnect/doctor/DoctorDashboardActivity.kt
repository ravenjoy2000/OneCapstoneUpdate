package com.example.mediconnect.doctor

import android.content.Intent
import android.net.Uri
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
import com.google.firebase.firestore.Query

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

        setupNavigationDrawer()
        handleBackPressBehavior()

        // Load doctor user data
        FireStoreClass().loadUserData(this)
    }

    private fun setupNavigationDrawer() {
        navigationView = findViewById(R.id.nav_doctor_view)
        navigationView.setNavigationItemSelectedListener(this)

        val headerView = navigationView.getHeaderView(0)
        val tvWebsite = headerView.findViewById<TextView?>(R.id.tv_clinic_website)
        tvWebsite?.setOnClickListener {
            val url = "https://www.facebook.com/profile.php?id=100092741389883&sk=about"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
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

        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(imageView)

        tvUsername.text = user.name

        // Load appointments for this doctor
        loadDoctorAppointments(user.name)
    }

    private fun loadDoctorAppointments(doctorName: String) {
        val db = FirebaseFirestore.getInstance()

        showProgressDialog("Loading appointments...")

        db.collection("appointments")
            .whereEqualTo("doctorName", doctorName)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                hideProgressDialog()

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
                Log.e("DoctorAppointments", "Error fetching appointments: ${e.message}", e)
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
