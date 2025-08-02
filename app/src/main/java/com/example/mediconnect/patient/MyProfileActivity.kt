package com.example.mediconnect.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.SplashActivity
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.example.mediconnect.utils.Constants
import com.google.firebase.storage.FirebaseStorage

class MyProfileActivity : BaseActivity() {

    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 1
        private const val PICK_IMAGE_REQUEST_CODE = 2
        private const val PICK_PHILHEALTH_IMAGE_REQUEST_CODE = 1001
    }

    // Image URIs
    private var mSelectedImageFileUri = Uri.EMPTY
    private var mProfileImageURL: String = ""

    private var mSelectedPhilHealthImageUri: Uri = Uri.EMPTY
    private var mPhilIdGovermentImageURL: String = ""

    private lateinit var mUserDetails: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupActionBar()
        FireStoreClass().loadUserData(this)

        // Setup profile image click
        findViewById<ImageView>(R.id.iv_profile_user_image).setOnClickListener {
            handlePermissionAndOpenGallery(PICK_IMAGE_REQUEST_CODE)
        }

        // Setup PhilHealth image click
        findViewById<Button>(R.id.btn_change_philhealth_id).setOnClickListener {
            handlePermissionAndOpenGallery(PICK_PHILHEALTH_IMAGE_REQUEST_CODE)
        }

        // Update profile button
        findViewById<Button>(R.id.btn_update).setOnClickListener {
            if (!::mUserDetails.isInitialized) {
                Toast.makeText(this, "Please wait, loading user data...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when {
                mSelectedImageFileUri != Uri.EMPTY -> uploadUserImage()
                mSelectedPhilHealthImageUri != Uri.EMPTY -> uploadPhilHealthImage()
                else -> {
                    showProgressDialog("Please wait...")
                    updateUserProfileData()
                }
            }
        }
    }

    /**
     * Sets up the custom toolbar.
     */
    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_my_profile_activity))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = resources.getString(R.string.my_profile_title)
        }

        findViewById<Toolbar>(R.id.toolbar_my_profile_activity)
            .setNavigationOnClickListener { onBackPressed() }
    }

    /**
     * Handles permission requests and launches the gallery.
     */
    private fun handlePermissionAndOpenGallery(requestCode: Int) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery(requestCode)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), READ_STORAGE_PERMISSION_CODE)
        }
    }

    private fun openGallery(requestCode: Int) {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openGallery(PICK_IMAGE_REQUEST_CODE) // default to profile image
        } else {
            showErrorSnackBar("Permission denied. You can enable it from settings.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data?.data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST_CODE -> {
                    mSelectedImageFileUri = data.data!!
                    Glide.with(this).load(data.data).centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(findViewById(R.id.iv_profile_user_image))
                }
                PICK_PHILHEALTH_IMAGE_REQUEST_CODE -> {
                    mSelectedPhilHealthImageUri = data.data!!
                    Glide.with(this).load(data.data).centerCrop()
                        .into(findViewById(R.id.change_philhealth_id_preview))
                }
            }
        }
    }

    /**
     * Upload profile image to Firebase Storage.
     */
    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        val fileRef = FirebaseStorage.getInstance().reference.child(
            "USER_IMAGE_${System.currentTimeMillis()}.${getFileExtension(mSelectedImageFileUri)}"
        )

        fileRef.putFile(mSelectedImageFileUri).addOnSuccessListener {
            it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                mProfileImageURL = uri.toString()
                updateUserProfileData()
            }
        }.addOnFailureListener {
            hideProgressDialog()
            Toast.makeText(this, "${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadPhilHealthImage() {
        showProgressDialog("Please wait...")

        val fileRef = FirebaseStorage.getInstance().reference.child(
            "PHILHEALTH_ID_${System.currentTimeMillis()}.${getFileExtension(mSelectedPhilHealthImageUri)}"
        )

        fileRef.putFile(mSelectedPhilHealthImageUri).addOnSuccessListener {
            it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                mPhilIdGovermentImageURL = uri.toString()
                sendImageToFirestore()
            }
        }.addOnFailureListener {
            hideProgressDialog()
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri))
    }

    /**
     * Update user info in Firestore.
     */
    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()
        var anyChangeMade = false

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangeMade = true
        }

        val inputName = findViewById<AppCompatEditText>(R.id.et_name).text.toString().trim()
        if (inputName != mUserDetails.name) {
            userHashMap[Constants.NAME] = inputName
            anyChangeMade = true
        }

        val inputMobile = findViewById<AppCompatEditText>(R.id.et_mobile).text.toString()
            .replace("+63", "")
            .replace(" ", "")
            .replace("-", "")
            .removePrefix("0")
            .trim()

        if (inputMobile != mUserDetails.mobile.toString()) {
            try {
                userHashMap[Constants.MOBILE] = inputMobile.toLong()
                anyChangeMade = true
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid mobile number.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (anyChangeMade) {
            FireStoreClass().updateUserProfileData(this, userHashMap)
        } else {
            Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageToFirestore() {
        val userHashMap = HashMap<String, Any>()
        if (mPhilIdGovermentImageURL.isNotEmpty()) {
            userHashMap[Constants.GOVERNMENT_OR_PHILHEALTH_ID] = mPhilIdGovermentImageURL
            FireStoreClass().updateUserProfileData(this, userHashMap)
        } else {
            Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Populate UI with user data from Firestore.
     */
    fun setUserDataInUI(user: User) {
        mUserDetails = user

        Glide.with(this)
            .load(user.image)
            .placeholder(R.drawable.ic_user_place_holder)
            .centerCrop()
            .into(findViewById(R.id.iv_profile_user_image))

        findViewById<AppCompatEditText>(R.id.et_name).setText(user.name)
        findViewById<AppCompatEditText>(R.id.et_username).setText(user.username)
        findViewById<AppCompatEditText>(R.id.et_email).setText(user.email)

        if (user.mobile != 0L) {
            val mobile = "+63 ${user.mobile.toString().chunked(3).joinToString(" ")}"
            findViewById<AppCompatEditText>(R.id.et_mobile).setText(mobile)
        }

        if (user.goverment_or_phealtID.isNotEmpty()) {
            Glide.with(this)
                .load(user.goverment_or_phealtID)
                .placeholder(R.drawable.cuteperson)
                .into(findViewById(R.id.change_philhealth_id_preview))
        }
    }

    fun profileUpdateSuccess() {
        hideProgressDialog()
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

        // Restart the app by launching the splash or main entry activity
        val intent = Intent(this, SplashActivity::class.java) // or MainActivity or LoginActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

        finish()
    }

}