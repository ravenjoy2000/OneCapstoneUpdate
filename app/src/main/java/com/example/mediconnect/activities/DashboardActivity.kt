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
import com.google.firebase.firestore.SetOptions
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

    // Store old values for when images are not changed
    private var oldProfileUrl: String = ""
    private var oldGovIdUrl: String = ""

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

        btnUploadProfile.setOnClickListener { pickImage(101) }
        btnUploadGovId.setOnClickListener { pickImage(102) }
        btnSave.setOnClickListener { saveProfile() }

        loadUserProfile()
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
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
                            findViewById<EditText>(R.id.et_phone).setText(it.phone)

                            if (it.image.isNotEmpty()) {
                                oldProfileUrl = it.image
                                Glide.with(this).load(it.image).into(ivProfile)
                            }
                            if (it.goverment_or_phealtID.isNotEmpty()) {
                                oldGovIdUrl = it.goverment_or_phealtID
                                Glide.with(this).load(it.goverment_or_phealtID).into(ivGovId)
                            }
                        }
                    }
                }
        }
    }

    private fun saveProfile() {
        val name = findViewById<EditText>(R.id.et_name).text.toString().trim()
        val phone = findViewById<EditText>(R.id.et_phone).text.toString().trim()
        val email = currentUser?.email ?: ""

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        currentUserId?.let { uid ->
            uploadImages(uid) { profileUrl, govIdUrl ->
                val userMap = mapOf(
                    "id" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "image" to (profileUrl.ifEmpty { oldProfileUrl }),
                    "goverment_or_phealtID" to (govIdUrl.ifEmpty { oldGovIdUrl })
                )

                db.collection("users").document(uid)
                    .set(userMap, SetOptions.merge()) // ✅ merge instead of update
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

        // Upload profile image if selected
        if (selectedProfileUri != null) {
            profileRef.putFile(selectedProfileUri!!).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                profileRef.downloadUrl
            }.addOnSuccessListener { uri ->
                profileUrl = uri.toString()
                if (selectedGovIdUri == null) {
                    onComplete(profileUrl, "")
                }
            }
        }

        // Upload gov ID if selected
        if (selectedGovIdUri != null) {
            govIdRef.putFile(selectedGovIdUri!!).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                govIdRef.downloadUrl
            }.addOnSuccessListener { uri ->
                govIdUrl = uri.toString()
                if (selectedProfileUri == null) {
                    onComplete("", govIdUrl)
                } else {
                    onComplete(profileUrl, govIdUrl)
                }
            }
        }

        // If no new uploads, just complete with empty values
        if (selectedProfileUri == null && selectedGovIdUri == null) {
            onComplete("", "")
        }
    }
}
