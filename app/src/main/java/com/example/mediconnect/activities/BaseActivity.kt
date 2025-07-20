// Package kung saan naka-save ang activity
package com.example.mediconnect.activities

// Mga import para sa UI at Firebase functionality
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mediconnect.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import android.os.Handler

// BaseActivity class na magagamit bilang parent ng ibang activities para sa reusable functions
open class BaseActivity : AppCompatActivity() {

    // Para sa double back press (para hindi agad mag-exit ang app nang hindi sinasadya)
    private var doubleBackToExitPressedOnce = false

    // Progress dialog para sa loading screen
    private lateinit var mProgressDialog: Dialog

    // Lifecycle method: tinatawag kapag nag-start ang activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // I-enable ang edge-to-edge screen layout
        enableEdgeToEdge()

        // I-set ang layout ng activity sa activity_base.xml
        setContentView(R.layout.activity_base)
    }

    /*============================= Loading Screen Section =============================*/

    // Function na nagpapakita ng loading dialog
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this) // Gumawa ng bagong dialog
        mProgressDialog.setContentView(R.layout.dialog_progress) // I-set ang layout ng dialog
        mProgressDialog.findViewById<TextView>(R.id.tv_progress_text).text = text // I-set ang text ng loading
        mProgressDialog.show() // Ipakita ang dialog
    }

    // Function para itago ang progress dialog
    fun hideProgressDialog() {
        mProgressDialog.dismiss() // Isara ang dialog
    }

    /*============================= Double Back to Exit Section =============================*/

    // Function na nagpi-prevent ng aksidenteng pag-exit ng app
    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            // Kapag naka-double back na, i-close na ang activity
            super.onBackPressed()
            return
        }

        // Kung first press pa lang, ipakita ang Toast message
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            resources.getString(R.string.please_click_back_again_to_exit),
            Toast.LENGTH_SHORT
        ).show()

        // Pagkatapos ng 2 seconds, ibalik ang flag sa false
        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    /*============================= Snackbar Section =============================*/

    // Function na nagpapakita ng error snackbar (red background)
    fun showErrorSnackBar(message: String) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(resources.getColor(R.color.snackbar_error_color))
        snackBar.show()
    }

    // Function na nagpapakita ng proceed snackbar (green background o depende sa kulay mo)
    fun showProccedSnacBar(message: String) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(resources.getColor(R.color.snackbar_proceed_color))
        snackBar.show()
    }

    /*============================= Custom Toast Section =============================*/

    // Function na nagpapakita ng custom toast (gamit ang custom layout)
    fun showCustomToast(message: String) {
        val layoutInflater = layoutInflater
        val view = layoutInflater.inflate(R.layout.custome_toast, null) // Gamit ang iyong custom layout

        val toastText = view.findViewById<TextView>(R.id.tv_toast_message) // Kunin ang TextView ng message
        toastText.text = message // I-set ang mensahe

        val toast = Toast(applicationContext) // Gumawa ng bagong toast
        toast.duration = Toast.LENGTH_LONG
        toast.view = view // Gamitin ang custom layout
        toast.show() // Ipakita ang toast
    }

    /*============================= Firebase Section =============================*/

    // Function na kinukuha ang UID ng currently logged-in Firebase user
    fun getCurrentUserID(): String {
        var currentUser = FirebaseAuth.getInstance().currentUser // Kunin ang kasalukuyang naka-login
        var currentUserID = "" // Mag-assign ng default value

        if (currentUser != null) {
            currentUserID = currentUser.uid // Kung may naka-login, kunin ang UID
        }

        return currentUserID // Ibalik ang UID
    }
}
