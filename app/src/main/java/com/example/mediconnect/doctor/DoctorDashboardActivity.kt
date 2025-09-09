/* ---------- DoctorDashboardActivity with Navigation Drawer + Dashboard Stats ---------- */
package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.IntroActivity
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DoctorDashboardActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Dashboard UI
    private lateinit var tvAppointmentCount: TextView
    private lateinit var tvPatientCount: TextView
    private lateinit var tvFeedbackCount: TextView
    private lateinit var tvAppointmentPercent: TextView
    private lateinit var tvPatientPercent: TextView
    private lateinit var tvFeedbackPercent: TextView
    private lateinit var progressAppointments: ProgressBar
    private lateinit var progressPatients: ProgressBar
    private lateinit var progressFeedback: ProgressBar

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        hideStatusBar()
        setupToolbarAndDrawer()
        setupBackPressHandler()
        bindDashboardViews()

        // Load user info for drawer
        FireStoreClass().loadUserData(this)

        // Load dashboard data
        loadDashboardStats()
    }

    /* ---------- UI Setup ---------- */
    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun setupToolbarAndDrawer() {
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
    }

    private fun setupBackPressHandler() {
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

    private fun bindDashboardViews() {
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount)
        tvPatientCount = findViewById(R.id.tvPatientCount)
        tvFeedbackCount = findViewById(R.id.tvFeedbackCount)
        tvAppointmentPercent = findViewById(R.id.tvAppointmentPercent)
        tvPatientPercent = findViewById(R.id.tvPatientPercent)
        tvFeedbackPercent = findViewById(R.id.tvFeedbackPercent)
        progressAppointments = findViewById(R.id.progressAppointments)
        progressPatients = findViewById(R.id.progressPatients)
        progressFeedback = findViewById(R.id.progressFeedback)
    }

    /* ---------- Load Dashboard Data ---------- */
    private fun loadDashboardStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Count today's booked appointments
        db.collection("appointments")
            .whereEqualTo("date", today)
            .whereEqualTo("status", "booked") // ✅ only booked appointments
            .get()
            .addOnSuccessListener { snapshot ->
                val bookedCount = snapshot.size()
                tvAppointmentCount.text = "Booked: $bookedCount"

                // Example percentage: assume 10 slots max per day
                val percent = if (bookedCount > 0) {
                    ((bookedCount.toDouble() / 10.0) * 100).toInt().coerceAtMost(100)
                } else 0

                progressAppointments.progress = percent
                tvAppointmentPercent.text = "$percent%"
            }

        // ✅ Count patients from users collection
        db.collection("users")
            .whereEqualTo("role", "patient")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                tvPatientCount.text = "Patients: $count"

                val percent = if (count > 0) (count * 4).coerceAtMost(100) else 0
                progressPatients.progress = percent
                tvPatientPercent.text = "$percent%"
            }

        // ✅ Get average feedback rating (1–5 stars)
        db.collection("patient_feedback")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    var totalRating = 0.0
                    var totalCount = 0

                    for (doc in snapshot) {
                        val rating = doc.getDouble("rating") ?: 0.0
                        totalRating += rating
                        totalCount++
                    }

                    val avgRating = if (totalCount > 0) totalRating / totalCount else 0.0

                    // Convert to percentage (out of 5 stars → 100%)
                    val percent = ((avgRating / 5.0) * 100).toInt()

                    tvFeedbackCount.text = "Avg Rating: %.1f".format(avgRating)
                    progressFeedback.progress = percent
                    tvFeedbackPercent.text = "$percent%"
                } else {
                    tvFeedbackCount.text = "No Feedback"
                    progressFeedback.progress = 0
                    tvFeedbackPercent.text = "0%"
                }
            }
    }


    /* ---------- Navigation ---------- */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_doctor_profile -> startActivity(Intent(this, DoctorProfile::class.java))
            R.id.nav_doctor_appointment -> startActivity(Intent(this, DoctorAppointment::class.java))
            R.id.nav_doctor_appointment_history -> startActivity(Intent(this, DoctorAppointmentHistory::class.java))
            R.id.nav_doctor_medical_log -> startActivity(Intent(this, DoctorMedicalLog::class.java))
            R.id.nav_doctor_feedback -> startActivity(Intent(this, DoctorFeedback::class.java))
            R.id.nav_doctor_sign_out -> signOutUser()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun signOutUser() {
        FirebaseAuth.getInstance().signOut()

        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, IntroActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /* ---------- User Info in Drawer ---------- */
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
    }
}
