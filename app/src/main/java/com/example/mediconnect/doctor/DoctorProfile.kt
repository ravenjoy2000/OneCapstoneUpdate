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

    // Views declaration
    private lateinit var ivProfile: de.hdodenhof.circleimageview.CircleImageView  // Profile image
    private lateinit var name: TextInputEditText       // Name input
    private lateinit var specialty: TextInputEditText  // Specialty input
    private lateinit var mobile: TextInputEditText     // Mobile number input
    private lateinit var email: TextInputEditText      // Email input (usually readonly)
    private lateinit var clinic: TextInputEditText     // Clinic address input
    private lateinit var bio: TextInputEditText        // Short bio input
    private lateinit var btnSave: Button                // Save button
    private lateinit var progressDialog: ProgressDialog // Progress dialog for loading/saving

    // Firebase instances
    private val db = FirebaseFirestore.getInstance()               // Firestore database
    private val storage = FirebaseStorage.getInstance().reference  // Firebase Storage reference
    private val currentUser = FirebaseAuth.getInstance().currentUser  // Currently logged in user
    private var imageUri: Uri? = null                              // Uri for selected profile image

    private var existingImageUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()                              // Itago ang status bar (fullscreen)
        setContentView(R.layout.activity_doctor_profile)  // Itakda ang layout ng activity

        // Itago status bar (para sa Android 11+ at mas luma)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())  // Android 11+
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()    // I-setup ang toolbar/action bar
        initViews()        // I-initialize ang mga views at listeners
        loadProfile()      // Kuhanin ang profile data mula Firestore
        hideStatusBar()    // Ulitin ang pagtatago ng status bar (just in case)
    }

    // Method para itago ang status bar, fullscreen ang app
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

    // I-setup ang toolbar bilang action bar
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_doctor_profile)  // Kunin toolbar sa layout
        setSupportActionBar(toolbar)                                      // Gawing action bar
        supportActionBar?.apply {
            title = "Doctor Profile"          // Itakda ang title ng toolbar
            setDisplayHomeAsUpEnabled(true)  // Ipakita ang back button sa toolbar
        }
        // Kapag pinindot ang back button, i-trigger ang back press
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // I-initialize ang mga views at button click listeners
    private fun initViews() {
        ivProfile = findViewById(R.id.iv_profile_image)  // Profile image view
        name = findViewById(R.id.et_name)                // Name input field
        specialty = findViewById(R.id.et_specialty)     // Specialty input field
        mobile = findViewById(R.id.et_mobile)            // Mobile input field
        email = findViewById(R.id.et_email)               // Email input field
        clinic = findViewById(R.id.et_clinic_address)    // Clinic address input field
        bio = findViewById(R.id.et_bio)                   // Bio input field
        btnSave = findViewById(R.id.btn_save)             // Save button
        progressDialog = ProgressDialog(this)             // Progress dialog instance

        email.setText(currentUser?.email ?: "")           // Ipakita ang kasalukuyang email sa input

        ivProfile.setOnClickListener { openGallery() }   // Kapag pinindot ang profile image, buksan gallery para pumili ng larawan

        btnSave.setOnClickListener { saveProfile() }      // Kapag pinindot ang save button, i-save ang profile
    }

    // Register activity result para sa gallery image selection
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Kapag successful ang pagpili ng larawan sa gallery
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data        // Kunin ang Uri ng napiling larawan
            ivProfile.setImageURI(imageUri)    // Ipakita ang napiling larawan sa ImageView
        }
    }

    // Buksan ang gallery para pumili ng larawan
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // Kuhanin ang profile data mula Firestore database
    private fun loadProfile() {
        val uid = currentUser?.uid ?: return  // Kunin ang user ID, kung walang user ay bumalik lang

        // Ipakita ang loading dialog
        progressDialog.setMessage("Loading profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Kunin ang dokumento ng user sa "users" collection
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                progressDialog.dismiss()  // Itago ang loading dialog kapag success

                if (doc != null && doc.exists()) {
                    // I-set ang mga field gamit ang mga value mula sa Firestore document
                    name.setText(doc.getString("name"))
                    specialty.setText(doc.getString("specialty"))
                    mobile.setText(doc.getString("phone"))
                    clinic.setText(doc.getString("clinicAddress"))
                    bio.setText(doc.getString("shortBio"))
                    email.setText(doc.getString("email"))

                    // Kunin ang URL ng profile image kung meron
                    val photoUrl = doc.getString("image")
                    existingImageUrl = photoUrl // keep the current image URL

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .apply(RequestOptions.placeholderOf(R.drawable.outline_account_circle_24))
                            .into(ivProfile)
                    }

                }
            }.addOnFailureListener {
                // Kapag may error sa pagkuha ng data, itago ang loading at ipakita error message
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }


    }

    // I-save ang profile data sa Firestore at storage (kung may bagong larawan)
    private fun saveProfile() {
        val uid = currentUser?.uid ?: return  // Kunin ang user ID, kung walang user ay bumalik lang

        // Kunin ang mga value mula sa input fields at i-trim ang spaces
        val nameVal = name.text.toString().trim()
        val specialtyVal = specialty.text.toString().trim()
        val mobileVal = mobile.text.toString().trim()
        val emailVal = email.text.toString().trim()
        val clinicVal = clinic.text.toString().trim()
        val bioVal = bio.text.toString().trim()

        // Check kung required fields ay puno bago magpatuloy
        if (nameVal.isEmpty() || specialtyVal.isEmpty() || mobileVal.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Ihanda ang data para i-save sa Firestore
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

        // Ipakita ang progress dialog habang nagsi-save
        progressDialog.setMessage("Saving profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        if (imageUri != null) {
            // Kung may bagong larawan, i-upload muna ito sa Firebase Storage
            val imageRef = storage.child("users/$uid/profile.jpg")
            imageRef.putFile(imageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    imageRef.downloadUrl  // Kunin ang download URL pagkatapos ng upload
                }.addOnSuccessListener { downloadUrl ->
                    // Idagdag ang image URL sa profile data bago i-save sa Firestore
                    profileData["image"] = downloadUrl.toString()
                    saveToFirestore(uid, profileData)
                }.addOnFailureListener {
                    // Kapag nabigo ang upload, itago ang loading at ipakita error
                    progressDialog.dismiss()
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Keep the existing image if no new image is selected
            existingImageUrl?.let {
                profileData["image"] = it
            }
            saveToFirestore(uid, profileData)
        }

    }

    // I-save ang profile data sa Firestore
    private fun saveToFirestore(uid: String, profileData: HashMap<String, Any>) {
        db.collection("users").document(uid).set(profileData)
            .addOnSuccessListener {
                progressDialog.dismiss()  // Itago ang progress dialog kapag successful
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                restartApp()              // I-restart ang app para ma-refresh ang UI
            }
            .addOnFailureListener {
                progressDialog.dismiss()  // Itago ang dialog kapag may error
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }

    // I-restart ang app sa DoctorDashboardActivity pagkatapos mag-save ng profile
    private fun restartApp() {
        val intent = Intent(this, DoctorDashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear activity stack
        startActivity(intent)
        finish()
    }

}
