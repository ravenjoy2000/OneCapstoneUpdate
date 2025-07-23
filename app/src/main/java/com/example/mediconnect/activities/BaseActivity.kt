package com.example.mediconnect.activities

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity() {

    // Flag for double back to exit
    private var doubleBackToExitPressedOnce = false

    // Progress dialog instance
    private lateinit var mProgressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_base)
    }

    /* ========================= 🔄 Progress Dialog ========================= */

    /** Show loading dialog with custom text */
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_progress)
        mProgressDialog.findViewById<TextView>(R.id.tv_progress_text).text = text
        mProgressDialog.show()
    }

    /** Hide the loading dialog */
    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    /* ========================= 🔙 Double Back to Exit ========================= */

    /** Press back twice within 2 seconds to exit */
    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            resources.getString(R.string.please_click_back_again_to_exit),
            Toast.LENGTH_SHORT
        ).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    /* ========================= 📢 Snackbars ========================= */

    /** Show red snackbar for error messages */
    fun showErrorSnackBar(message: String) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        snackBar.view.setBackgroundColor(resources.getColor(R.color.snackbar_error_color))
        snackBar.show()
    }

    /** Show green snackbar for success messages */
    fun showProccedSnacBar(message: String) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        snackBar.view.setBackgroundColor(resources.getColor(R.color.snackbar_proceed_color))
        snackBar.show()
    }

    /* ========================= 🍞 Custom Toast ========================= */

    /** Show custom toast using custom layout */
    fun showCustomToast(message: String) {
        val view = layoutInflater.inflate(R.layout.custome_toast, null)
        view.findViewById<TextView>(R.id.tv_toast_message).text = message

        Toast(applicationContext).apply {
            duration = Toast.LENGTH_LONG
            this.view = view
            show()
        }
    }

    /* ========================= 🔐 Firebase ========================= */

    /** Return the UID of the currently logged-in Firebase user */
    fun getCurrentUserID(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}
