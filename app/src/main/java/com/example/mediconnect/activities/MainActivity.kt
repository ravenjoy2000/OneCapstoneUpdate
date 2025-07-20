// Package kung saan nakalagay ang MainActivity
package com.example.mediconnect.activities

// Import ng mga Android at Firebase tools na kailangan
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.example.mediconnect.R
import com.example.mediconnect.firebase.FireStoreClass
import com.example.mediconnect.models.User
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

// MainActivity na gumagamit ng navigation drawer, toolbar, at user info display
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Lifecycle method: tinatawag kapag nagbubukas ang activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge() // Ginagawang fullscreen o edge-to-edge ang layout

        setContentView(R.layout.activity_main) // I-set ang layout file na gagamitin

        setupActionBar() // Tawagin ang function para isetup ang Toolbar with drawer

        val navigationView = findViewById<NavigationView>(R.id.nav_view) // Kunin ang NavigationView

        navigationView.setNavigationItemSelectedListener(this) // I-set kung sino ang magha-handle ng menu click (ito ay 'this')

        backPressDispatcher() // Tawagin ang function na magha-handle ng custom back behavior (double back to exit)

        FireStoreClass().singInUser(this) // Kumuha ng user data at i-update ang UI
    }

    // ➤ Setup para sa toolbar at drawer toggle icon (hamburger menu)
    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_main_activity)) // Gamitin ang toolbar bilang ActionBar

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Ipakita ang ← (back/up) button

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu) // Palitan ang icon ng drawer menu

        val toolbar_main_activity = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main_activity)

        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer() // Tawagin ang function para buksan/sarhan ang drawer
        }
    }

    // ➤ Buksan o isara ang drawer depende sa current state
    private fun toggleDrawer() {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START) // Kung bukas, isara
        } else {
            drawerLayout.openDrawer(GravityCompat.START) // Kung sarado, buksan
        }
    }

    // ➤ Custom na behavior kapag pinindot ang back button (gamit ang back press dispatcher)
    private fun backPressDispatcher() {
        onBackPressedDispatcher.addCallback(this) {
            val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)

            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START) // Kung bukas ang drawer, isara ito
            } else {
                doubleBackToExit() // Kung hindi bukas, gamitin ang double back exit
            }
        }
    }

    // ➤ Function na nagha-handle kapag may pinili sa navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)

        when(item.itemId) {
            R.id.nav_my_profile -> {
                showCustomToast("My Profile") // Ipakita ang toast kapag pinindot ang My Profile
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut() // I-log out ang user

                val intent = Intent(this, IntroActivity::class.java) // Bumalik sa Intro screen

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) // Linisin ang backstack

                startActivity(intent) // Buksan ang IntroActivity
                finish() // I-close ang MainActivity
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START) // Isara ang drawer pagkatapos mag-click

        return true // Ibalik ang true para sabihing na-handle na ang click
    }

    // ➤ Ipakita ang user profile (image at name) sa navigation drawer
    fun updateNavigationUserDetails(user: User) {
        Glide.with(this) // Gamitin ang Glide para mag-load ng image
            .load(user.image) // Kunin ang image URL
            .centerCrop() // I-center at crop ang image para hindi ma-stretch
            .placeholder(R.drawable.ic_user_place_holder) // Placeholder habang naglo-load
            .into(findViewById(R.id.iv_user_image)) // I-load sa ImageView na may id = iv_user_image

        val tv_usernames = findViewById<TextView>(R.id.tv_username) // Kunin ang TextView para sa pangalan
        tv_usernames.text = user.name // I-set ang text ng username
    }
}
