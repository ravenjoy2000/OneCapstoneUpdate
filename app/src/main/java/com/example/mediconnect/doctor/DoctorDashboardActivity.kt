package com.example.mediconnect.doctor // Package/folder kung saan nakalagay ang file na ito

// Mga Android at app imports
import android.content.Intent // Para makalipat sa ibang screen (activity)
import android.os.Build // Para malaman kung anong Android version ang gamit
import android.os.Bundle // Para magdala ng data sa pagitan ng activities
import android.util.Log // Para sa paglalagay ng debug logs
import android.view.MenuItem // Para sa pagpili ng items sa navigation menu
import android.view.WindowInsets // Para i-hide ang status bar (new method)
import android.view.WindowManager // Para i-hide ang status bar (old method)
import android.widget.ImageView // Para sa pagpapakita ng larawan
import android.widget.TextView // Para sa pagpapakita ng text
import android.widget.Toast // Para sa maliit na pop-up na notification

import androidx.activity.OnBackPressedCallback // Para makontrol ang behavior ng back button
import androidx.appcompat.app.ActionBarDrawerToggle // Para sa hamburger menu toggle
import androidx.appcompat.widget.Toolbar // Para sa toolbar
import androidx.core.view.GravityCompat // Para sa drawer open/close control
import androidx.drawerlayout.widget.DrawerLayout // Para sa navigation drawer
import androidx.recyclerview.widget.LinearLayoutManager // Layout para sa RecyclerView
import androidx.recyclerview.widget.RecyclerView // List view para sa appointments

// Third-party at custom imports
import com.bumptech.glide.Glide // Image loading library
import com.example.mediconnect.R // Resource IDs
import com.example.mediconnect.activities.BaseActivity // Base activity na may common functions
import com.example.mediconnect.activities.IntroActivity // Login/intro screen
import com.example.mediconnect.doctor_adapter.DoctorAppointmentAdapter // Adapter ng appointments
import com.example.mediconnect.firebase.FireStoreClass // Custom Firestore operations
import com.example.mediconnect.models.Appointment // Model para sa appointment
import com.example.mediconnect.models.AppointmentListItem // Model para sa grouped appointments
import com.example.mediconnect.models.User // Model para sa user data
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // Para sa Google Sign-In config
import com.google.android.material.navigation.NavigationView // Para sa navigation drawer UI
import com.google.firebase.auth.FirebaseAuth // Firebase authentication
import com.google.firebase.firestore.FirebaseFirestore // Firebase Firestore database

// Para sa date/time formatting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Main class ng Doctor Dashboard screen
class DoctorDashboardActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Mga variable para sa drawer navigation
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Ito ang unang method na tatakbo pag binuksan ang screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard) // I-set ang layout ng screen

        // Hide status bar depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // New API method
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            // Old API method
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Kunin ang toolbar mula sa layout at i-set bilang ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar_doctor_dashboard)
        setSupportActionBar(toolbar)

        // Setup ng drawer at hamburger menu
        drawerLayout = findViewById(R.id.drawer_doctor_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle) // Makinig sa drawer open/close events
        toggle.syncState() // I-sync ang estado ng hamburger icon

        // Kunin ang navigation view at set listener para sa menu clicks
        navigationView = findViewById(R.id.nav_doctor_view)
        navigationView.setNavigationItemSelectedListener(this)

        // I-setup ang custom back button behavior
        handleBackPressBehavior()

        // Load doctor data mula sa Firestore at update header UI
        FireStoreClass().loadUserData(this)
    }

    // Function para mag-logout
    private fun signOutUser() {
        FirebaseAuth.getInstance().signOut() // Logout sa Firebase

        // Logout din sa Google account
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        // Kapag tapos mag-signout, bumalik sa IntroActivity
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, IntroActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Linisin ang back stack
            startActivity(intent)
            finish()
        }
    }

    // Para kontrolin ang back button
    private fun handleBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START) // Isara ang drawer kung bukas
                } else {
                    doubleBackToExit() // Double back press bago mag-exit
                }
            }
        })
    }

    // Kapag may pinindot sa navigation menu
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_doctor_profile -> startActivity(Intent(this, DoctorProfile::class.java))
            R.id.nav_doctor_appointment -> startActivity(Intent(this, DoctorAppointment::class.java))
            R.id.nav_doctor_appointment_history -> startActivity(Intent(this, DoctorAppointmentHistory::class.java))
            R.id.nav_doctor_medical_log -> startActivity(Intent(this, DoctorMedicalLog::class.java))
            R.id.nav_doctor_feedback -> startActivity(Intent(this, DoctorFeedback::class.java))
            R.id.nav_doctor_sign_out -> signOutUser()
        }
        drawerLayout.closeDrawer(GravityCompat.START) // Isara drawer pagkatapos pumili
        return true
    }

    // Update ng navigation header details (profile picture at name)
    fun updateNavigationUserDetails(user: User) {
        val headerView = navigationView.getHeaderView(0) // Kunin ang header view ng navigation
        val imageView = headerView.findViewById<ImageView>(R.id.iv_doctor_image)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_doctor_user)

        // Load doctor profile image gamit ang Glide
        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder) // Default icon kung walang image
            .into(imageView)

        // Set doctor name
        tvUsername.text = user.name

        // Load appointments ng doctor
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            loadDoctorAppointments(currentUserId)
        }
    }

    // Function para i-group ang appointments ayon sa date
    private fun groupAppointmentsByDate(appointments: List<Appointment>): List<AppointmentListItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date()) // Kunin ang current date

        val grouped = appointments.groupBy { it.date } // Group by date
        val sortedDates = grouped.keys.sorted() // Sort dates ascending

        val result = mutableListOf<AppointmentListItem>()

        for (date in sortedDates) {
            val label = when {
                date == today -> "Today's Appointment" // Kung today
                else -> {
                    val dateObj = sdf.parse(date)
                    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(dateObj!!)
                    "$dayName Appointment" // Halimbawa: Monday Appointment
                }
            }

            result.add(AppointmentListItem.Header(label, date)) // Add header

            // Add appointments under that date
            grouped[date]?.forEach { appointment ->
                result.add(AppointmentListItem.AppointmentItem(appointment))
            }
        }

        return result
    }

    // Function para mag-load ng booked appointments mula sa Firestore
    private fun loadDoctorAppointments(doctorId: String) {
        val db = FirebaseFirestore.getInstance()
        showProgressDialog("Loading appointments...") // Show loading dialog

        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId) // Filter by doctor ID
            .whereEqualTo("status", "booked") // Only booked
            .get()
            .addOnSuccessListener { documents ->
                hideProgressDialog() // Hide loading dialog
                Log.d("DoctorAppointments", "Fetched ${documents.size()} documents.")

                if (documents.isEmpty) {
                    Toast.makeText(this, "No booked appointments found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Convert Firestore data to Appointment objects
                val appointments = documents.map { doc ->
                    Appointment(
                        patientName = doc.getString("patientName") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        appointmentId = doc.getString("appointmentId") ?: "",
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
                // Setup ng RecyclerView
                setupRecyclerView(appointments)
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Log.e("DoctorAppointments", "Error: ${e.message}", e)
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    // Function para i-setup ang RecyclerView ng appointments
    private fun setupRecyclerView(appointments: List<Appointment>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_current_appointments)
        recyclerView.layoutManager = LinearLayoutManager(this) // Vertical list

        // Group appointments by date para may header bawat araw
        val groupedList = groupAppointmentsByDate(appointments)

        // Gumamit ng custom adapter
        val adapter = DoctorAppointmentAdapter(groupedList) { appointment ->
            val intent = Intent(this, AppointmentDetailsActivity::class.java)
            intent.putExtra("appointment_data", appointment) // I-pass ang data
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
}
