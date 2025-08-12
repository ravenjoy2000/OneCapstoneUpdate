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

    // Declare UI elements
    lateinit var voiceCallBtn: ZegoSendCallInvitationButton      // Button para sa voice call invite
    lateinit var videoCallBtn: ZegoSendCallInvitationButton      // Button para sa video call invite
    lateinit var currentUsernameTextView: TextView               // TextView para sa kasalukuyang username
    lateinit var targetuserInput: EditText                        // EditText para sa target username (read-only)
    lateinit var btnAddMedicalLog: Button                         // Button para magdagdag ng medical log pagkatapos ng tawag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                           // Enable edge-to-edge display para full screen content
        setContentView(R.layout.activity_video_call) // Itakda ang layout ng activity

        // Kunin ang mga views mula sa layout gamit ang kanilang IDs
        voiceCallBtn = findViewById(R.id.voice_call_btn)
        videoCallBtn = findViewById(R.id.video_call_btn)
        currentUsernameTextView = findViewById(R.id.current_user_name_textview)
        targetuserInput = findViewById(R.id.target_usernae_input)
        btnAddMedicalLog = findViewById(R.id.btn_add_medical_log)

        // Kunin ang mga data na ipinasa sa activity via Intent extras
        val targetUserId = intent.getStringExtra("targetUserId") ?: ""      // Target na user ID
        val targetUserName = intent.getStringExtra("targetUserName") ?: ""  // Target na username
        val currentUserId = intent.getStringExtra("currentUserId") ?: ""    // Kasalukuyang user ID
        val currentUserName = intent.getStringExtra("currentUserName") ?: ""// Kasalukuyang username
        val appointmentId = intent.getStringExtra("appointmentId") ?: ""    // Appointment ID para sa medical log

        // Ipakita ang kasalukuyang username sa TextView
        currentUsernameTextView.text = currentUserName

        // Kapag may valid na target user ID at pangalan, i-set ang target input at i-setup ang tawag
        if (targetUserId.isNotEmpty() && targetUserName.isNotEmpty()) {
            targetuserInput.setText(targetUserName)              // Ipakita ang pangalan ng target user sa EditText
            setupVoiceCall(targetUserId, targetUserName)          // I-setup ang voice call invite button
            setupVideoCall(targetUserId, targetUserName)          // I-setup ang video call invite button
        }

        targetuserInput.isEnabled = false  // Huwag payagan na i-edit ang target user input field

        // Simulate na matapos ang tawag kapag pinindot ang video call button (para testing lang)
        videoCallBtn.setOnClickListener {
            setupVideoCall(targetUserId, targetUserName)  // I-setup muli ang video call (para testing)

            // Ipakita ang button para magdagdag ng medical log pagkatapos ng tawag
            btnAddMedicalLog.visibility = View.VISIBLE
        }

        // Kapag pinindot ang Add Medical Log button, pumunta sa AddMedicalLogActivity na may mga kinakailangang data
        btnAddMedicalLog.setOnClickListener {
            val intent = Intent(this, AddMedicalLogActivity::class.java)
            intent.putExtra("appointmentId", appointmentId)  // Ipadala ang appointment ID
            intent.putExtra("patientId", targetUserId)       // Ipadala ang patient ID (target user)
            intent.putExtra("doctorId", currentUserId)       // Ipadala ang doctor ID (current user)
            startActivity(intent)                             // Simulan ang activity
        }
    }

    // I-setup ang voice call invite button
    fun setupVoiceCall(userId: String, userName: String) {
        voiceCallBtn.setIsVideoCall(false)                             // Itakda bilang voice call lang (walang video)
        voiceCallBtn.resourceID = "zego_uikit_call"                    // Tukuyin ang resource ID para sa Zego UI kit
        voiceCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))  // I-set ang mga invitees para tawagan (target user)
    }

    // I-setup ang video call invite button
    fun setupVideoCall(userId: String, userName: String) {
        videoCallBtn.setIsVideoCall(true)                              // Itakda bilang video call (may video)
        videoCallBtn.resourceID = "zego_uikit_call"                    // Tukuyin ang resource ID para sa Zego UI kit
        videoCallBtn.setInvitees(listOf(ZegoUIKitUser(userId, userName)))  // I-set ang mga invitees para tawagan (target user)
    }

}
