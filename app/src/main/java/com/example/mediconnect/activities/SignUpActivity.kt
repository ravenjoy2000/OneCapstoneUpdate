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
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.common.io.Files.getFileExtension

class SignUpActivity : BaseActivity() {

    // ========== Constants & Variables ==========
    private val PICK_PHILHEALTH_IMAGE_CODE = 1001
    private var selectedPhilhealthIdUri: Uri? = null
    private var mPhilIdGovermentImageURL: String = ""

    // ========== Lifecycle ==========
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        makeFullScreen()
        setupActionBar()
        setupListeners()
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
        findViewById<Button>(R.id.btn_sign_up).setOnClickListener {
            registerUser()
        }

        findViewById<Button>(R.id.btn_upload_philhealth_id).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_PHILHEALTH_IMAGE_CODE)
        }
    }

    // ========== Register User ==========
    private fun registerUser() {
        val name = findViewById<EditText>(R.id.et_name).text.toString().trim()
        val username = findViewById<EditText>(R.id.et_username).text.toString().trim()
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

        if (validateForm(name, username, email, password)) {
            showProgressDialog("Please wait")

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null && selectedPhilhealthIdUri != null) {
                        uploadPhilhealthIdImage(user.uid, name, username, email)
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
    private fun uploadPhilhealthIdImage(userId: String, name: String, username: String, email: String) {
        val extension = getFileExtension(selectedPhilhealthIdUri)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("PhilhealthID_${System.currentTimeMillis()}.$extension")

        selectedPhilhealthIdUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        mPhilIdGovermentImageURL = downloadUri.toString()
                        val user = User(userId, name, username, email, mPhilIdGovermentImageURL)
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
    private fun validateForm(name: String, username: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter name.")
                false
            }
            TextUtils.isEmpty(username) -> {
                showErrorSnackBar("Please enter username.")
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.")
                false
            }
            selectedPhilhealthIdUri == null -> {
                showErrorSnackBar("Please upload your PhilHealth ID image.")
                false
            }
            else -> true
        }
    }

    // ========== Success Feedback ==========
    fun userRegisteredSuccess() {
        showCustomToast("You have successfully registered")
        hideProgressDialog()

        val intent = Intent(this, SignInActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    // ========== Image Preview After Pick ==========
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PHILHEALTH_IMAGE_CODE && resultCode == RESULT_OK && data != null) {
            selectedPhilhealthIdUri = data.data
            findViewById<ImageView>(R.id.img_philhealth_id_preview).setImageURI(selectedPhilhealthIdUri)
        }
    }
}
