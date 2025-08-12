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
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.example.mediconnect.models.AppConstant
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText

class SignInActivity : BaseActivity() {

    // Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance()
    // Firestore database instance
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

        // Para maging fullscreen ang activity (walang status bar)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        bindViews() // I-bind ang mga view sa variables
        setFullscreenMode() // Itago ang status bar
        setupActionBar() // I-set ang toolbar na may back button
        setupGoogleSignIn() // I-setup ang Google Sign In client
        setupButtonClicks() // I-assign ang mga click listeners sa buttons
    }

    /** I-bind ang mga views sa layout */
    private fun bindViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnSignIn = findViewById(R.id.btn_sign_in)
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
    }

    /** Itago ang status bar para full screen */
    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    /** Setup toolbar with back arrow */
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    /** Setup click listeners for buttons */
    private fun setupButtonClicks() {
        btnSignIn.setOnClickListener { signInRegisteredUser() }
        btnGoogleSignIn.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    /** Ipakita ang dialog para sa "Forgot Password" at magpadala ng reset email */
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val input = EditText(this).apply {
            hint = "Enter your email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
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

    /** Setup Google Sign-In client */
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID sa strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    /** Authenticate Firebase gamit ang Google ID token */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuthWithCredential(credential, "Google")
    }

    /** Common function para sa Firebase Auth gamit anumang credential */
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
                                // Kung may existing user record, i-redirect base sa role
                                handleRoleAndRedirect(
                                    doc.getString("role"),
                                    doc.getString("name") ?: auth.currentUser?.displayName
                                )
                            } else {
                                // Kung bagong user, save default role "patient" at pangalan
                                val newUser = hashMapOf(
                                    "name" to auth.currentUser?.displayName,
                                    "email" to auth.currentUser?.email,
                                    "role" to "patient"
                                )
                                db.collection("users").document(userId).set(newUser)
                                    .addOnSuccessListener {
                                        showCustomToast("Welcome, ${newUser["name"]}!")
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

    /** Sign in gamit ang email at password */
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
                        // Kunin ang role ng user mula sa Firestore
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

    /** Simple validation ng email at password */
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
     * I-redirect ang user depende sa role (doctor/patient)
     * At i-initialize ang Zego call service para sa realtime calls
     */
    private fun handleRoleAndRedirect(role: String?, name: String?) {
        val userId = auth.currentUser?.uid ?: ""
        val userName = name ?: "User"

        val config = ZegoUIKitPrebuiltCallInvitationConfig().apply {
            translationText = ZegoTranslationText() // Para maiwasan error sa translation text
        }

        // Initialize Zego call SDK
        ZegoUIKitPrebuiltCallService.init(application, AppConstant.appId, AppConstant.appSign, userId, userName, config)

        showCustomToast("Welcome, $userName!")

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

    /** Logout function - para i-uninitialize ang Zego at Firebase logout */
    fun logout() {
        ZegoUIKitPrebuiltCallService.unInit() // Itigil ang pakikinig sa tawag
        auth.signOut()

        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Result mula sa Google Sign-In Intent */
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

    /** Optional: Call kapag successful ang sign-in gamit User model */
    fun signInSuccess(user: User) {
        hideProgressDialog()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(Constants.USER_NAME, user.name)
        startActivity(intent)
        finish()
    }
}
