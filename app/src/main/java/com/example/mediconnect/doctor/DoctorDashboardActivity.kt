package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.IntroActivity
import com.example.mediconnect.doctor_adapter.DoctorAppointmentAdapter
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.models.AppointmentListItem
import com.example.mediconnect.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoctorDashboardActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Declare DrawerLayout para sa side navigation drawer
    private lateinit var drawerLayout: DrawerLayout

    // Declare NavigationView para sa drawer menu
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_doctor_dashboard)  // I-set ang layout ng dashboard

        hideStatusBar()                // Itago ang status bar para fullscreen experience
        setupToolbarAndDrawer()        // I-setup ang toolbar at navigation drawer
        setupBackPressHandler()        // I-handle ang back press para isara ang drawer o exit app
        setupSwipeRefresh()            // I-setup ang swipe-to-refresh para mag-refresh ng appointments

        FireStoreClass().loadUserData(this)  // Load ang detalye ng doctor user mula Firestore
    }

    /* ---------- UI Setup ---------- */

    // Function para itago ang status bar sa iba't ibang Android versions
    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())  // For Android 11+
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )   // For older Android versions
        }
    }

    // I-setup ang toolbar at side drawer toggle
    private fun setupToolbarAndDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_doctor_dashboard)
        setSupportActionBar(toolbar)   // Itakda ang toolbar bilang action bar

        drawerLayout = findViewById(R.id.drawer_doctor_layout)  // Kunin ang DrawerLayout mula sa layout
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )   // Gumawa ng toggle button sa toolbar para buksan/sara ang drawer

        drawerLayout.addDrawerListener(toggle)   // I-add ang toggle listener sa drawer
        toggle.syncState()                        // Sync ang toggle state para maayos ang icon

        navigationView = findViewById(R.id.nav_doctor_view)  // Kunin ang NavigationView (drawer menu)
        navigationView.setNavigationItemSelectedListener(this)  // I-set ang item selected listener
    }

    // I-setup ang swipe-to-refresh functionality para i-refresh ang appointment list
    private fun setupSwipeRefresh() {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            FirebaseAuth.getInstance().currentUser?.uid?.let {
                loadDoctorAppointments(it, showProgress = false)  // Reload ang mga appointment ng doctor
            }
            swipeRefresh.isRefreshing = false   // Patayin ang refresh animation
        }
    }

    // I-handle ang back press behavior, close drawer muna bago exit app
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)   // Isara ang drawer kung bukas
                } else {
                    doubleBackToExit()   // Tawagin ang function para double back tap para exit
                }
            }
        })
    }

    /* ---------- Navigation ---------- */

    // I-handle ang pagpili ng item sa navigation drawer menu
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_doctor_profile -> startActivity(Intent(this, DoctorProfile::class.java))  // Profile screen
            R.id.nav_doctor_appointment -> startActivity(Intent(this, DoctorAppointment::class.java))  // Current appointments
            R.id.nav_doctor_appointment_history -> startActivity(Intent(this, DoctorAppointmentHistory::class.java))  // History ng appointments
            R.id.nav_doctor_medical_log -> startActivity(Intent(this, DoctorMedicalLog::class.java))  // Medical logs
            R.id.nav_doctor_feedback -> startActivity(Intent(this, DoctorFeedback::class.java))  // Feedback page
            R.id.nav_doctor_sign_out -> signOutUser()   // Sign out ng user
        }
        drawerLayout.closeDrawer(GravityCompat.START)  // Isara ang drawer pagkatapos pumili
        return true
    }

    // Function para mag-sign out ang user at logout sa Google Sign-In
    private fun signOutUser() {
        FirebaseAuth.getInstance().signOut()    // Firebase sign out

        // Setup Google sign-in client para logout sa Google account
        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Request ID token
                .requestEmail()   // Request email
                .build()
        )

        // Sign out sa Google account, pagkatapos balik sa IntroActivity
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, IntroActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Clear history ng activities
            startActivity(intent)   // Simulan ang IntroActivity (login screen)
            finish()               // Tapusin ang kasalukuyang activity
        }
    }

    /* ---------- Data Loading ---------- */

    // I-update ang user details sa navigation drawer header kapag nakuha ang user data
    fun updateNavigationUserDetails(user: User) {
        val headerView = navigationView.getHeaderView(0)     // Kunin ang header view ng navigation drawer
        val imageView = headerView.findViewById<ImageView>(R.id.iv_doctor_image)  // ImageView ng user image
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_doctor_user)  // TextView ng user name

        // I-load ang user image gamit ang Glide (efficient image loading)
        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)   // Placeholder habang naglo-load
            .into(imageView)

        tvUsername.text = user.name  // Ipakita ang pangalan ng doctor sa drawer header

        // Kunin ang kasalukuyang user UID at i-load ang mga appointments niya
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            loadDoctorAppointments(it)
        }
    }

    // I-load ang listahan ng mga naka-book na appointment ng doctor mula Firestore
    private fun loadDoctorAppointments(doctorId: String, showProgress: Boolean = true) {
        val db = FirebaseFirestore.getInstance()

        if (showProgress) showProgressDialog("Loading appointments...")   // Ipakita loading dialog kung kailangan

        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)   // Filter by doctor ID
            .whereEqualTo("status", "booked")     // Filter lang ang mga naka-book na appointment
            .get()
            .addOnSuccessListener { documents ->
                if (showProgress) hideProgressDialog()   // Itago ang loading dialog

                Log.d("DoctorAppointments", "Fetched ${documents.size()} documents.")  // Log ng bilang ng nakuha

                if (documents.isEmpty) {
                    Toast.makeText(this, "No booked appointments found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener  // Walang appointment, tapos na
                }

                // I-map ang bawat document sa Appointment data class
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

                setupRecyclerView(appointments)   // I-display ang mga appointments sa RecyclerView
            }
            .addOnFailureListener { e ->
                if (showProgress) hideProgressDialog()   // Itago loading dialog kapag may error
                Log.e("DoctorAppointments", "Error: ${e.message}", e)  // Log ang error
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    /* ---------- RecyclerView Setup ---------- */

    // I-setup ang RecyclerView gamit ang listahan ng mga appointment (grouped by date)
    private fun setupRecyclerView(appointments: List<Appointment>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_current_appointments)
        recyclerView.layoutManager = LinearLayoutManager(this)  // Vertical list layout

        val groupedList = groupAppointmentsByDate(appointments) // I-group ang mga appointment ayon sa petsa

        // Gumawa ng adapter na may click listener para buksan ang detalye ng appointment
        val adapter = DoctorAppointmentAdapter(groupedList) { appointment ->
            val intent = Intent(this, AppointmentDetailsActivity::class.java)
            intent.putExtra("appointment_data", appointment)   // Ipadala ang appointment object sa detalye screen
            startActivity(intent)
        }
        recyclerView.adapter = adapter  // I-set ang adapter sa RecyclerView
    }

    // I-group ang mga appointment ayon sa petsa para sa mas maayos na display
    private fun groupAppointmentsByDate(appointments: List<Appointment>): List<AppointmentListItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())   // Format ng petsa
        val today = sdf.format(Date())                                  // Kunin ang kasalukuyang petsa

        val grouped = appointments.groupBy { it.date }   // Group appointments by date
        val sortedDates = grouped.keys.sorted()           // Ayusin ang mga petsa

        val result = mutableListOf<AppointmentListItem>()  // Listahang paglalagyan ng grouped items
        for (date in sortedDates) {
            val label = if (date == today) {
                "Today's Appointment"          // Label kapag ngayong araw ang petsa
            } else {
                val dateObj = sdf.parse(date)
                val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(dateObj!!)
                "$dayName Appointment"         // Label para sa ibang araw (e.g. Monday Appointment)
            }

            result.add(AppointmentListItem.Header(label, date))    // Idagdag ang header ng date group
            grouped[date]?.forEach { appointment ->
                result.add(AppointmentListItem.AppointmentItem(appointment))   // Idagdag ang bawat appointment sa ilalim ng header
            }
        }
        return result  // Ibalik ang grouped list para gamitin sa adapter
    }
}
