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

    private lateinit var ratingBar: RatingBar
    private lateinit var feedbackEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var feedbackResultTextView: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var existingFeedbackId: String? = null  // Track if feedback exists

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patent_feedback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        ratingBar = findViewById(R.id.ratingBar)
        feedbackEditText = findViewById(R.id.et_feedback)
        submitButton = findViewById(R.id.btn_submit_feedback)
        feedbackResultTextView = findViewById(R.id.tv_feedback_result)

        setupActionBar()

        checkExistingFeedback()

        submitButton.setOnClickListener {
            if (existingFeedbackId != null) {
                // Feedback exists, ask user if they want to edit
                showEditDialog()
            } else {
                // No previous feedback, submit normally
                submitFeedback()
            }
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_patient_feedback)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)
            title = "Patient Feedback"
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkExistingFeedback() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("patient_feedback")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    existingFeedbackId = doc.id
                    val rating = doc.getDouble("rating") ?: 0.0
                    val feedback = doc.getString("feedback") ?: ""
                    val timestamp = doc.getLong("timestamp")

                    // Format the timestamp
                    val formattedTime = timestamp?.let {
                        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "Unknown date"

                    // Display existing feedback
                    ratingBar.rating = rating.toFloat()
                    feedbackEditText.setText(feedback)

                    val stars = "★".repeat(rating.toInt()) + "☆".repeat(5 - rating.toInt())
                    feedbackResultTextView.text = """
                    You rated: $stars
                    Date: $formattedTime
                    
                    Your feedback:
                    "$feedback"
                """.trimIndent()
                    feedbackResultTextView.visibility = View.VISIBLE

                    // Disable editing initially
                    ratingBar.isEnabled = false
                    feedbackEditText.isEnabled = false
                }
            }
    }


    private fun showEditDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Feedback?")
            .setMessage("You've already submitted feedback. Do you want to change it?")
            .setPositiveButton("Yes") { dialog, _ ->
                ratingBar.isEnabled = true
                feedbackEditText.isEnabled = true
                submitButton.text = "Update Feedback"
                dialog.dismiss()
                // Now user can edit and re-click to update
                submitButton.setOnClickListener {
                    updateFeedback()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun submitFeedback() {
        val rating = ratingBar.rating
        val feedback = feedbackEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (userId == null || feedback.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val feedbackData = hashMapOf(
            "userId" to userId,
            "rating" to rating,
            "feedback" to feedback,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("patient_feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFeedback() {
        val rating = ratingBar.rating
        val feedback = feedbackEditText.text.toString().trim()
        val feedbackId = existingFeedbackId ?: return

        if (feedback.isEmpty()) {
            Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "rating" to rating,
            "feedback" to feedback,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("patient_feedback")
            .document(feedbackId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Feedback updated!", Toast.LENGTH_SHORT).show()
                recreate() // reload to lock the fields again
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}