// Package kung saan naka-save ang Firestore helper class
package com.example.mediconnect.firebase

// Mga kailangan para sa Firebase, log, at activity operations
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.MainActivity
import com.example.mediconnect.activities.SignInActivity
import com.example.mediconnect.activities.SignUpActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Class na magha-handle ng Firestore-related operations
class FireStoreClass : BaseActivity() {

    // Gumagawa ng Firestore instance para magamit sa buong class na ito
    private val mFireStore = FirebaseFirestore.getInstance()

    // =======================================================================================
    // ⬇⬇⬇ FUNCTION PARA SA REGISTRATION NG USER SA FIRESTORE DATABASE ⬇⬇⬇
    // =======================================================================================

    /**
     * Nagre-register ng bagong user sa Firestore Database.
     * Tinatawag ito pagkatapos makapag-sign up ang user gamit ang Firebase Authentication.
     */
    fun registerUser(activity: SignUpActivity, @SuppressLint("RestrictedApi") userInfo: User) {

        mFireStore.collection(Constants.USERS) // Punta sa "users" collection sa Firestore

            .document(getCurrentUserID()) // Gumawa o i-access ang document na may UID ng user

            .set(userInfo, SetOptions.merge()) // I-save ang userInfo; `merge()` para hindi mabura ang existing data

            .addOnSuccessListener {
                // Kapag success, ipaalam sa SignUpActivity na successful ang registration
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                // Kapag nagkaroon ng error sa pag-save
                activity.hideProgressDialog() // Itago ang loading dialog
                Log.e( // Mag-print ng error sa logcat
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    } // Wakas ng registerUser()

    // =======================================================================================
    // ⬇⬇⬇ FUNCTION PARA MAKUHA ANG USER INFO PAGKATAPOS MAG-SIGN IN ⬇⬇⬇
    // =======================================================================================

    fun singInUser(activity: Activity) {

        mFireStore.collection(Constants.USERS) // Punta sa "users" collection

            .document(getCurrentUserID()) // Hanapin ang document gamit ang UID ng naka-login na user

            .get() // Kuhanin ang laman ng document (user info)

            .addOnSuccessListener { document ->
                // Kapag nakuha ang data nang tama

                val loggedInUser = document.toObject(User::class.java) // I-convert ang document sa User object

                when(activity){
                    is SignInActivity -> {
                        // Kung galing sa SignInActivity
                        if (loggedInUser != null) {
                            // Ibalik ang user data sa SignInActivity
                            activity.singInSuccess(loggedInUser)
                        }
                    }
                    is MainActivity -> {
                        // Kung galing sa MainActivity, i-update ang UI (profile image & name)
                        activity.updateNavigationUserDetails(loggedInUser!!)
                    }
                }
            }

            .addOnFailureListener { e ->
                // Kung may error sa pagkuha ng data

                when(activity){
                    is SignInActivity -> {
                        activity.hideProgressDialog() // Itago ang loading sa SignInActivity
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog() // Itago ang loading sa MainActivity
                    }
                }

                Log.e(
                    activity.javaClass.simpleName, // Pangalan ng class kung saan nag-error
                    "Error writing document", // Error message
                    e // Exception details
                )
            }
    } // Wakas ng singInUser()

    // =======================================================================================
    // ⬇⬇⬇ GET CURRENT USER ID FUNCTION — ito ay nasa BaseActivity na daw kaya naka-comment ⬇⬇⬇
    // =======================================================================================

//    fun getCurrentUserID(): String {
//        return FirebaseAuth.getInstance().currentUser!!.uid
//    }

} // Wakas ng FireStoreClass
