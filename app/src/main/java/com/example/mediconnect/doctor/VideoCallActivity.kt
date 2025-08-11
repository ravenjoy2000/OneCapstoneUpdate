package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.example.mediconnect.R
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser


class VideoCallActivity : AppCompatActivity() {

    lateinit var voiceCallBtn: ZegoSendCallInvitationButton
    lateinit var videoCallBtn: ZegoSendCallInvitationButton

    lateinit var currentUsernameTextView: TextView

    lateinit var targetuserInput: EditText

    lateinit var btnAddMedicalLog: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_call)

        voiceCallBtn = findViewById(R.id.voice_call_btn)
        videoCallBtn = findViewById(R.id.video_call_btn)
        currentUsernameTextView = findViewById(R.id.current_user_name_textview)
        targetuserInput = findViewById(R.id.target_usernae_input)
        btnAddMedicalLog = findViewById(R.id.btn_add_medical_log)

        // Receive extras
        val targetUserId = intent.getStringExtra("targetUserId") ?: ""
        val targetUserName = intent.getStringExtra("targetUserName") ?: ""
        val currentUserId = intent.getStringExtra("currentUserId") ?: ""
        val currentUserName = intent.getStringExtra("currentUserName") ?: ""
        val appointmentId = intent.getStringExtra("appointmentId") ?: ""

        currentUsernameTextView.text = currentUserName

        if (targetUserId.isNotEmpty() && targetUserName.isNotEmpty()) {
            targetuserInput.setText(targetUserName)
            setupVoiceCall(targetUserId, targetUserName)
            setupVideoCall(targetUserId, targetUserName)
        }

        targetuserInput.isEnabled = false

        // Simulate call end (replace with real event from Zego)
        videoCallBtn.setOnClickListener {
            setupVideoCall(targetUserId, targetUserName)

            // After call ends -> show button
            btnAddMedicalLog.visibility = View.VISIBLE
        }

        btnAddMedicalLog.setOnClickListener {
            val intent = Intent(this, AddMedicalLogActivity::class.java)
            intent.putExtra("appointmentId", appointmentId)
            intent.putExtra("patientId", targetUserId)
            intent.putExtra("doctorId", currentUserId)
            startActivity(intent)
        }
    }

    fun setupVoiceCall(userId: String, userName: String) {
        voiceCallBtn.setIsVideoCall(false)
        voiceCallBtn.resourceID = "zego_uikit_call"
        voiceCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))
    }

    fun setupVideoCall(userId: String, userName: String) {
        videoCallBtn.setIsVideoCall(true)
        videoCallBtn.resourceID = "zego_uikit_call"
        videoCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))
    }




}