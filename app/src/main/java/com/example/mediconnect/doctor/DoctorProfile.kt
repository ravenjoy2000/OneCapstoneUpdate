package com.example.mediconnect.doctor

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.IntroActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DoctorProfile : BaseActivity() {

    private lateinit var ivProfile: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var name: TextInputEditText
    private lateinit var specialty: TextInputEditText
    private lateinit var mobile: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var clinic: TextInputEditText
    private lateinit var bio: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var progressDialog: ProgressDialog

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
        setContentView(R.layout.activity_doctor_profile)

        setupActionBar()
        initViews()
        loadProfile()
        hideStatusBar()
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_doctor_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Doctor Profile"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.iv_profile_image)
        name = findViewById(R.id.et_name)
        specialty = findViewById(R.id.et_specialty)
        mobile = findViewById(R.id.et_mobile)
        email = findViewById(R.id.et_email)
        clinic = findViewById(R.id.et_clinic_address)
        bio = findViewById(R.id.et_bio)
        btnSave = findViewById(R.id.btn_save)
        progressDialog = ProgressDialog(this)

        email.setText(currentUser?.email ?: "")
        ivProfile.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveProfile() }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            ivProfile.setImageURI(imageUri)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun loadProfile() {
        val uid = currentUser?.uid ?: return
        progressDialog.setMessage("Loading profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                progressDialog.dismiss()
                if (doc != null && doc.exists()) {
                    name.setText(doc.getString("name"))
                    specialty.setText(doc.getString("specialty"))
                    mobile.setText(doc.getString("phone"))
                    clinic.setText(doc.getString("clinicAddress"))
                    bio.setText(doc.getString("shortBio"))
                    email.setText(doc.getString("email"))

                    val photoUrl = doc.getString("image")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .apply(RequestOptions.placeholderOf(R.drawable.outline_account_circle_24))
                            .into(ivProfile)
                    }
                }
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val uid = currentUser?.uid ?: return

        val nameVal = name.text.toString().trim()
        val specialtyVal = specialty.text.toString().trim()
        val mobileVal = mobile.text.toString().trim()
        val emailVal = email.text.toString().trim()
        val clinicVal = clinic.text.toString().trim()
        val bioVal = bio.text.toString().trim()

        if (nameVal.isEmpty() || specialtyVal.isEmpty() || mobileVal.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val profileData = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to nameVal,
            "specialty" to specialtyVal,
            "phone" to mobileVal,
            "email" to emailVal,
            "clinicAddress" to clinicVal,
            "shortBio" to bioVal,
            "createdAt" to System.currentTimeMillis(),
            "role" to "doctor"
        )

        progressDialog.setMessage("Saving profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        if (imageUri != null) {
            val imageRef = storage.child("users/$uid/profile.jpg")
            imageRef.putFile(imageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    imageRef.downloadUrl
                }.addOnSuccessListener { downloadUrl ->
                    profileData["image"] = downloadUrl.toString()
                    saveToFirestore(uid, profileData)
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveToFirestore(uid, profileData)
        }
    }

    private fun saveToFirestore(uid: String, profileData: HashMap<String, Any>) {
        db.collection("users").document(uid).set(profileData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                restartApp()

            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun restartApp() {
        val intent = Intent(this, DoctorDashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

}
