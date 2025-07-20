// Package kung saan nakalagay ang SplashActivity
package com.example.mediconnect.activities

// Mga import para sa UI, window control, at font
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass

// SplashActivity class na siyang unang screen pag binuksan ang app
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout (para sa fullscreen na experience)
        enableEdgeToEdge()

        // I-set ang UI layout file na gagamitin
        setContentView(R.layout.activity_splash)

        // ================= REMOVE STATUS BAR / FULLSCREEN =======================
        // Para sa older Android versions (bago pa ang Android 11)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Para sa Android 11 pataas, hide ang status bar gamit WindowInsets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
        // ========================================================================


        // ================= CUSTOM FONT SA APP NAME ===============================
        val tv_app_name = findViewById<TextView>(R.id.tv_app_name) // Hanapin ang TextView na may ID na tv_app_name
        val typFace: Typeface = Typeface.createFromAsset(assets, "Billy Bounce.otf") // Load ang custom font galing sa assets folder
        tv_app_name.typeface = typFace // I-apply ang font sa TextView
        // =========================================================================


        // ============ SPLASH TIMER + AUTO LOGIN CHECK ============================
        android.os.Handler().postDelayed({

            // Gamitin ang FireStoreClass para kunin ang current user ID (UID ng naka-login)
            val currentUserID = FireStoreClass().getCurrentUserID()

            // ✅ Kung hindi empty, ibig sabihin may naka-login → punta sa MainActivity
            if (currentUserID.isNotEmpty()) {
                startActivity(Intent(this, MainActivity::class.java)) // Open MainActivity
            } else {
                // ❌ Kung walang naka-login → punta sa IntroActivity (Sign In / Sign Up)
                startActivity(Intent(this, IntroActivity::class.java)) // Open IntroActivity
            }

            // Tapusin na ang SplashActivity para hindi na ito balikan kapag nag-back press
            finish()

        }, 2500) // Delay ng 2.5 seconds (2500 milliseconds)
        // ==========================================================================

    }
}
