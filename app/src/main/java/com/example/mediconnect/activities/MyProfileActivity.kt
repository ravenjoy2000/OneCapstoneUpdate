package com.example.mediconnect.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.example.mediconnect.utils.Constants
import com.google.common.io.Files.getFileExtension
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 1
        private const val PICK_IMAGE_REQUEST_CODE = 2
    }

    private var mSelectedImageFileUri = Uri.EMPTY
    private var mProfileImageURL : String = ""

    private lateinit var mUserDetails: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_profile)


        setupActionBar()



        FireStoreClass().loadUserData(this)




        //###BTN IMAGE####
        val iv_profile_user_image = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_profile_user_image)
        iv_profile_user_image.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    READ_STORAGE_PERMISSION_CODE
                )
            }
        } // End of iv_profile_user_image.setOnClickListener





        //####BTN UPDATE####
        val btn_update = findViewById<Button>(R.id.btn_update)
        btn_update.setOnClickListener {
            if (!::mUserDetails.isInitialized) {
                Toast.makeText(this, "Please wait, loading user data...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mSelectedImageFileUri != Uri.EMPTY){
                uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()

            }
        }





    }// End of onCreate()





    //--------------------- Toolbar or ActionBar ------------------------------------
    private fun setupActionBar() {
        // Gamitin ang custom toolbar (na nasa layout) bilang default ActionBar ng activity
        setSupportActionBar(findViewById(R.id.toolbar_my_profile_activity))

        // Kunin ang ActionBar na isin-et sa itaas
        // Tapos ipakita ang "back arrow" (up button) sa kaliwa ng ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Palitan ang default back arrow icon ng isang custom drawable (arrow back na iOS-style)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)

        // Itakda ang title ng ActionBar gamit ang string resource (ex: "My Profile")
        supportActionBar?.title = resources.getString(R.string.my_profile_title)

        // Mag-assign ng navigation click listener sa toolbar mismo
        // Kapag pinindot ang back arrow, tatawagin ang onBackPressed() para bumalik sa previous screen
        val toolbar_my_profile_activity = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_my_profile_activity)
        toolbar_my_profile_activity.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    //--------------------------END-------------------------------------------






    //---------------------------- Access Gallery to Select Image --------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                showImageChooser()
            }else{
                showErrorSnackBar("Oops, you just denied the permission for storage. You can also allow it from settings.")
            }
        }
    }

    private fun showImageChooser(){
        var galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE && data!!.data != null){
            mSelectedImageFileUri = data.data!!

            try{
                Glide
                    .with(this@MyProfileActivity) // Context: itong activity
                    .load(mSelectedImageFileUri) // I-load ang image URL ng user
                    .centerCrop() // I-crop ang image para kumasya nang maayos sa ImageView
                    .placeholder(R.drawable.ic_user_place_holder) // Temporary placeholder image habang naglo-load pa
                    .into(findViewById(R.id.iv_profile_user_image)) // Ipasok ang image sa ImageView na may ID na 'iv_user_image'
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

    }
    //------------------------------End----------------------------------








    //------------------- Show the user's data in the UI form Firestore Database except the image ------------------------------------
    fun setUserDataInUI(user: User) {

        mUserDetails = user

        // Gamitin ang Glide library para i-load ang user's profile image mula sa URL (user.image)
        Glide
            .with(this@MyProfileActivity) // Context: itong activity
            .load(user.image) // I-load ang image URL ng user
            .centerCrop() // I-crop ang image para kumasya nang maayos sa ImageView
            .placeholder(R.drawable.ic_user_place_holder) // Temporary placeholder image habang naglo-load pa
            .into(findViewById(R.id.iv_profile_user_image)) // Ipasok ang image sa ImageView na may ID na 'iv_user_image'

        // I-set ang user's name sa EditText field na may ID na 'et_name'
        findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.et_name).setText(user.name)

        // I-set ang user's email sa EditText field na may ID na 'et_email'
        findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.et_email).setText(user.email)

        // I-check muna kung may laman ang mobile number (hindi 0) bago ito i-set
        if (user.mobile != 0L) {
            // I-convert ang number (na Long) into String para ma-format natin
            val mobile = user.mobile.toString()

            // Hatiin ang number para maging format ng PH number: +63 915 123 4567
            val formattedMobile = "+63 ${mobile.substring(0, 3)} ${mobile.substring(3, 6)} ${mobile.substring(6)}"
            // Halimbawa: "9151234567"
            // → mobile.substring(0, 3) = "915"
            // → mobile.substring(3, 6) = "123"
            // → mobile.substring(6)     = "4567"
            // Final result: "+63 915 123 4567"

            // I-set ang formatted mobile number sa EditText na may ID 'et_mobile'
            findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.et_mobile)
                .setText(formattedMobile)
        }
    }


    private fun updateUserProfileData(){
        val userHashMap = HashMap<String, Any>()
        var anyChangeMade = false

        // Fix 1: Image comparison
        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangeMade = true
        }

        // Fix 2: Trim name input
        val et_name = findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.et_name)
        val inputName = et_name.text.toString().trim()
        if (inputName != mUserDetails.name.trim()) {
            userHashMap[Constants.NAME] = inputName
            anyChangeMade = true
        }

        // Fix 3: Normalize mobile number
        val et_mobile = findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.et_mobile)
        val inputMobile = et_mobile.text.toString()
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
                Toast.makeText(this, "Invalid mobile number format.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Final call
        if (anyChangeMade){
            Log.d("UpdateDebug", "Updating fields: $userHashMap")
            FireStoreClass().updateUserProfileData(this, userHashMap)

        } else {
            Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
        }


    }



    //--------------------------END-------------------------------------------






    //----------------------------Upload Image in Storage--------------------------------
    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != Uri.EMPTY) {

            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(
                    mSelectedImageFileUri
                )
            )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                Log.i(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.i("Downloadable Image URL", uri.toString())
                    mProfileImageURL = uri.toString()

                    updateUserProfileData()
                }

                }.addOnFailureListener { exception ->
                    Toast.makeText(
                    this@MyProfileActivity,
                    exception.message,
                    Toast.LENGTH_LONG
                    ).show()

                }
        }

    }

    private fun getFileExtension(uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    fun profileUpdateSuccess(){
        hideProgressDialog()
        Toast.makeText(this@MyProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }
    //--------------------------End-----------------------------------------
















}