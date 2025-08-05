package com.example.mediconnect.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DashboardActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var ivGovId: ImageView
    private lateinit var tvEmail: TextView
    private var selectedProfileUri: Uri? = null
    private var selectedGovIdUri: Uri? = null

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentUserId = currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        ivProfile = findViewById(R.id.iv_profile_user_image)
        ivGovId = findViewById(R.id.img_philhealth_id_preview)
        tvEmail = findViewById(R.id.et_email)

        val btnUploadProfile = findViewById<Button>(R.id.btnUploadProfileImage)
        val btnUploadGovId = findViewById<Button>(R.id.btn_upload_philhealth_id)
        val btnSave = findViewById<Button>(R.id.btn_lets_go)

        // Display email from FirebaseAuth
        tvEmail.text = currentUser?.email ?: "No user logged in"

        btnUploadProfile.setOnClickListener {
            pickImage(101)
        }

        btnUploadGovId.setOnClickListener {
            pickImage(102)
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        loadUserProfile()
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                101 -> {
                    selectedProfileUri = data.data
                    ivProfile.setImageURI(selectedProfileUri)
                }
                102 -> {
                    selectedGovIdUri = data.data
                    ivGovId.setImageURI(selectedGovIdUri)
                }
            }
        }
    }

    private fun loadUserProfile() {
        currentUserId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            findViewById<EditText>(R.id.et_name).setText(it.name)
                            findViewById<EditText>(R.id.et_username).setText(it.username)
                            findViewById<EditText>(R.id.et_phone).setText(it.phone)

                            if (it.image.isNotEmpty()) {
                                Glide.with(this).load(it.image).into(ivProfile)
                            }
                            if (it.goverment_or_phealtID.isNotEmpty()) {
                                Glide.with(this).load(it.goverment_or_phealtID).into(ivGovId)
                            }
                        }
                    }
                }
        }
    }

    private fun saveProfile() {
        val name = findViewById<EditText>(R.id.et_name).text.toString().trim()
        val username = findViewById<EditText>(R.id.et_username).text.toString().trim()
        val phone = findViewById<EditText>(R.id.et_phone).text.toString().trim()
        val email = currentUser?.email ?: ""

        if (name.isEmpty() || username.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        currentUserId?.let { uid ->
            uploadImages(uid) { profileUrl, govIdUrl ->
                val userMap = mapOf(
                    "id" to uid,
                    "name" to name,
                    "username" to username,
                    "phone" to phone,
                    "email" to email,
                    "image" to profileUrl,
                    "goverment_or_phealtID" to govIdUrl
                )

                db.collection("users").document(uid).update(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun uploadImages(uid: String, onComplete: (String, String) -> Unit) {
        val profileRef = storage.reference.child("users/$uid/profile.jpg")
        val govIdRef = storage.reference.child("users/$uid/gov_id.jpg")

        var profileUrl = ""
        var govIdUrl = ""

        val uploadProfile = selectedProfileUri?.let {
            profileRef.putFile(it).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                profileRef.downloadUrl
            }
        } ?: return onComplete("", "")

        val uploadGovId = selectedGovIdUri?.let {
            govIdRef.putFile(it).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                govIdRef.downloadUrl
            }
        } ?: return onComplete("", "")

        uploadProfile.addOnSuccessListener { uri1 ->
            profileUrl = uri1.toString()
            uploadGovId.addOnSuccessListener { uri2 ->
                govIdUrl = uri2.toString()
                onComplete(profileUrl, govIdUrl)
            }
        }
    }
}
