package com.example.mediconnect.doctor

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediconnect.R
import com.example.mediconnect.databinding.ActivityDoctorFeedbackBinding
import com.example.mediconnect.models.FeedbackModel
import com.google.firebase.firestore.FirebaseFirestore

class DoctorFeedback : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorFeedbackBinding
    private val feedbackList = mutableListOf<FeedbackModel>()
    private lateinit var adapter: Doctor_feedbackAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setFullscreenMode()
        setupRecyclerView()
        loadFeedbackFromFirestore()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_patient_feedback)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun setupRecyclerView() {
        adapter = Doctor_feedbackAdapter(feedbackList)
        binding.recyclerViewFeedback.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFeedback.adapter = adapter
    }

    private fun loadFeedbackFromFirestore() {
        firestore.collection("patient_feedback")
            .get()
            .addOnSuccessListener { documents ->
                feedbackList.clear()

                var totalRating = 0f
                var feedbackCount = 0
                val starCounts = IntArray(5) { 0 }

                for (doc in documents) {
                    val rating = doc.getDouble("rating")?.toFloat() ?: continue
                    val comment = doc.getString("feedback") ?: "No comment"
                    val patientName = doc.getString("userId") ?: "Anonymous"

                    feedbackList.add(
                        FeedbackModel(
                            rating = rating,
                            comment = comment,
                            patientName = patientName
                        )
                    )

                    totalRating += rating
                    feedbackCount++

                    val starIndex = (rating.toInt().coerceIn(1, 5)) - 1
                    starCounts[starIndex]++
                }

                adapter.notifyDataSetChanged()

                if (feedbackCount > 0) {
                    val average = totalRating / feedbackCount
                    binding.textAverageRating.text = "Average Rating: %.1f ★".format(average)
                    binding.ratingBarAverage.rating = average
                    binding.textTotalFeedbacks.text = "Total Feedbacks: $feedbackCount"

                    updateStarBar(binding.progress5, binding.label5, starCounts[4], feedbackCount)
                    updateStarBar(binding.progress4, binding.label4, starCounts[3], feedbackCount)
                    updateStarBar(binding.progress3, binding.label3, starCounts[2], feedbackCount)
                    updateStarBar(binding.progress2, binding.label2, starCounts[1], feedbackCount)
                    updateStarBar(binding.progress1, binding.label1, starCounts[0], feedbackCount)
                } else {
                    showNoFeedbackUI()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error loading feedback: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStarBar(progressBar: ProgressBar, label: TextView, starCount: Int, total: Int) {
        val percentage = if (total > 0) (starCount * 100) / total else 0
        progressBar.progress = percentage
        label.text = "$starCount"
    }

    private fun showNoFeedbackUI() {
        binding.textAverageRating.text = "No ratings yet"
        binding.ratingBarAverage.rating = 0f
        binding.textTotalFeedbacks.text = "Total Feedbacks: 0"
        listOf(binding.progress1, binding.progress2, binding.progress3, binding.progress4, binding.progress5).forEach {
            it.progress = 0
        }
        listOf(binding.label1, binding.label2, binding.label3, binding.label4, binding.label5).forEach {
            it.text = "0"
        }
    }
}
