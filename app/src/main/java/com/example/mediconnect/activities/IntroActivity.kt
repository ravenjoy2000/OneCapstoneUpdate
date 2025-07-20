package com.example.mediconnect.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R

class IntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_intro)

        // ----- This one for Removing the Head icon------

        window.setFlags(  // for old device
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // for new device
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
        //------This is the End-----------


        //------------This for Button SignUp in Intro------------------
       val btn_for_sign_up = findViewById<Button>(R.id.btn_sign_up_intro)
        btn_for_sign_up.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        //---------------End------------------------------------

        //----------------This For Button For SignIn in Intro---------------
        val btn_for_sign_in = findViewById<Button>(R.id.btn_sign_in_intro)
        btn_for_sign_in.setOnClickListener{
            startActivity(Intent(this, SignInActivity::class.java))
        }
        //------------------End---------------------------------------------



    }
}