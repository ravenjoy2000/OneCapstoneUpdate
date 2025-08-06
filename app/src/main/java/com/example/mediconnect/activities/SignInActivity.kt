package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.mediconnect.R
import com.example.mediconnect.databinding.ActivitySignInBinding
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.utils.Constants
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : BaseActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        bindViews()
        setFullscreenMode()
        setupActionBar()
        setupGoogleSignIn()
        setupButtonClicks()
    }

    private fun bindViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnSignIn = findViewById(R.id.btn_sign_in)
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
    }

    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupButtonClicks() {
        btnSignIn.setOnClickListener { signInRegisteredUser() }
        btnGoogleSignIn.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val input = EditText(this)
        input.hint = "Enter your email"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(input)

        builder.setPositiveButton("Send") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset link: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

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
                                val User = hashMapOf(
                                    "name" to auth.currentUser?.displayName,
                                    "email" to auth.currentUser?.email,
                                    "role" to "patient"
                                )
                                db.collection("users").document(userId).set(User)
                                    .addOnSuccessListener {
                                        showCustomToast("Welcome, ${User["name"]}!")
                                        startActivity(Intent(this, DashboardActivity::class.java))
                                        finish()

                                    }
                            }
                        }
                } else {
                    showErrorSnackBar("Firebase authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun signInRegisteredUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateForm(email, password)) {
            Toast.makeText(this, "Email and password must not be empty", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog(getString(R.string.please_wait))

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // Get role from Firestore
                        db.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                hideProgressDialog()
                                if (document.exists()) {
                                    val role = document.getString("role")
                                    val name = document.getString("name") ?: "User"
                                    handleRoleAndRedirect(role, name)
                                } else {
                                    auth.signOut()
                                    showErrorSnackBar("User data not found in Firestore.")
                                }
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                auth.signOut()
                                showErrorSnackBar("Failed to fetch user data: ${it.localizedMessage}")
                            }
                    } else {
                        auth.signOut()
                        hideProgressDialog()
                        showErrorSnackBar("Please verify your email before logging in.")
                    }
                } else {
                    hideProgressDialog()
                    val errorMessage = when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        else -> "Login failed: ${exception?.localizedMessage}"
                    }
                    showErrorSnackBar(errorMessage)
                }
            }
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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account?.idToken != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    showErrorSnackBar("Google Sign-In failed: Missing ID token.")
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
