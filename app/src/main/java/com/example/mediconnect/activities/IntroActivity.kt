package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.models.AppConstant
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText

class IntroActivity : BaseActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setContentView(R.layout.activity_intro)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // If already logged in → skip
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.uid, currentUser.displayName ?: "User")
            return
        }

        // Show Google login button
        findViewById<Button>(R.id.btn_google_sign_in).setOnClickListener {
            signInWithGoogle()
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                firebaseAuthWithGoogle(task.result)
            } else {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) return
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkUserRoleAndRedirect(user.uid, user.displayName ?: "User")
                    }
                } else {
                    Toast.makeText(this, "Firebase Auth Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserRoleAndRedirect(uid: String, userName: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Existing user → setup Zego + redirect
                    initZego(uid, userName)

                    val role = document.getString("role")
                    val profileComplete = document.getBoolean("profileComplete") ?: false

                    when (role) {
                        "doctor" -> {
                            startActivity(Intent(this, DoctorDashboardActivity::class.java))
                            finish()
                        }
                        "patient" -> {
                            if (profileComplete) {
                                startActivity(Intent(this, MainActivity::class.java))
                            } else {
                                startActivity(Intent(this, MainActivity::class.java))
                            }
                            finish()
                        }
                        else -> {
                            Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show()
                            FirebaseAuth.getInstance().signOut()
                        }
                    }
                } else {
                    // New user → save default role (patient) and force profile setup
                    val newUser = hashMapOf(
                        "uid" to uid,
                        "name" to userName,
                        "email" to auth.currentUser?.email,
                        "photoUrl" to auth.currentUser?.photoUrl?.toString(),
                        "role" to "patient", // default role
                        "profileComplete" to false, // NEW FLAG
                        "createdAt" to System.currentTimeMillis(),
                        "appointments" to emptyList<String>(),
                        "history" to emptyList<String>()
                    )
                    db.collection("users").document(uid).set(newUser)
                        .addOnSuccessListener {
                            initZego(uid, userName)
                            // Redirect to Profile Setup first
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking user role", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initZego(uid: String, userName: String) {
        val config = ZegoUIKitPrebuiltCallInvitationConfig().apply {
            translationText = ZegoTranslationText()
        }
        ZegoUIKitPrebuiltCallService.init(
            application,
            AppConstant.appId,
            AppConstant.appSign,
            uid,
            userName,
            config
        )
    }

    private fun redirectBasedOnRole(role: String?) {
        when (role) {
            "doctor" -> {
                startActivity(Intent(this, DoctorDashboardActivity::class.java))
                finish()
            }
            "patient" -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            else -> {
                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show()
                FirebaseAuth.getInstance().signOut()
            }
        }
    }
}
