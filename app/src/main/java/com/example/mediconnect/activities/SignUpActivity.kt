package com.example.mediconnect.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class SignUpActivity : BaseActivity() {

    // ========== Constants & Variables ==========
    private var selectedPhilhealthIdUri: Uri? = null
    private var mPhilIdGovermentImageURL: String = ""

    // ========== Views ==========
    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var imgPhilhealthPreview: ImageView
    private lateinit var btnUpload: Button
    private lateinit var btnSignUp: Button

    // ========== Image Picker ==========
    private val pickImageLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPhilhealthIdUri = uri
                imgPhilhealthPreview.setImageURI(uri)
            }
        }

    // ========== Lifecycle ==========
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        makeFullScreen()
        setupActionBar()
        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        etName = findViewById(R.id.et_name)
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etPhone = findViewById(R.id.et_phone)
        imgPhilhealthPreview = findViewById(R.id.img_philhealth_id_preview)
        btnUpload = findViewById(R.id.btn_upload_philhealth_id)
        btnSignUp = findViewById(R.id.btn_sign_up)
    }

    // ========== Fullscreen ==========
    private fun makeFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    // ========== Toolbar ==========
    private fun setupActionBar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    // ========== Event Listeners ==========
    private fun setupListeners() {
        btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    // ========== Register User ==========
    private fun registerUser() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (validateForm(name, username, email, password, phone)) {
            showProgressDialog("Please wait...")

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null && selectedPhilhealthIdUri != null) {
                        // Send email verification
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                showCustomToast("Verification email sent to ${user.email}. Please verify before logging in.")
                                uploadPhilhealthIdImage(user.uid, name, username, email, phone)
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                showErrorSnackBar("Failed to send verification email: ${it.message}")
                            }
                    } else {
                        hideProgressDialog()
                        showErrorSnackBar("Something went wrong. Please try again.")
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    showErrorSnackBar("Registration failed: ${it.message}")
                }
        }
    }


    // ========== Upload PhilHealth ID ==========
    private fun uploadPhilhealthIdImage(userId: String, name: String, username: String, email: String, phone: String) {
        val extension = getFileExtension(selectedPhilhealthIdUri)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("PhilhealthID_${System.currentTimeMillis()}.$extension")

        selectedPhilhealthIdUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        mPhilIdGovermentImageURL = downloadUri.toString()
                        val user = User(userId, name, username, email, mPhilIdGovermentImageURL, "patient", phone)
                        FireStoreClass().registerUser(this, user)
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    showErrorSnackBar("Image upload failed: ${it.message}")
                }
        }
    }

    private fun getFileExtension(uri: Uri?): String? {
        return contentResolver.getType(uri!!)?.let {
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
    }

    // ========== Validation ==========
    private fun validateForm(name: String, username: String, email: String, password: String, phone: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter your name.")
                false
            }
            TextUtils.isEmpty(username) -> {
                showErrorSnackBar("Please enter your username.")
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter your email.")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter your password.")
                false
            }
            TextUtils.isEmpty(phone) -> {
                showErrorSnackBar("Please enter your phone number.")
                false
            }
            selectedPhilhealthIdUri == null -> {
                showErrorSnackBar("Please upload your PhilHealth ID image.")
                false
            }
            else -> true
        }
    }

    // ========== Registration Success ==========
    fun userRegisteredSuccess() {
        hideProgressDialog()
        showCustomToast("You have successfully registered.")

        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
