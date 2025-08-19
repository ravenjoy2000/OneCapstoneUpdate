/* ---------- Updated DoctorDashboardActivity with Auto Top Refresh ---------- */
package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
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

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView

    private var cancelMenuItem: MenuItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        hideStatusBar()
        setupToolbarAndDrawer()
        setupBackPressHandler()
        setupSwipeRefresh()
        setupRecyclerAutoRefresh()

        FireStoreClass().loadUserData(this)
    }

    /* ---------- UI Setup ---------- */


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_dashboard, menu)
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        cancelMenuItem = menu.findItem(R.id.action_cancel)
        return super.onPrepareOptionsMenu(menu)
    }


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

    private fun setupSwipeRefresh() {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            FirebaseAuth.getInstance().currentUser?.uid?.let {
                loadDoctorAppointments(it, showProgress = false)
            }
            swipeRefresh.isRefreshing = false
        }
    }

    private fun setupRecyclerAutoRefresh() {
        recyclerView = findViewById(R.id.rv_current_appointments)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)

                if (dy > 0) {
                    // Restart the whole app
                    val intent = baseContext.packageManager
                        .getLaunchIntentForPackage(baseContext.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finishAffinity() // close all current activities
                }
            }
        })
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

    /* ---------- Data Loading ---------- */

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

        FirebaseAuth.getInstance().currentUser?.uid?.let {
            loadDoctorAppointments(it)
        }
    }

    private fun loadDoctorAppointments(doctorId: String, showProgress: Boolean = true) {
        val db = FirebaseFirestore.getInstance()

        if (showProgress) showProgressDialog("Loading appointments...")

        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("status", "booked")
            .get()
            .addOnSuccessListener { documents ->
                if (showProgress) hideProgressDialog()

                Log.d("DoctorAppointments", "Fetched ${documents.size()} documents.")

                if (documents.isEmpty) {
                    Toast.makeText(this, "No booked appointments found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

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

                setupRecyclerView(appointments)
            }
            .addOnFailureListener { e ->
                if (showProgress) hideProgressDialog()
                Log.e("DoctorAppointments", "Error: ${e.message}", e)
                Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show()
            }
    }

    /* ---------- RecyclerView Setup ---------- */

    private fun setupRecyclerView(appointments: List<Appointment>) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val groupedList = groupAppointmentsByDate(appointments)

        val adapter = DoctorAppointmentAdapter(
            groupedList,
            onAppointmentClick = { appointment ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("appointment_data", appointment)
                startActivity(intent)
            },
            onSelectionChanged = { count ->
                // Example: show selection count in toolbar or toast
                if (count > 0) {
                    Toast.makeText(this, "$count selected", Toast.LENGTH_SHORT).show()
                }
            }
        )

        recyclerView.adapter = adapter
    }


    private fun groupAppointmentsByDate(appointments: List<Appointment>): List<AppointmentListItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val grouped = appointments.groupBy { it.date }
        val sortedDates = grouped.keys.sorted()

        val result = mutableListOf<AppointmentListItem>()
        for (date in sortedDates) {
            val label = if (date == today) {
                "Today's Appointment"
            } else {
                val dateObj = sdf.parse(date)
                val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(dateObj!!)
                "$dayName Appointment"
            }

            result.add(AppointmentListItem.Header(label, date))
            grouped[date]?.forEach { appointment ->
                result.add(AppointmentListItem.AppointmentItem(appointment))
            }
        }
        return result
    }
}
