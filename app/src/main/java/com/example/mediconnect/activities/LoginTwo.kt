package com.example.mediconnect.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.patient.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class LoginTwo : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Phone Auth
    private lateinit var phoneInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    // Google Sign-In
    private lateinit var btnGoogleSignIn: Button
    private lateinit var googleSignInClient: GoogleSignInClient

    // Role selection
    private lateinit var radioGroupRole: RadioGroup
    private lateinit var radioPatient: RadioButton
    private lateinit var radioDoctor: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_two)

        auth = FirebaseAuth.getInstance()

        setupActionBar()
        setFullscreenMode()
        initViews()
        setupGoogleSignIn()
        setListeners()
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

    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun initViews() {
        phoneInput = findViewById(R.id.et_phone)
        otpInput = findViewById(R.id.et_otp)
        btnSendOtp = findViewById(R.id.btn_send_otp)
        btnVerifyOtp = findViewById(R.id.btn_verify_otp)
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in)
        radioGroupRole = findViewById(R.id.radio_group_role)
        radioPatient = findViewById(R.id.radio_patient)
        radioDoctor = findViewById(R.id.radio_doctor)

        otpInput.visibility = View.GONE
        btnVerifyOtp.visibility = View.GONE
    }

    private fun setListeners() {
        btnSendOtp.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty()) {
                startPhoneNumberVerification(phone)
            } else {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerifyOtp.setOnClickListener {
            val code = otpInput.text.toString().trim()
            if (!storedVerificationId.isNullOrEmpty() && code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
                signInWithPhoneAuthCredential(credential)
            }
        }

        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneAuthCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@LoginTwo, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("PhoneAuth", "Error: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            storedVerificationId = verificationId
            resendToken = token
            otpInput.visibility = View.VISIBLE
            btnVerifyOtp.visibility = View.VISIBLE
            Toast.makeText(this@LoginTwo, "OTP Sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goToDashboard()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { signInTask ->
                        if (signInTask.isSuccessful) {
                            goToDashboard()
                        } else {
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.e("GOOGLE_SIGN_IN", "Error: ${e.message}")
            }
        }
    }

    private fun goToDashboard() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                val selectedRole = when (radioGroupRole.checkedRadioButtonId) {
                    R.id.radio_patient -> "patient"
                    R.id.radio_doctor -> "doctor"
                    else -> "patient"
                }

                if (document.exists()) {
                    val role = document.getString("role")

                    if (selectedRole == "doctor" && role != "doctor") {
                        // Not allowed to log in as doctor
                        Toast.makeText(this, "You are not authorized as a doctor.", Toast.LENGTH_LONG).show()
                        FirebaseAuth.getInstance().signOut()
                        return@addOnSuccessListener
                    }

                    val intent = if (role == "doctor") {
                        Intent(this, DoctorDashboardActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    startActivity(intent)
                    finish()

                } else {
                    // New user trying to register
                    if (selectedRole == "doctor") {
                        // Prevent doctors from signing up themselves
                        Toast.makeText(this, "Only admin can register a doctor.", Toast.LENGTH_LONG).show()
                        FirebaseAuth.getInstance().signOut()
                        return@addOnSuccessListener
                    }

                    val email = auth.currentUser?.email ?: ""
                    val phone = auth.currentUser?.phoneNumber ?: ""

                    val newUser = hashMapOf(
                        "email" to email,
                        "phone" to phone,
                        "role" to "patient"
                    )

                    userDocRef.set(newUser)
                        .addOnSuccessListener {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("LoginTwo", "Error creating user", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking profile: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginTwo", "Firestore error", e)
            }
    }
}
