// Package kung saan naka-save ang Firestore helper class
package com.example.mediconnect.firebase

// Mga kailangan para sa Firebase, log, at activity operations
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.activities.MainActivity
import com.example.mediconnect.activities.MyProfileActivity
import com.example.mediconnect.activities.SignInActivity
import com.example.mediconnect.activities.SignUpActivity
import com.example.mediconnect.models.User
import com.example.mediconnect.utils.Constants
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

    fun loadUserData(activity: Activity) {
        // Simulan ang pagkuha ng user data mula sa Firebase Firestore
        mFireStore.collection(Constants.USERS) // Punta sa "users" collection
            .document(getCurrentUserID()) // Hanapin ang specific na document gamit ang UID ng kasalukuyang naka-login na user
            .get() // Kuhanin ang buong laman ng document na 'yan (user data)
            .addOnSuccessListener { document ->
                // Kapag matagumpay ang pagkuha ng document

                val loggedInUser = document.toObject(User::class.java)
                // I-convert ang Firestore document sa User object na pwedeng gamitin sa app

                // Depende kung anong Activity ang tumawag sa function na ito:
                when(activity) {

                    is SignInActivity -> {
                        // Kung ang activity ay SignInActivity
                        if (loggedInUser != null) {
                            // Ibalik ang user data sa SignInActivity para makapag-proceed
                            activity.singInSuccess(loggedInUser)
                        }
                    }

                    is MainActivity -> {
                        // Kung MainActivity ang tumawag, i-update ang navigation drawer (profile image at name)
                        activity.updateNavigationUserDetails(loggedInUser!!)
                        // Ginamitan ng !! dahil sigurado tayo na hindi null ito
                    }

                    is MyProfileActivity -> {
                        // Placeholder para sa future feature: e.g. ipakita ang user data sa profile
                        activity.setUserDataInUI(loggedInUser!!)
                    }
                }
            }

            .addOnFailureListener { e ->
                // Kapag may error sa pagkuha ng data mula sa Firestore

                when(activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog() // Itago ang progress dialog
                    }

                    is MainActivity -> {
                        activity.hideProgressDialog() // Ganoon din sa MainActivity
                    }

                    // Pwede rin maglagay ng handling sa MyProfileActivity kung gusto mo
                }

                // I-log ang error message para sa debugging
                Log.e(
                    activity.javaClass.simpleName, // Pangalan ng Activity kung saan nagka-error
                    "Error writing document", // Custom error message
                    e // Actual error (exception object)
                )
            }
    } // End of loadUserData()








    //--------- Update ng information-----------------
    fun updateUserProfileData(activity: MyProfileActivity, userHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.USERS) // Punta sa "users" collection
            .document(getCurrentUserID()) // Hanapin ang specific na document gamit ang UID ng kasalukuyang naka-login na user
            .update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "Profile Data Updated Successfully!")
                activity.showProccedSnacBar("Profile updated successfully")
                activity.profileUpdateSuccess()
            }.addOnFailureListener {
                e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while updating profile",
                    e
                )
            }
    }
    //---------------------END-------------------------












    // =======================================================================================
    // ⬇⬇⬇ GET CURRENT USER ID FUNCTION — ito ay nasa BaseActivity na daw kaya naka-comment ⬇⬇⬇
    // =======================================================================================

//    fun getCurrentUserID(): String {
//        return FirebaseAuth.getInstance().currentUser!!.uid
//    }

} // Wakas ng FireStoreClass
