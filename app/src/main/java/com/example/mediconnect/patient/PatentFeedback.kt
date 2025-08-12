package com.example.mediconnect.patient

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mediconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatentFeedback : AppCompatActivity() {

    // Declare variables para sa UI components
    private lateinit var ratingBar: RatingBar
    private lateinit var feedbackEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var feedbackResultTextView: TextView

    // Declare Firebase Firestore at Auth instance
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Variable para subaybayan kung may existing feedback na
    private var existingFeedbackId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patent_feedback)  // I-set ang layout file

        // Itago ang status bar depende sa Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Kunin ang Firestore instance
        db = FirebaseFirestore.getInstance()
        // Kunin ang FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // I-bind ang mga UI elements sa variables
        ratingBar = findViewById(R.id.ratingBar)
        feedbackEditText = findViewById(R.id.et_feedback)
        submitButton = findViewById(R.id.btn_submit_feedback)
        feedbackResultTextView = findViewById(R.id.tv_feedback_result)

        setupActionBar()     // I-setup ang toolbar

        checkExistingFeedback()  // Tingnan kung may existing feedback na

        // Kapag pinindot ang submit button
        submitButton.setOnClickListener {
            if (existingFeedbackId != null) {
                // Kung may existing feedback, tanungin kung gusto i-edit
                showEditDialog()
            } else {
                // Wala pang feedback, normal na submit
                submitFeedback()
            }
        }
    }

    // I-setup ang custom action bar na may back button
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_patient_feedback)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)     // Ipakita ang back arrow
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24) // Icon ng back arrow
            title = "Patient Feedback"          // Title ng toolbar
        }

        // Kapag pinindot ang back arrow, bumalik sa previous screen
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // Tingnan sa Firestore kung may feedback na para sa current user
    private fun checkExistingFeedback() {
        val userId = auth.currentUser?.uid ?: return   // Kunin ang userId, kung wala, exit

        db.collection("patient_feedback")
            .whereEqualTo("userId", userId)  // Hanapin ang feedback para sa user na ito
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    existingFeedbackId = doc.id    // I-save ang dokumento ID
                    val rating = doc.getDouble("rating") ?: 0.0
                    val feedback = doc.getString("feedback") ?: ""
                    val timestamp = doc.getLong("timestamp")

                    // I-format ang petsa gamit ang timestamp
                    val formattedTime = timestamp?.let {
                        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "Unknown date"

                    // Ipakita ang existing rating sa rating bar
                    ratingBar.rating = rating.toFloat()
                    // I-set ang existing feedback sa edit text
                    feedbackEditText.setText(feedback)

                    // Ipakita ang stars (★) batay sa rating, at empty stars (☆) para kulang
                    val stars = "★".repeat(rating.toInt()) + "☆".repeat(5 - rating.toInt())
                    feedbackResultTextView.text = """
                    You rated: $stars
                    Date: $formattedTime
                    
                    Your feedback:
                    "$feedback"
                """.trimIndent()
                    feedbackResultTextView.visibility = View.VISIBLE  // Ipakita ang feedback text view

                    // I-disable muna ang editing para hindi mabago agad
                    ratingBar.isEnabled = false
                    feedbackEditText.isEnabled = false
                }
            }
    }

    // Ipakita ang dialog kung gusto bang i-edit ang existing feedback
    private fun showEditDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Feedback?")
            .setMessage("You've already submitted feedback. Do you want to change it?")
            .setPositiveButton("Yes") { dialog, _ ->
                // I-enable ang rating bar at edit text para makapag-edit
                ratingBar.isEnabled = true
                feedbackEditText.isEnabled = true
                submitButton.text = "Update Feedback"  // Palitan ang button text
                dialog.dismiss()
                // I-override ang click listener para mag-update ng feedback
                submitButton.setOnClickListener {
                    updateFeedback()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()  // Isara ang dialog kung ayaw mag-edit
            }
            .show()
    }

    // Submit ng bagong feedback sa Firestore
    private fun submitFeedback() {
        val rating = ratingBar.rating
        val feedback = feedbackEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        // Kung walang user o walang feedback na na-input, magpakita ng error
        if (userId == null || feedback.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Ihanda ang data para i-save
        val feedbackData = hashMapOf(
            "userId" to userId,
            "rating" to rating,
            "feedback" to feedback,
            "timestamp" to System.currentTimeMillis()
        )

        // I-save sa Firestore collection "patient_feedback"
        db.collection("patient_feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                recreate()  // I-reload ang activity para ma-refresh UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // I-update ang existing feedback sa Firestore
    private fun updateFeedback() {
        val rating = ratingBar.rating
        val feedback = feedbackEditText.text.toString().trim()
        val feedbackId = existingFeedbackId ?: return  // Exit kung walang feedback ID

        // Kung walang feedback na nilagay, ipakita error
        if (feedback.isEmpty()) {
            Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            return
        }

        // Ihanda ang updated data
        val updateData = mapOf(
            "rating" to rating,
            "feedback" to feedback,
            "timestamp" to System.currentTimeMillis()
        )

        // I-update ang dokumento gamit ang feedback ID
        db.collection("patient_feedback")
            .document(feedbackId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Feedback updated!", Toast.LENGTH_SHORT).show()
                recreate()  // I-reload ang activity para ma-refresh at ma-lock uli ang fields
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
