// Package declaration for MainActivity
package com.example.mediconnect.activities

// Import necessary Android and Firebase components
import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
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

/**
 * MainActivity handles navigation drawer, toolbar setup, user information display,
 * and activity navigation (like MyProfile and SignOut).
 */
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
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
                // Open MyProfileActivity
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }

            R.id.nav_sign_out -> {
                // Sign out user and return to IntroActivity
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START) // Always close drawer
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
}
