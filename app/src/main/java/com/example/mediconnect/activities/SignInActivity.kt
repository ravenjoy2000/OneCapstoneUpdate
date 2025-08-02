package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.utils.Constants
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : BaseActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        setFullscreenMode()
        setupActionbar()

        findViewById<Button>(R.id.btn_sign_in).setOnClickListener {
            signInRegisteredUser()
        }
    }

    private fun setFullscreenMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun setupActionbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

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

    private fun signInRegisteredUser() {
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

        if (validateForm(email, password)) {
            showProgressDialog(getString(R.string.please_wait))

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    hideProgressDialog()
                                    if (document != null && document.exists()) {
                                        val role = document.getString("role")
                                        val name = document.getString("name") ?: "User"
                                        showCustomToast("Welcome, $name!")

                                        val intent = when (role) {
                                            "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
                                            "patient" -> Intent(this, MainActivity::class.java)
                                            else -> {
                                                showErrorSnackBar("User role is undefined.")
                                                return@addOnSuccessListener
                                            }
                                        }

                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        showErrorSnackBar("User record not found in database.")
                                    }
                                }
                                .addOnFailureListener {
                                    hideProgressDialog()
                                    showErrorSnackBar("Error fetching user details: ${it.message}")
                                }
                        }
                    } else {
                        hideProgressDialog()
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException,
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                            else -> "Authentication failed: ${task.exception?.message}"
                        }
                        showErrorSnackBar(errorMessage)
                    }
                }
        }
    }

    fun signInSuccess(user: User) {
        hideProgressDialog()

        // Optional: Save user info in shared preferences or pass via intent
        // Example: redirect to MainActivity
        val intent = Intent(this@SignInActivity, MainActivity::class.java)
        intent.putExtra(Constants.USER_NAME, user.name)
        startActivity(intent)
        finish()
    }

}
