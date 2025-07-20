// Package kung saan naka-save ang activity
package com.example.mediconnect.activities

// Mga import para sa Android UI at Firebase functionality
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

// Activity na nagha-handle ng Sign In process
class SignInActivity : BaseActivity() {

    // Lifecycle method – tinatawag kapag nagbubukas ang SignInActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Fullscreen layout support
        setContentView(R.layout.activity_sign_in) // Gamitin ang layout file na ito

        //================= REMOVE STATUS BAR (Full Screen Mode) ====================
        // Para sa mga lumang Android devices
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Para sa mga bagong Android devices (Android 11 pataas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }

        //================== END FULLSCREEN CONFIG ==========================

        setupActionbar() // Tawagin ang function para i-setup ang toolbar

        //===================== LOGIN BUTTON CLICK LISTENER ====================
        val btn_sign_in = findViewById<Button>(R.id.btn_sign_in)
        btn_sign_in.setOnClickListener {
            singInRegisteredUser() // Kapag pinindot ang login button, subukang mag-login
        }
        //=====================================================================
    }

    //===================== SETUP TOOLBAR / ACTION BAR ========================
    private fun setupActionbar() {
        val toolbar_for_sign_up_activity = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbar_for_sign_up_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true) // Enable back arrow
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24) // Custom back arrow icon
        }

        // Kapag pinindot ang back arrow, bumalik sa previous screen
        toolbar_for_sign_up_activity.setNavigationOnClickListener { onBackPressed() }
    }

    //===================== VALIDATE EMAIL & PASSWORD INPUT ========================
    private fun validateFormSigninActivity(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.") // Kung walang email
                false
            }

            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.") // Kung walang password
                false
            }

            else -> {
                true // Kung parehas may laman, magpatuloy
            }
        }
    }

    //===================== ACTUAL SIGN-IN LOGIC ========================
    private fun singInRegisteredUser() {

        // Kunin ang values sa EditText at tanggalin ang extra spaces
        val email: String = findViewById<EditText>(R.id.et_email).text.toString().trim { it <= ' ' }
        val password: String = findViewById<EditText>(R.id.et_password).text.toString().trim { it <= ' ' }

        // I-validate muna kung may laman ang inputs
        if (validateFormSigninActivity(email, password)) {

            // Ipakita ang loading dialog habang naglo-login
            showProgressDialog(resources.getString(R.string.please_wait))

            // Firebase sign-in gamit ang email at password
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    hideProgressDialog() // Alisin ang loading kahit success or fail

                    if (task.isSuccessful) {
                        // ✅ SUCCESS
                        val user = FirebaseAuth.getInstance().currentUser

                        // Magpakita ng success toast message
                        showCustomToast("You have successfully signed in as ${user?.email}")

                        // Lipat sa MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else {
                        // ❌ ERROR: Hanapin kung anong klase ng error ang nangyari
                        val errorMessage = when (val exception = task.exception) {
                            is FirebaseAuthInvalidUserException -> "Invalid email or password"
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                            else -> "Authentication failed. Please try again."
                        }

                        // Ipakita ang error sa Snackbar
                        showErrorSnackBar(errorMessage)
                    }
                }
        }
    }

    //====================== CALLED FROM FireStoreClass IF USER DATA IS LOADED ========================
    fun singInSuccess(user: User) {
        hideProgressDialog() // I-hide ang loading dialog
        startActivity(Intent(this, MainActivity::class.java)) // Lipat sa MainActivity
        finish() // Tapusin ang SignInActivity para hindi na bumalik pag-back
    }
}
