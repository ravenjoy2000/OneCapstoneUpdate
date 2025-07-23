// Package where the activity is stored
package com.example.mediconnect.activities

// Android UI and Firebase libraries
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * Activity that handles user Sign In functionality.
 */
class SignInActivity : BaseActivity() {

    // Called when activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables full-screen layout support
        setContentView(R.layout.activity_sign_in)

        // Fullscreen Mode Setup (status bar hidden)
        setFullscreenMode()

        // Setup toolbar / action bar
        setupActionbar()

        // Handle Sign In button click
        findViewById<Button>(R.id.btn_sign_in).setOnClickListener {
            signInRegisteredUser()
        }
    }

    /**
     * Enables fullscreen mode by hiding status bar.
     */
    private fun setFullscreenMode() {
        // For older Android versions
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // For Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    /**
     * Set up the toolbar with back navigation.
     */
    private fun setupActionbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // Show back arrow
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24) // Custom back icon
        }

        // Back navigation behavior
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    /**
     * Validate email and password fields.
     * @return true if valid, false otherwise.
     */
    private fun validateForm(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.")
                false
            }

            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.")
                false
            }

            else -> true
        }
    }

    /**
     * Perform Firebase sign-in for registered users.
     */
    private fun signInRegisteredUser() {
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

        if (validateForm(email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    hideProgressDialog()

                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        showCustomToast("You have successfully signed in as ${user?.email}")

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException,
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                            else -> "Authentication failed. Please try again."
                        }

                        showErrorSnackBar(errorMessage)
                    }
                }
        }
    }

    /**
     * Called after user info is successfully fetched from Firestore.
     * (Can be triggered from FireStoreClass if needed)
     */
    fun signInSuccess(user: User) {
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
