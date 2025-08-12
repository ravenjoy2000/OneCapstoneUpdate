package com.example.mediconnect.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.models.Appointment
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser

class VideoCallActivity : AppCompatActivity() {

    // UI elements
    private lateinit var voiceCallBtn: ZegoSendCallInvitationButton
    private lateinit var videoCallBtn: ZegoSendCallInvitationButton
    private lateinit var currentUsernameTextView: TextView
    private lateinit var targetUserInput: EditText
    private lateinit var btnAreDoneWithCall: Button

    // Intent data variables
    private var targetUserId = ""
    private var targetUserName = ""
    private var currentUserId = ""
    private var currentUserName = ""
    private var appointmentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_call)

        // Bind views
        voiceCallBtn = findViewById(R.id.voice_call_btn)
        videoCallBtn = findViewById(R.id.video_call_btn)
        currentUsernameTextView = findViewById(R.id.current_user_name_textview)
        targetUserInput = findViewById(R.id.target_usernae_input)
        btnAreDoneWithCall = findViewById(R.id.btn_are_done_with_call)

        // Retrieve Intent extras
        targetUserId = intent.getStringExtra("targetUserId") ?: ""
        targetUserName = intent.getStringExtra("targetUserName") ?: ""
        currentUserId = intent.getStringExtra("currentUserId") ?: ""
        currentUserName = intent.getStringExtra("currentUserName") ?: ""
        appointmentId = intent.getStringExtra("appointmentId") ?: ""

        // Set current username display
        currentUsernameTextView.text = currentUserName

        // Disable editing on target user input
        targetUserInput.isEnabled = false

        if (targetUserId.isNotEmpty() && targetUserName.isNotEmpty()) {
            targetUserInput.setText(targetUserName)
            setupVoiceCall(targetUserId, targetUserName)
            setupVideoCall(targetUserId, targetUserName)
        } else {
            Log.w("VideoCallActivity", "Target user info missing.")
        }

        // Show the "Are you done with the call?" button when video call button is clicked
        videoCallBtn.setOnClickListener {
            setupVideoCall(targetUserId, targetUserName)
            btnAreDoneWithCall.visibility = android.view.View.VISIBLE
        }

        // When "Are you done with the call?" is clicked, go back to AppointmentDetailsActivity
        btnAreDoneWithCall.setOnClickListener {
            finish() // Go back to previous activity in the stack
        }

    }

    private fun setupVoiceCall(userId: String, userName: String) {
        voiceCallBtn.setIsVideoCall(false)
        voiceCallBtn.resourceID = "zego_uikit_call"
        voiceCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))
    }

    private fun setupVideoCall(userId: String, userName: String) {
        videoCallBtn.setIsVideoCall(true)
        videoCallBtn.resourceID = "zego_uikit_call"
        videoCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))
    }
}
