// Package kung saan nakalagay ang activity na ito
package com.example.mediconnect.activities

// Mga import para sa UI, Firebase, at Android features
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.example.mediconnect.utils.Constants
import com.google.common.io.Files.getFileExtension
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// Ang activity na nagha-handle ng user sign-up (registration)
class SignUpActivity : BaseActivity() {


    //------ Kasama sa Goverment Id or Philhealt Id
    private var selectedPhilhealthIdUri: Uri? = null

    private var mPhilIdGovermentImageURL : String = ""
    private val PICK_PHILHEALTH_IMAGE_CODE = 1001


    //--------- End

    // Main function na tumatakbo pag bukas ang activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Para sa full screen layout
        setContentView(R.layout.activity_sign_up) // I-set ang layout

        // ========= REMOVE STATUS BAR / FULLSCREEN ============
        window.setFlags( // Para sa old Android version
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Para sa bagong Android version
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }

        // =====================================================

        setupActionbar() // Tawagin ang toolbar setup

        // ========== BUTTON: Sign Up =============
        val btn_for_sign_up = findViewById<Button>(R.id.btn_sign_up)
        btn_for_sign_up.setOnClickListener {
            registersUser() // Kapag pinindot ang sign up, tawagin ang register function
        }


        //------ Kasama sa Goverment Id or Philhealt Id
        val btnUploadPhilhealth = findViewById<Button>(R.id.btn_upload_philhealth_id)
        val imgPreview = findViewById<ImageView>(R.id.img_philhealth_id_preview)

        btnUploadPhilhealth.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_PHILHEALTH_IMAGE_CODE)
        }
        //--------- End

    }

    // ========== TOOLBAR / ACTION BAR SETUP ============
    private fun setupActionbar() {
        val toolbar_for_sign_up_activity =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar_for_sign_up_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true) // Enable back arrow
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24) // Set back icon
        }

        // Kapag pinindot ang back icon, bumalik sa previous screen
        toolbar_for_sign_up_activity.setNavigationOnClickListener { onBackPressed() }
    }

    // ========== VALIDATION NG USER INPUT ============
    private fun validateForm(name: String, username: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter name.") // Walang name
                false
            }

            TextUtils.isEmpty(username) -> {
                showErrorSnackBar("Please enter username.") // Walang username
                false
            }

            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.") // Walang email
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.") // Walang password
                false
            }

            selectedPhilhealthIdUri == null -> {
                showErrorSnackBar("Please upload your PhilHealth ID image.")
                false
            }

            else -> {
                true // Kung kumpleto lahat, magpatuloy
            }
        }
    }

    // ========== MAGRE-REGISTER NG BAGONG USER SA FIREBASE AUTH & FIRESTORE ============
    private fun registersUser() {
        val name = findViewById<EditText>(R.id.et_name).text.toString().trim()
        val username = findViewById<EditText>(R.id.et_username).text.toString().trim()
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()

        if (validateForm(name, username, email, password)) {
            showProgressDialog("Please wait")

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user
                    if (firebaseUser != null && selectedPhilhealthIdUri != null) {
                        // Upload the image first
                        uploadPhilhealthIdImage(firebaseUser.uid, name, username, email)
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


    // ========== CALLBACK MULA FireStoreClass PAG MATAGUMPAY ANG REGISTRATION ============
    fun userRegisteredSuccess() {
        showCustomToast("You have successfully registered") // Ipakita ang toast
        hideProgressDialog() // Alisin ang loading

        val intent = Intent(this, SignInActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // para hindi na makabalik sa SignUpActivity pag pinindot ang back
    }


    //------ Kasama sa Goverment Id or Philhealt Id
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PHILHEALTH_IMAGE_CODE && resultCode == RESULT_OK && data != null) {
            val imgPreview = findViewById<ImageView>(R.id.img_philhealth_id_preview)
            selectedPhilhealthIdUri = data.data
            imgPreview.setImageURI(selectedPhilhealthIdUri)
        }
    }
    //------ End


    private fun uploadPhilhealthIdImage(userId: String, name: String, username: String, email: String) {
        val extension = getFileExtension(selectedPhilhealthIdUri)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("PhilhealthID_${System.currentTimeMillis()}.$extension")

        selectedPhilhealthIdUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        mPhilIdGovermentImageURL = downloadUri.toString()

                        val user = User(
                            userId,
                            name,
                            username,
                            email,
                            mPhilIdGovermentImageURL
                        )

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
            android.webkit.MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(it)
        }
    }




} // END NG SignUpActivity CLASS
