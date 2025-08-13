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

    // ========== Variables para sa PhilHealth ID image ==========
    private var selectedPhilhealthIdUri: Uri? = null  // URI ng napiling PhilHealth ID image
    private var mPhilIdGovermentImageURL: String = "" // URL sa Firebase Storage ng uploaded image

    // ========== UI Elements ==========
    private lateinit var etName: EditText  // Input para sa pangalan
    private lateinit var etEmail: EditText  // Input para sa email
    private lateinit var etPassword: EditText  // Input para sa password
    private lateinit var etPhone: EditText  // Input para sa phone number
    private lateinit var imgPhilhealthPreview: ImageView  // Preview ng uploaded PhilHealth ID image
    private lateinit var btnUpload: Button  // Button para mag-upload ng PhilHealth ID image
    private lateinit var btnSignUp: Button  // Button para mag-register ng user

    // ========== ActivityResultLauncher para sa image picker ==========
    private val pickImageLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            // Kapag may napiling image
            if (uri != null) {
                selectedPhilhealthIdUri = uri  // I-save ang URI ng image
                imgPhilhealthPreview.setImageURI(uri) // Ipakita ang preview sa ImageView
            }
        }

    // ========== onCreate lifecycle method ==========
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()  // Para sa edge-to-edge layout support
        setContentView(R.layout.activity_sign_up)  // I-set ang layout ng activity

        makeFullScreen()  // Itago ang status bar para maging fullscreen
        setupActionBar()  // I-setup ang toolbar na may back button
        bindViews()  // I-bind ang mga UI elements sa variables
        setupListeners()  // I-setup ang mga click listeners ng buttons
    }

    // I-bind ang mga UI elements gamit findViewById
    private fun bindViews() {
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etPhone = findViewById(R.id.et_phone)
        imgPhilhealthPreview = findViewById(R.id.img_philhealth_id_preview)
        btnUpload = findViewById(R.id.btn_upload_philhealth_id)
        btnSignUp = findViewById(R.id.btn_sign_up)
    }

    // Itago ang status bar para fullscreen experience
    private fun makeFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para sa Android 11+ (R)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    // Setup toolbar na may back button
    private fun setupActionBar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // Ipakita ang back button
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24) // Icon ng back button
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }  // Ibalik sa previous screen kapag pinindot
    }

    // Setup click listeners para sa buttons
    private fun setupListeners() {
        btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*") // Buksan ang image picker para pumili ng larawan
        }

        btnSignUp.setOnClickListener {
            registerUser()  // Simulan ang proseso ng user registration
        }
    }

    // Function para mag-register ng bagong user
    private fun registerUser() {
        // Kunin ang mga input na string at i-trim ang whitespace
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        // I-validate ang mga inputs bago magproceed
        if (validateForm(name,  email, password, phone)) {
            showProgressDialog("Please wait...") // Ipakita ang progress dialog

            // Gamitin ang Firebase Authentication para gumawa ng bagong user
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user  // Kunin ang user object mula sa resulta
                    if (user != null && selectedPhilhealthIdUri != null) {
                        // Mag-send ng verification email sa bagong user
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                // Ipakita ang success message sa user
                                showCustomToast("Verification email sent to ${user.email}. Please verify before logging in.")
                                // I-upload ang PhilHealth ID image sa Firebase Storage
                                uploadPhilhealthIdImage(user.uid, name, email, phone)
                            }
                            .addOnFailureListener {
                                hideProgressDialog() // Itago ang progress dialog kapag may error
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

    // Function para i-upload ang PhilHealth ID image sa Firebase Storage
    private fun uploadPhilhealthIdImage(userId: String, name: String, email: String, phone: String) {
        // Kunin ang file extension ng selected image (png, jpg, etc)
        val extension = getFileExtension(selectedPhilhealthIdUri)

        // Gumawa ng reference sa Firebase Storage gamit ang unique filename
        val storageRef = FirebaseStorage.getInstance().reference
            .child("PhilhealthID_${System.currentTimeMillis()}.$extension")

        selectedPhilhealthIdUri?.let { uri ->
            // I-upload ang file sa Storage
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // Kapag successful, kunin ang download URL ng image
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        mPhilIdGovermentImageURL = downloadUri.toString()  // I-save ang URL

                        // Gumawa ng User object para i-save sa Firestore
                        val user = User(userId, name, email, mPhilIdGovermentImageURL, "patient", phone)

                        // Tawagin ang FireStoreClass para i-register ang user sa Firestore
                        FireStoreClass().registerUser(this, user)
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    showErrorSnackBar("Image upload failed: ${it.message}")  // Ipakita error kung nabigo
                }
        }
    }

    // Helper function para makuha ang file extension ng image mula sa URI
    private fun getFileExtension(uri: Uri?): String? {
        return contentResolver.getType(uri!!)?.let {
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
    }

    // Validation ng mga input fields at uploaded image
    private fun validateForm(name: String, email: String, password: String, phone: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter your name.")  // Error kapag walang pangalan
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter your email.")  // Error kapag walang email
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter your password.")  // Error kapag walang password
                false
            }
            TextUtils.isEmpty(phone) -> {
                showErrorSnackBar("Please enter your phone number.")  // Error kapag walang phone
                false
            }
            selectedPhilhealthIdUri == null -> {
                showErrorSnackBar("Please upload your PhilHealth ID image.")  // Error kapag walang image upload
                false
            }
            else -> true  // Lahat ay valid
        }
    }

    // Method na tatawagin kapag successful ang registration ng user sa Firestore
    fun userRegisteredSuccess() {
        hideProgressDialog()  // Itago ang loading dialog
        showCustomToast("You have successfully registered.")  // Ipakita success message

        // Redirect sa SignInActivity at i-clear ang previous activities
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()  // Tapusin ang current activity
    }
}
