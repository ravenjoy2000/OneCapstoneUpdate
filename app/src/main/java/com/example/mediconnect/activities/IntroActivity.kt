package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.patient.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.example.mediconnect.models.AppConstant
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoTranslationText

class IntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen support
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

        // If user is logged in, determine role and redirect
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userName = currentUser.displayName ?: "User"

            FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Setup Zego Call Invitation Config
                        val config = ZegoUIKitPrebuiltCallInvitationConfig().apply {
                            translationText = ZegoTranslationText() // avoid NullPointerException
                        }

                        // Initialize Zego Call Service
                        ZegoUIKitPrebuiltCallService.init(application, AppConstant.appId, AppConstant.appSign, uid, userName, config)

                        when (document.getString("role")) {
                            "doctor" -> {
                                startActivity(Intent(this, DoctorDashboardActivity::class.java))
                                finish()
                            }
                            "patient" -> {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            else -> {
                                // Unknown role, force logout
                                FirebaseAuth.getInstance().signOut()
                            }
                        }
                    } else {
                        // No user doc found, force logout
                        FirebaseAuth.getInstance().signOut()
                    }
                }
                .addOnFailureListener {
                    FirebaseAuth.getInstance().signOut()
                }

            return // prevent buttons from showing while waiting
        }

        // Not logged in → Show sign-in/signup buttons
        findViewById<Button>(R.id.btn_sign_in_intro).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        findViewById<Button>(R.id.btn_sign_up_intro).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
