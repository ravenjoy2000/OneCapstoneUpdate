package com.example.mediconnect.patient

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
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
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Views
    private lateinit var profileImageView: CircleImageView
    private lateinit var doctorName: TextView
    private lateinit var doctorEmail: TextView
    private lateinit var doctorPhone: TextView
    private lateinit var doctorAddress: TextView
    private lateinit var doctorSpecialty: TextView

    companion object {
        private const val MY_PROFILE_REQUEST_CODE = 101
        private const val MY_APPOINTMENT_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // UI setup
        setupActionBar()
        setupNavigationDrawer()
        handleBackPressBehavior()

        // View initialization
        doctorName = findViewById(R.id.tv_doctor_name)
        doctorEmail = findViewById(R.id.tv_email_doctor)
        doctorPhone = findViewById(R.id.tv_doctor_phone)
        doctorAddress = findViewById(R.id.tv_doctor_address)
        doctorSpecialty = findViewById(R.id.tv_doctor_specialty)
        profileImageView = findViewById(R.id.iv_doctor_icon)


        val bookNowBtn = findViewById<Button>(R.id.btn_book_now)
        bookNowBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            checkIfUserHasActiveAppointment(userId) { hasActive ->
                if (hasActive) {
                    bookNowBtn.isEnabled = false
                    bookNowBtn.text = "You already have an appointment"
                } else {
                    startActivity(Intent(this, appointment::class.java))
                }
            }
        }

        // Load data
        FireStoreClass().loadUserData(this)
        fetchDoctorInformation()
    }

    private fun fetchDoctorInformation() {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("role", "doctor")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    doctorName.text = doc.getString("name") ?: "N/A"
                    doctorEmail.text = doc.getString("email") ?: "N/A"
                    doctorPhone.text = doc.getString("phone") ?: "N/A"
                    doctorAddress.text = doc.getString("address") ?: "N/A"
                    doctorSpecialty.text = doc.getString("specialty") ?: "N/A"

                    val imageUrl = doc.getString("image") ?: ""
                    Glide.with(this)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(profileImageView)
                } else {
                    Log.d("Firestore", "No doctor found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching doctor", e)
            }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu)
        }

        toolbar.setNavigationOnClickListener { toggleDrawer() }
    }

    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun handleBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    doubleBackToExit()
                }
            }
        })
    }

    private fun toggleDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_my_appointment -> {
                startActivity(Intent(this, MyAppointment::class.java))
            }
            R.id.nav_my_appointment_history -> {
                startActivity(Intent(this, AppointmentHistoryUser::class.java))
            }
            R.id.nav_medical_log -> {
                startActivity(Intent(this, MedicalLogs::class.java))
            }
            R.id.nav_patent_feedback -> {
                startActivity(Intent(this, PatentFeedback::class.java))
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                // Also sign out from Google
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
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FireStoreClass().loadUserData(this)
        } else {
            Log.e("Cancelled", "Cancelled or no result")
        }
    }

    fun updateNavigationUserDetails(user: User) {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        val ivUserImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_image)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_username)

        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(ivUserImage)

        tvUsername.text = user.name
    }

    fun openClinicWebsite(view: View) {
        val url = "https://www.facebook.com/profile.php?id=100092741389883"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}