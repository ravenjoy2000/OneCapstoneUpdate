package com.example.mediconnect.firebase

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.mediconnect.activities.*
import com.example.mediconnect.doctor.DoctorDashboardActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.patient.MainActivity
import com.example.mediconnect.patient.MyProfileActivity
import com.example.mediconnect.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Firestore helper class para sa mga operations sa Firebase Firestore database
class FireStoreClass : BaseActivity() {

    private val mFireStore = FirebaseFirestore.getInstance()   // Instance ng Firestore database

    // 🔹 I-register ang bagong user sa Firestore
    fun registerUser(activity: SignUpActivity, @SuppressLint("RestrictedApi") userInfo: User) {
        mFireStore.collection(Constants.USERS)                   // Koleksyon ng users
            .document(getCurrentUserID())                        // Dokumento gamit ang current user ID
            .set(userInfo, SetOptions.merge())                   // Isave ang user info, i-merge kung may existing
            .addOnSuccessListener {
                activity.userRegisteredSuccess()                 // Tawagin kapag successful ang registration
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()                     // Itago ang loading dialog sa failure
                Log.e(activity.javaClass.simpleName, "Error writing document", e)  // I-log ang error
            }
    }

    // 🔹 Kunin ang kasalukuyang user data mula sa Firestore
    fun loadUserData(activity: Activity) {
        val currentUserId = getCurrentUserID()                   // Kunin ang kasalukuyang user ID

        mFireStore.collection(Constants.USERS)
            .document(currentUserId)
            .get()                                               // Kuhanin ang dokumento ng user
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {                           // Kung may dokumento
                    val user = userDoc.toObject(User::class.java)  // I-convert sa User object
                    dispatchUserData(activity, user)              // Ipadala ang user data sa tamang activity
                } else {
                    Log.e("Firestore", "User document does not exist.")   // Log error kapag wala
                    handleError(activity, Exception("User document not found"))  // Tawagin ang error handler
                }
            }
            .addOnFailureListener { e ->
                handleError(activity, e)                           // Tawagin error handler sa failure
            }
    }

    // 🔹 Ipadala ang user data sa tamang activity depende sa uri nito
    private fun dispatchUserData(activity: Activity, user: User?) {
        user?.let {
            when (activity) {
                is SignInActivity -> activity.signInSuccess(it)                // Tawagin success sa SignInActivity
                is MainActivity -> activity.updateNavigationUserDetails(it)    // I-update ang user details sa MainActivity
                is MyProfileActivity -> activity.setUserDataInUI(it)           // I-set ang user data sa UI ng MyProfileActivity
                is DoctorDashboardActivity -> activity.updateNavigationUserDetails(it)  // I-update sa DoctorDashboard
            }
        }
    }

    // 🔹 Centralized na error handler para sa pag-load ng data
    private fun handleError(activity: Activity, e: Exception) {
        Log.e(activity.javaClass.simpleName, "Error loading user data", e)     // I-log ang error
        when (activity) {
            is SignInActivity,
            is MainActivity,
            is MyProfileActivity,
            is DoctorDashboardActivity -> activity.hideProgressDialog()          // Itago loading dialog sa mga activity na ito
        }
    }

    // 🔹 I-update ang user profile data sa Firestore
    fun updateUserProfileData(activity: MyProfileActivity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID())
            .update(userHashMap)                          // I-update ang dokumento gamit ang HashMap ng bagong data
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "Profile updated successfully")  // Log success
                activity.showProccedSnacBar("Profile updated successfully")            // Ipakita snackbar na successful
                activity.profileUpdateSuccess()                                        // Tawagin success method sa activity
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()                 // Itago loading dialog sa failure
                Log.e(activity.javaClass.simpleName, "Error while updating profile", e)  // I-log ang error
            }
    }

    // 🔹 Kuhanin ang role ng kasalukuyang user mula sa Firestore
    fun getCurrentUserRole(onRoleFetched: (String) -> Unit) {
        val currentUserID = getCurrentUserID()               // Kunin ang kasalukuyang user ID
        if (currentUserID.isNotEmpty()) {
            mFireStore.collection(Constants.USERS)
                .document(currentUserID)
                .get()                                       // Kuhanin ang dokumento ng user
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: ""  // Kunin ang "role" field o empty string
                    onRoleFetched(role)                          // Ibalik ang role gamit ang callback
                }
                .addOnFailureListener {
                    onRoleFetched("")                            // Ibalik empty string kung may error
                }
        } else {
            onRoleFetched("")                                   // Walang naka-login na user, ibalik empty string
        }
    }
}
