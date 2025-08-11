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

class FireStoreClass : BaseActivity() {

    private val mFireStore = FirebaseFirestore.getInstance()

    // 🔹 Register new user
    fun registerUser(activity: SignUpActivity, @SuppressLint("RestrictedApi") userInfo: User) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error writing document", e)
            }
    }

    // 🔹 Load current user's data from Firestore

    fun loadUserData(activity: Activity) {
        val currentUserId = getCurrentUserID()

        mFireStore.collection(Constants.USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    dispatchUserData(activity, user)
                } else {
                    Log.e("Firestore", "User document does not exist.")
                    handleError(activity, Exception("User document not found"))
                }
            }
            .addOnFailureListener { e ->
                handleError(activity, e)
            }
    }

    // 🔹 Dispatch user data to the correct activity
    private fun dispatchUserData(activity: Activity, user: User?) {
        user?.let {
            when (activity) {
                is SignInActivity -> activity.signInSuccess(it)
                is MainActivity -> activity.updateNavigationUserDetails(it)
                is MyProfileActivity -> activity.setUserDataInUI(it)
                is DoctorDashboardActivity -> activity.updateNavigationUserDetails(it)
            }
        }
    }


    // 🔹 Centralized error handler for loading data
    private fun handleError(activity: Activity, e: Exception) {
        Log.e(activity.javaClass.simpleName, "Error loading user data", e)
        when (activity) {
            is SignInActivity,
            is MainActivity,
            is MyProfileActivity,
            is DoctorDashboardActivity -> activity.hideProgressDialog()
        }
    }

    // 🔹 Update user profile data
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

    // 🔹 Fetch current user's role from Firestore
    fun getCurrentUserRole(onRoleFetched: (String) -> Unit) {
        val currentUserID = getCurrentUserID()
        if (currentUserID.isNotEmpty()) {
            mFireStore.collection(Constants.USERS)
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
}
