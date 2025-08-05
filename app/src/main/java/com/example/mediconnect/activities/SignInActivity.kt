package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.utils.Constants
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignInActivity : BaseActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        setFullscreenMode()
        setupActionBar()
        setupGoogleSignIn()
        setupButtonClicks()
    }

    // -----------------------------
    // UI Setup
    // -----------------------------

    private fun setFullscreenMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupButtonClicks() {
        findViewById<Button>(R.id.btn_sign_in).setOnClickListener { signInRegisteredUser() }

        findViewById<Button>(R.id.btn_google_sign_in).setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    // -----------------------------
    // Google Sign-In
    // -----------------------------

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuthWithCredential(credential, "Google")
    }

    // -----------------------------
    // Firebase Auth Shared Logic
    // -----------------------------

    private fun firebaseAuthWithCredential(credential: AuthCredential, provider: String) {
        showProgressDialog("Signing in with $provider...")

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideProgressDialog()
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                handleRoleAndRedirect(
                                    doc.getString("role"),
                                    doc.getString("name") ?: auth.currentUser?.displayName
                                )
                            } else {
                                val newUser = hashMapOf(
                                    "name" to auth.currentUser?.displayName,
                                    "email" to auth.currentUser?.email,
                                    "role" to "patient"
                                )
                                db.collection("users").document(userId).set(newUser)
                                    .addOnSuccessListener {
                                        showCustomToast("Welcome, ${newUser["name"]}!")
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                            }
                        }
                } else {
                    showErrorSnackBar("Firebase authentication failed: ${task.exception?.message}")
                }
            }
    }

    // -----------------------------
    // Email/Password Sign-In
    // -----------------------------

    private fun signInRegisteredUser() {
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

        if (!validateForm(email, password)) return

        showProgressDialog(getString(R.string.please_wait))

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                hideProgressDialog()
                                if (document.exists()) {
                                    val role = document.getString("role")
                                    val name = document.getString("name") ?: "User"
                                    handleRoleAndRedirect(role, name)
                                } else {
                                    showErrorSnackBar("User record not found in database.")
                                }
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                showErrorSnackBar("Error fetching user: ${it.message}")
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

    // -----------------------------
    // Redirect Logic
    // -----------------------------

    private fun handleRoleAndRedirect(role: String?, name: String?) {
        showCustomToast("Welcome, $name!")

        val intent = when (role) {
            "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
            "patient" -> Intent(this, MainActivity::class.java)
            else -> {
                showErrorSnackBar("User role is undefined.")
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // -----------------------------
    // Utility
    // -----------------------------

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                showErrorSnackBar("Google Sign-In failed: ${e.message}")
            }
        }
    }

    fun signInSuccess(user: User) {
        hideProgressDialog()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(Constants.USER_NAME, user.name)
        startActivity(intent)
        finish()
    }
}
