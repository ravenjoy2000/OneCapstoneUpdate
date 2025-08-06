package com.example.mediconnect.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.models.Doctor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class DoctorIntro : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var btnUpload: Button
    private lateinit var etName: EditText
    private lateinit var etSpecialty: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var etWebsite: EditText
    private lateinit var etBio: EditText
    private lateinit var btnLetsGo: Button

    private var selectedImageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_intro)

        setupToolbar()
        initViews()
        populateEmail()
        setListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.iv_doctor_profile)
        btnUpload = findViewById(R.id.btnUploadDoctorProfile)
        etName = findViewById(R.id.et_doctor_name)
        etSpecialty = findViewById(R.id.et_doctor_specialty)
        etPhone = findViewById(R.id.et_doctor_phone)
        etEmail = findViewById(R.id.et_doctor_email)
        etAddress = findViewById(R.id.et_doctor_address)
        etWebsite = findViewById(R.id.et_doctor_website)
        etBio = findViewById(R.id.et_doctor_bio)
        btnLetsGo = findViewById(R.id.btn_lets_go)
    }

    private fun populateEmail() {
        val currentUser = auth.currentUser
        etEmail.setText(currentUser?.email ?: "")
    }

    private fun setListeners() {
        btnUpload.setOnClickListener {
            openImageChooser()
        }

        btnLetsGo.setOnClickListener {
            if (validateInputs()) {
                if (selectedImageUri != null) {
                    uploadProfileImage()
                } else {
                    saveDoctorProfile("") // Save without image
                }
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data!!.data
            Glide.with(this).load(selectedImageUri).into(ivProfile)
        }
    }

    private fun uploadProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseStorage.getInstance().reference
            .child("doctor_profiles/$uid-${UUID.randomUUID()}.jpg")

        selectedImageUri?.let { uri ->
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        saveDoctorProfile(imageUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveDoctorProfile(profileUrl: String) {
        val uid = auth.currentUser?.uid ?: return

        val doctor = Doctor(
            profileImage = profileUrl,
            name = etName.text.toString(),
            specialty = etSpecialty.text.toString(),
            phoneNumber = etPhone.text.toString(),
            email = etEmail.text.toString(),
            address = etAddress.text.toString(),
            website = etWebsite.text.toString(),
            bio = etBio.text.toString()
        )

        db.collection("users").document(uid)
            .set(doctor)
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor profile set up successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DoctorDashboardActivity::class.java)
                intent.putExtra("doctor_profile", doctor)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        return when {
            etName.text.isNullOrEmpty() -> {
                etName.error = "Name is required"
                false
            }
            etSpecialty.text.isNullOrEmpty() -> {
                etSpecialty.error = "Specialty is required"
                false
            }
            etPhone.text.isNullOrEmpty() -> {
                etPhone.error = "Phone number is required"
                false
            }
            else -> true
        }
    }
}
