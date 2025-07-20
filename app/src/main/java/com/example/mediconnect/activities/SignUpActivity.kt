// Package kung saan nakalagay ang activity na ito
package com.example.mediconnect.activities

// Mga import para sa UI, Firebase, at Android features
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.firebase.auth.FirebaseAuth

// Ang activity na nagha-handle ng user sign-up (registration)
class SignUpActivity : BaseActivity() {

    // Main function na tumatakbo pag bukas ang activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Para sa full screen layout
        setContentView(R.layout.activity_sign_up) // I-set ang layout

        // ========= REMOVE STATUS BAR / FULLSCREEN ============
        window.setFlags( // Para sa old Android version
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Para sa bagong Android version
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }

        // =====================================================

        setupActionbar() // Tawagin ang toolbar setup

        // ========== BUTTON: Sign Up =============
        val btn_for_sign_up = findViewById<Button>(R.id.btn_sign_up)
        btn_for_sign_up.setOnClickListener {
            registersUser() // Kapag pinindot ang sign up, tawagin ang register function
        }
    }

    // ========== TOOLBAR / ACTION BAR SETUP ============
    private fun setupActionbar() {
        val toolbar_for_sign_up_activity =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar_for_sign_up_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true) // Enable back arrow
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24) // Set back icon
        }

        // Kapag pinindot ang back icon, bumalik sa previous screen
        toolbar_for_sign_up_activity.setNavigationOnClickListener { onBackPressed() }
    }

    // ========== VALIDATION NG USER INPUT ============
    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter name.") // Walang name
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.") // Walang email
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.") // Walang password
                false
            }
            else -> {
                true // Kung kumpleto lahat, magpatuloy
            }
        }
    }

    // ========== MAGRE-REGISTER NG BAGONG USER SA FIREBASE AUTH & FIRESTORE ============
    private fun registersUser() {
        // Kunin ang mga values galing sa input fields at tanggalin ang extra spaces
        val name: String = findViewById<EditText>(R.id.et_name).text.toString().trim { it <= ' ' }
        val email: String = findViewById<EditText>(R.id.et_email).text.toString().trim { it <= ' ' }
        val password: String = findViewById<EditText>(R.id.et_password).text.toString().trim { it <= ' ' }

        if (validateForm(name, email, password)) {
            // Ipakita ang "Please wait" loading dialog
            showProgressDialog(resources.getString(R.string.please_wait))

            // Gamitin ang Firebase para gumawa ng bagong user account
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    hideProgressDialog() // Alisin ang loading dialog

                    if (task.isSuccessful) {
                        // ✅ Tagumpay: nakuha na ang bagong Firebase user
                        val firebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email!!

                        // Gumawa ng `User` object para i-save sa Firestore
                        val user = User(
                            firebaseUser.uid, // UID ng Firebase user
                            name,              // Pangalan na in-input
                            registeredEmail    // Email address
                        )

                        // I-save ang user object sa Firestore gamit ang FireStoreClass
                        FireStoreClass().registerUser(this, user)

                    } else {
                        // ❌ Registration failed
                        showErrorSnackBar("Registration Failed")
                    }
                }
        }
    }

    // ========== CALLBACK MULA FireStoreClass PAG MATAGUMPAY ANG REGISTRATION ============
    fun userRegisteredSuccess() {
        showCustomToast("You have successfully registered") // Ipakita ang toast
        hideProgressDialog() // Alisin ang loading
    }

} // END NG SignUpActivity CLASS
