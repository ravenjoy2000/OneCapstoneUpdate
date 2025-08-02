package com.example.mediconnect.firebase

// Required imports
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.mediconnect.activities.*
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.patient.MyProfileActivity
import com.example.mediconnect.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * FireStoreClass
 * Handles all Firebase Firestore-related operations such as:
 * - Registering a user
 * - Loading user data
 * - Updating profile info
 */
class FireStoreClass : BaseActivity() {

    // Instance of Firestore database
    private val mFireStore = FirebaseFirestore.getInstance()

    // ---------------------------------------------------------------------------------------
    // 🔹 FUNCTION: Register User
    // Registers a new user in Firestore database using UID as document ID.
    // Called after successful Firebase Authentication in SignUpActivity.
    // ---------------------------------------------------------------------------------------
    fun registerUser(activity: SignUpActivity, @SuppressLint("RestrictedApi") userInfo: User) {
        mFireStore.collection(Constants.USERS)               // Go to "users" collection
            .document(getCurrentUserID())                    // Use UID as document ID
            .set(userInfo, SetOptions.merge())               // Save user info (merge to keep existing fields)
            .addOnSuccessListener {
                activity.userRegisteredSuccess()             // Notify SignUpActivity of success
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()                // Hide loading if error occurs
                Log.e(activity.javaClass.simpleName, "Error writing document", e)
            }
    }

    // ---------------------------------------------------------------------------------------
    // 🔹 FUNCTION: Load User Data
    // Fetches user info from Firestore and sends it back to the appropriate Activity.
    // Supports: SignInActivity, MainActivity, MyProfileActivity.
    // ---------------------------------------------------------------------------------------
    fun loadUserData(activity: Activity) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)

                when (activity) {
                    is SignInActivity -> {
                        if (loggedInUser != null) {
                            activity.signInSuccess(loggedInUser)
                        }
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser!!)
                    }
                    is MyProfileActivity -> {
                        activity.setUserDataInUI(loggedInUser!!)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Hide progress dialog based on activity
                when (activity) {
                    is SignInActivity -> activity.hideProgressDialog()
                    is MainActivity -> activity.hideProgressDialog()
                    is MyProfileActivity -> activity.hideProgressDialog()
                }

                Log.e(activity.javaClass.simpleName, "Error loading user data", e)
            }
    }

    // ---------------------------------------------------------------------------------------
    // 🔹 FUNCTION: Update User Profile
    // Updates only specific fields in the user document (ex: name, mobile, image, etc.).
    // Called from MyProfileActivity when user taps Save button.
    // ---------------------------------------------------------------------------------------
    fun updateUserProfileData(activity: MyProfileActivity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "Profile updated successfully")
                activity.showProccedSnacBar("Profile updated successfully")
                activity.profileUpdateSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while updating profile", e)
            }
    }

    // ---------------------------------------------------------------------------------------
    // 🔹 FUNCTION: Get Current User ID (Assumed in BaseActivity)
    // This is commented out because your BaseActivity already includes it.
    // ---------------------------------------------------------------------------------------
//    fun getCurrentUserID(): String {
//        return FirebaseAuth.getInstance().currentUser!!.uid
//    }

    fun getCurrentUserRole(onRoleFetched: (String) -> Unit) {
        val currentUserID = getCurrentUserID()
        if (currentUserID.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users") // or your actual collection name
                .document(currentUserID)
                .get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: ""
                    onRoleFetched(role)
                }
                .addOnFailureListener {
                    onRoleFetched("") // return empty on failure
                }
        } else {
            onRoleFetched("") // no user logged in
        }
    }


} // 🔚 End of FireStoreClass
