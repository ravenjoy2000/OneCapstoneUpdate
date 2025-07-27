// Package declaration for MainActivity
package com.example.mediconnect.activities

// Import necessary Android and Firebase components
import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

/**
 * MainActivity handles navigation drawer, toolbar setup, user information display,
 * and activity navigation (like MyProfile and SignOut).
 */
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private val MY_PROFILE_REQUEST_CODE = 101
        private val MY_APPOINTMENT_REQUEST_CODE = 102

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Make layout fullscreen (edge-to-edge)
        setContentView(R.layout.activity_main) // Set content view to main layout

        setupActionBar() // Setup toolbar and drawer icon
        setupNavigationDrawer() // Handle drawer menu interactions
        handleBackPressBehavior() // Handle custom back press behavior

        // Load user data and update UI in drawer
        FireStoreClass().loadUserData(this)

        val bookNowBtn = findViewById<Button>(R.id.btn_book_now)
        bookNowBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            checkIfUserHasActiveAppointment(userId) { hasActive ->
                if (hasActive) {
                    bookNowBtn.isEnabled = false
                    bookNowBtn.text = "You already have an appointment"
                } else {
                    // Go to Appointment Booking
                    val intent = Intent(this, appointment::class.java)
                    startActivity(intent)
                }
            }
        }

        val websiteTextView = findViewById<TextView>(R.id.tv_clinic_website)

        websiteTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.facebook.com/profile.php?id=100092741389883&sk=about")
            startActivity(intent)
        }



    }

    /**
     * Sets up toolbar and drawer menu icon (hamburger icon)
     */
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu)
        }

        toolbar.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    /**
     * Open or close drawer depending on its current state
     */
    private fun toggleDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * Handle custom back button behavior to support double back press
     */
    private fun handleBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this) {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                doubleBackToExit()
            }
        }
    }

    /**
     * Set navigation item click listener
     */
    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    /**
     * Handle result from MyProfileActivity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FireStoreClass().loadUserData(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    /**
     * Handle navigation item selection events
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        when (item.itemId) {
            R.id.nav_my_profile -> {
                val intent = Intent(this, MyProfileActivity::class.java)
                startActivity(intent)
            }

            R.id.nav_my_appointment -> {
                val intent = Intent(this, MyAppointment::class.java)
                startActivity(intent)
            }

            R.id.nav_my_appointment_history -> {
                val intent = Intent(this, AppointmentHistoryUser::class.java)
                startActivity(intent)
            }

            R.id.nav_sign_out -> {
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



    /**
     * Display user details (image and name) in the navigation drawer
     */
    fun updateNavigationUserDetails(user: User) {
        Glide.with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(findViewById(R.id.iv_user_image))

        findViewById<TextView>(R.id.tv_username).text = user.name
    }


    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled_once"))
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)
            }
            .addOnFailureListener {
                onResult(false) // Assume no active appointment on failure
            }
    }



}
