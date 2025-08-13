package com.example.mediconnect.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowInsets
import android.view.WindowManager
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
        // Constant na code para sa permission request sa storage
        private const val READ_STORAGE_PERMISSION_CODE = 1
        // Constant na code para sa pagpili ng profile image mula gallery
        private const val PICK_IMAGE_REQUEST_CODE = 2
        // Constant na code para sa pagpili ng PhilHealth ID image mula gallery
        private const val PICK_PHILHEALTH_IMAGE_REQUEST_CODE = 1001
    }

    // Variables para sa mga napiling image URI at URL ng images sa Firebase Storage
    private var mSelectedImageFileUri = Uri.EMPTY
    private var mProfileImageURL: String = ""

    private var mSelectedPhilHealthImageUri: Uri = Uri.EMPTY
    private var mPhilIdGovermentImageURL: String = ""

    // Variable para sa user data na naka-load mula Firestore
    private lateinit var mUserDetails: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)  // I-set ang layout file

        // Itago ang status bar depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()              // I-setup ang toolbar/action bar
        FireStoreClass().loadUserData(this)  // Kunin ang user data mula Firestore

        // Kapag pinindot ang profile image, i-handle ang permission at open gallery
        findViewById<ImageView>(R.id.iv_profile_user_image).setOnClickListener {
            handlePermissionAndOpenGallery(PICK_IMAGE_REQUEST_CODE)
        }

        // Kapag pinindot ang PhilHealth ID button, i-handle permission at open gallery
        findViewById<Button>(R.id.btn_change_philhealth_id).setOnClickListener {
            handlePermissionAndOpenGallery(PICK_PHILHEALTH_IMAGE_REQUEST_CODE)
        }

        // Kapag pinindot ang Update button, i-update ang profile
        findViewById<Button>(R.id.btn_update).setOnClickListener {
            // Check kung na-load na ang user data bago mag-update
            if (!::mUserDetails.isInitialized) {
                Toast.makeText(this, "Please wait, loading user data...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kung may bagong profile image na napili, i-upload muna
            when {
                mSelectedImageFileUri != Uri.EMPTY -> uploadUserImage()
                // Kung may bagong PhilHealth image na napili, i-upload muna
                mSelectedPhilHealthImageUri != Uri.EMPTY -> uploadPhilHealthImage()
                else -> {
                    // Walang bagong image, diretso na i-update ang ibang data
                    showProgressDialog("Please wait...")
                    updateUserProfileData()
                }
            }
        }
    }

    /**
     * I-setup ang custom toolbar at back button
     */
    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_my_profile_activity))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)                          // I-enable ang back arrow
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24) // Icon ng back arrow
            title = resources.getString(R.string.my_profile_title)  // Title ng toolbar
        }

        // Kapag pinindot ang back arrow, bumalik sa previous activity
        findViewById<Toolbar>(R.id.toolbar_my_profile_activity)
            .setNavigationOnClickListener { onBackPressed() }
    }

    /**
     * I-handle ang permission request at buksan ang gallery kung aprubado
     */
    private fun handlePermissionAndOpenGallery(requestCode: Int) {
        // Para sa Android 13 pataas, ibang permission ang kailangan
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        // Check kung naaprubahan na ang permission
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery(requestCode)  // Buksan ang gallery
        } else {
            // Hilingin ang permission kung wala pa
            ActivityCompat.requestPermissions(this, arrayOf(permission), READ_STORAGE_PERMISSION_CODE)
        }
    }

    // Buksan ang gallery para pumili ng larawan
    private fun openGallery(requestCode: Int) {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, requestCode)
    }

    // Resulta ng permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            // Kapag pinayagan, buksan ang gallery para sa profile image by default
            openGallery(PICK_IMAGE_REQUEST_CODE)
        } else {
            // Kapag tinanggihan, ipakita error message
            showErrorSnackBar("Permission denied. You can enable it from settings.")
        }
    }

    // Resulta ng pagpili ng larawan mula gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check kung may nakuha talagang image
        if (resultCode == RESULT_OK && data?.data != null) {
            when (requestCode) {
                // Kapag profile image ang pinili
                PICK_IMAGE_REQUEST_CODE -> {
                    mSelectedImageFileUri = data.data!!  // I-save ang Uri
                    Glide.with(this).load(data.data).centerCrop()   // I-load gamit Glide
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(findViewById(R.id.iv_profile_user_image)) // Ipakita sa ImageView
                }
                // Kapag PhilHealth ID image ang pinili
                PICK_PHILHEALTH_IMAGE_REQUEST_CODE -> {
                    mSelectedPhilHealthImageUri = data.data!!       // I-save ang Uri
                    Glide.with(this).load(data.data).centerCrop()  // I-load gamit Glide
                        .into(findViewById(R.id.change_philhealth_id_preview))  // Ipakita sa preview
                }
            }
        }
    }

    /**
     * I-upload ang profile image sa Firebase Storage
     */
    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait)) // Ipakita loading dialog

        // Gumawa ng reference para sa image file na iu-upload
        val fileRef = FirebaseStorage.getInstance().reference.child(
            "USER_IMAGE_${System.currentTimeMillis()}.${getFileExtension(mSelectedImageFileUri)}"
        )

        // I-upload ang file sa Firebase Storage
        fileRef.putFile(mSelectedImageFileUri).addOnSuccessListener {
            it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                mProfileImageURL = uri.toString()  // Kunin ang download URL
                updateUserProfileData()            // I-update ang profile data sa Firestore
            }
        }.addOnFailureListener {
            hideProgressDialog()                 // Itago ang loading dialog
            Toast.makeText(this, "${it.message}", Toast.LENGTH_LONG).show()  // Ipakita error
        }
    }

    /**
     * I-upload ang PhilHealth ID image sa Firebase Storage
     */
    private fun uploadPhilHealthImage() {
        showProgressDialog("Please wait...")   // Ipakita loading dialog

        // Gumawa ng reference para sa PhilHealth ID image file
        val fileRef = FirebaseStorage.getInstance().reference.child(
            "PHILHEALTH_ID_${System.currentTimeMillis()}.${getFileExtension(mSelectedPhilHealthImageUri)}"
        )

        // I-upload ang file sa Firebase Storage
        fileRef.putFile(mSelectedPhilHealthImageUri).addOnSuccessListener {
            it.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                mPhilIdGovermentImageURL = uri.toString()  // Kunin ang download URL
                sendImageToFirestore()                     // I-save ang URL sa Firestore user profile
            }
        }.addOnFailureListener {
            hideProgressDialog()                          // Itago ang loading dialog
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()  // Ipakita error
        }
    }

    // Kunin ang file extension mula sa Uri ng image
    private fun getFileExtension(uri: Uri): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri))
    }

    /**
     * I-update ang user info sa Firestore
     */
    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()
        var anyChangeMade = false

        // Kung may bagong profile image URL at iba sa dati, idagdag sa update map
        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangeMade = true
        }

        // Kunin ang pangalan mula sa input field
        val inputName = findViewById<AppCompatEditText>(R.id.et_name).text.toString().trim()
        // Kung iba sa dati, idagdag sa update map
        if (inputName != mUserDetails.name) {
            userHashMap[Constants.NAME] = inputName
            anyChangeMade = true
        }

        // Kunin ang mobile number mula sa input at linisin ang mga space at symbol
        val inputMobile = findViewById<AppCompatEditText>(R.id.et_mobile).text.toString()
            .replace("", "")
            .replace(" ", "")
            .replace("-", "")
            .removePrefix("")
            .trim()

        // Kung iba sa dati, idagdag sa update map
        if (inputMobile != mUserDetails.phone.toString()) {
            try {
                userHashMap[Constants.MOBILE] = inputMobile.toLong()
                anyChangeMade = true
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid mobile number.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Kung may anumang pagbabago, i-update sa Firestore
        if (anyChangeMade) {
            FireStoreClass().updateUserProfileData(this, userHashMap)
        } else {
            Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
        }
    }

    // I-save ang PhilHealth ID image URL sa Firestore
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
     * Ipakita ang user data sa UI mula sa Firestore User object
     */
    fun setUserDataInUI(user: User) {
        mUserDetails = user  // Save user data locally

        // Load profile image
        Glide.with(this)
            .load(user.image)
            .placeholder(R.drawable.ic_user_place_holder)
            .centerCrop()
            .into(findViewById(R.id.iv_profile_user_image))

        // Show name in input field
        findViewById<AppCompatEditText>(R.id.et_name).setText(user.name)

        // Show email in input field
        findViewById<AppCompatEditText>(R.id.et_email).setText(user.email)

        // Show mobile number if available
        if (user.phone != "") {
            val mobile = " ${user.phone.toString().chunked(3).joinToString(" ")}"
            findViewById<AppCompatEditText>(R.id.et_mobile).setText(mobile)
        }

        // Show PhilHealth ID image if available
        if (user.goverment_or_phealtID.isNotEmpty()) {
            Glide.with(this)
                .load(user.goverment_or_phealtID)
                .placeholder(R.drawable.cuteperson)
                .into(findViewById(R.id.change_philhealth_id_preview))
        }
    }


    /**
     * Ipatupad kapag successful ang profile update
     */
    fun profileUpdateSuccess() {
        hideProgressDialog()   // Itago ang loading dialog
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

        // I-restart ang app sa SplashActivity (o pwede rin MainActivity)
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

        finish()  // Isara ang current activity
    }

}
