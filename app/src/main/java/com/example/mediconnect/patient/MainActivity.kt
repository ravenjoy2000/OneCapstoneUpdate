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

// Pangunahing activity para sa patient side ng app
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Mga view na gagamitin sa activity
    private lateinit var profileImageView: CircleImageView   // Profile picture ng doktor
    private lateinit var doctorName: TextView                // Pangalan ng doktor
    private lateinit var doctorEmail: TextView               // Email ng doktor
    private lateinit var doctorPhone: TextView               // Telepono ng doktor
    private lateinit var doctorAddress: TextView             // Address ng doktor
    private lateinit var doctorSpecialty: TextView           // Specialty ng doktor

    companion object {
        private const val MY_PROFILE_REQUEST_CODE = 101       // Request code para sa profile update
        private const val MY_APPOINTMENT_REQUEST_CODE = 102   // Request code para appointment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                                      // Para sa edge-to-edge UI effect
        setContentView(R.layout.activity_main)                  // I-set ang layout ng activity

        // Itago ang status bar depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // I-setup ang action bar at navigation drawer
        setupActionBar()
        setupNavigationDrawer()
        handleBackPressBehavior()

        // Kunin ang mga views mula sa layout
        doctorName = findViewById(R.id.tv_doctor_name)
        doctorEmail = findViewById(R.id.tv_email_doctor)
        doctorPhone = findViewById(R.id.tv_doctor_phone)
        doctorAddress = findViewById(R.id.tv_doctor_address)
        doctorSpecialty = findViewById(R.id.tv_doctor_specialty)
        profileImageView = findViewById(R.id.iv_doctor_icon)

        // Button para mag-book ng appointment
        val bookNowBtn = findViewById<Button>(R.id.btn_book_now)
        bookNowBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            // Tingnan kung may existing active appointment ang user bago payagang mag-book
            checkIfUserHasActiveAppointment(userId) { hasActive ->
                if (hasActive) {
                    bookNowBtn.isEnabled = false                 // Disable ang button
                    bookNowBtn.text = "You already have an appointment" // Palitan ang text
                } else {
                    startActivity(Intent(this, appointment::class.java)) // Puntahan ang booking screen
                }
            }
        }

        // Load user data mula sa Firestore
        FireStoreClass().loadUserData(this)

        // Kunin ang impormasyon ng doktor mula sa Firestore
        fetchDoctorInformation()
    }

    // Kunin ang impormasyon ng doktor mula sa Firestore database
    private fun fetchDoctorInformation() {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("role", "doctor")                    // Filter para sa mga user na may role na doktor
            .limit(1)                                           // Kuhanin lang ang isang doktor (first)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    // I-update ang mga TextView gamit ang data ng doktor
                    doctorName.text = doc.getString("name") ?: "N/A"
                    doctorEmail.text = doc.getString("email") ?: "N/A"
                    doctorPhone.text = doc.getString("phone") ?: "N/A"
                    doctorAddress.text = doc.getString("address") ?: "N/A"
                    doctorSpecialty.text = doc.getString("specialty") ?: "N/A"

                    val imageUrl = doc.getString("image") ?: ""
                    // I-load gamit ang Glide ang larawan ng doktor
                    Glide.with(this)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(profileImageView)
                } else {
                    Log.d("Firestore", "No doctor found.")      // Walang doktor na nahanap
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching doctor", e)    // Kapag may error sa pagkuha ng data
            }
    }

    // I-setup ang action bar sa taas ng screen
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)                      // Ipakita ang back/home button
            setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu) // Icon para sa navigation drawer
        }

        // Kapag pinindot ang icon, toggle ang drawer
        toolbar.setNavigationOnClickListener { toggleDrawer() }
    }

    // I-setup ang navigation drawer at i-set ang listener sa item selection
    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    // I-handle ang behavior kapag pinindot ang back button ng device
    private fun handleBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)  // Isara muna drawer kung bukas pa
                } else {
                    doubleBackToExit()                              // Exit app kapag 2 beses pinindot ang back
                }
            }
        })
    }

    // Buksan o isara ang navigation drawer depende sa estado nito
    private fun toggleDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    // Kapag pumili ng menu item sa navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        when (item.itemId) {
            R.id.nav_my_profile -> {
                // Buksan ang MyProfileActivity at hintayin ang resulta
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_my_appointment -> {
                // Buksan ang MyAppointment activity
                startActivity(Intent(this, MyAppointment::class.java))
            }
            R.id.nav_my_appointment_history -> {
                // Buksan ang appointment history ng user
                startActivity(Intent(this, AppointmentHistoryUser::class.java))
            }
            R.id.nav_medical_log -> {
                // Buksan ang medical logs screen
                startActivity(Intent(this, MedicalLogs::class.java))
            }
            R.id.nav_patent_feedback -> {
                // Buksan ang patient feedback screen
                startActivity(Intent(this, PatentFeedback::class.java))
            }
            R.id.nav_sign_out -> {
                // Mag-sign out mula sa Firebase Auth
                FirebaseAuth.getInstance().signOut()

                // Mag-sign out din mula sa Google account
                val googleSignInClient = GoogleSignIn.getClient(
                    this,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                )

                googleSignInClient.signOut().addOnCompleteListener {
                    // Pagkatapos mag-sign out, pumunta sa IntroActivity at linisin ang activity stack
                    val intent = Intent(this, IntroActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)   // Isara ang drawer pagkatapos pumili ng menu item
        return true
    }

    // Pagbalik mula sa ibang activity, tulad ng profile update
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FireStoreClass().loadUserData(this)          // Reload user data kapag may pagbabago sa profile
        } else {
            Log.e("Cancelled", "Cancelled or no result")  // Kapag kinansela o walang resulta
        }
    }

    // I-update ang navigation drawer header gamit ang impormasyon ng user
    fun updateNavigationUserDetails(user: User) {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        val ivUserImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_image)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_username)

        Glide.with(this)
            .load(user.image)                              // I-load ang profile image ng user
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder) // Placeholder image habang naglo-load
            .into(ivUserImage)

        tvUsername.text = user.name                        // Ipakita ang pangalan ng user
    }

    // Function para buksan ang clinic website gamit ang browser
    fun openClinicWebsite(view: View) {
        val url = "https://www.facebook.com/profile.php?id=100092741389883"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)                              // Simulan ang intent para buksan ang URL
    }

    // Tingnan kung may active appointment ang user
    private fun checkIfUserHasActiveAppointment(userId: String, onResult: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("appointments")
            .whereEqualTo("patientId", userId)                  // Filter by patient ID
            .whereIn("status", listOf("booked", "rescheduled_once")) // Status na booked o rescheduled once
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)                     // I-return kung may mga dokumento
            }
            .addOnFailureListener {
                onResult(false)                                  // Sa failure, return false
            }
    }
}
