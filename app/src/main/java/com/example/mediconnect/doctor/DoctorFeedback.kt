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
import com.example.mediconnect.doctor_adapter.Doctor_feedbackAdapter
import com.example.mediconnect.models.FeedbackModel
import com.google.firebase.firestore.FirebaseFirestore

class DoctorFeedback : AppCompatActivity() {

    // Binding para sa activity layout gamit ViewBinding
    private lateinit var binding: ActivityDoctorFeedbackBinding

    // Listahan ng feedback data na ipapasa sa adapter
    private val feedbackList = mutableListOf<FeedbackModel>()

    // Adapter para sa RecyclerView ng feedback
    private lateinit var adapter: Doctor_feedbackAdapter

    // Firestore instance para kumuha ng data
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // I-inflate ang layout gamit ang ViewBinding
        binding = ActivityDoctorFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)  // I-set ang content view sa binding root

        setupActionBar()      // I-setup ang action bar (toolbar)
        setFullscreenMode()   // Gawing fullscreen ang activity (itago ang status bar)
        setupRecyclerView()   // I-setup ang RecyclerView kasama ang adapter at layout manager
        loadFeedbackFromFirestore()   // Kumuha ng feedback data mula Firestore
    }

    // I-setup ang action bar na may back button
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_patient_feedback)   // Kunin ang toolbar view
        setSupportActionBar(toolbar)    // I-set bilang support action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)                       // Ipakita ang back arrow
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)  // Palitan ang icon ng back arrow
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }  // I-handle ang back press ng toolbar
    }

    // Itago ang status bar para fullscreen mode
    private fun setFullscreenMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) // Itago status bar sa lahat ng versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())  // Para sa Android 11+
        }
    }

    // I-setup ang RecyclerView na may LinearLayoutManager at adapter
    private fun setupRecyclerView() {
        adapter = Doctor_feedbackAdapter(feedbackList)   // Gumawa ng adapter gamit ang feedback list
        binding.recyclerViewFeedback.layoutManager = LinearLayoutManager(this)  // Vertical list
        binding.recyclerViewFeedback.adapter = adapter   // I-set ang adapter sa RecyclerView
    }

    // Kunin ang feedback data mula Firestore collection "patient_feedback"
    private fun loadFeedbackFromFirestore() {
        firestore.collection("patient_feedback")
            .get()
            .addOnSuccessListener { documents ->
                feedbackList.clear()   // Linisin muna ang list bago punuin

                var totalRating = 0f       // Variable para sa total ng lahat ng ratings
                var feedbackCount = 0      // Bilang ng feedback entries
                val starCounts = IntArray(5) { 0 }   // Array para bilang ng bawat star rating (1-5)

                for (doc in documents) {
                    // Kunin ang rating, convert sa float; skip kung wala
                    val rating = doc.getDouble("rating")?.toFloat() ?: continue

                    // Kunin ang comment, default "No comment" kung wala
                    val comment = doc.getString("feedback") ?: "No comment"

                    // Kunin ang pangalan ng patient o "Anonymous" kung wala
                    val patientName = doc.getString("userId") ?: "Anonymous"

                    // Idagdag ang feedback sa list
                    feedbackList.add(
                        FeedbackModel(
                            rating = rating,
                            comment = comment,
                            patientName = patientName
                        )
                    )

                    totalRating += rating  // I-add ang rating sa total
                    feedbackCount++        // Dagdagan ang bilang ng feedback

                    // I-compute ang index para sa starCounts array (1 star -> index 0, 5 star -> index 4)
                    val starIndex = (rating.toInt().coerceIn(1, 5)) - 1
                    starCounts[starIndex]++    // Dagdagan ang count sa specific star
                }

                adapter.notifyDataSetChanged()   // Sabihin sa adapter na may bagong data

                if (feedbackCount > 0) {
                    val average = totalRating / feedbackCount  // Compute average rating

                    // Ipakita ang average rating bilang text at sa RatingBar
                    binding.textAverageRating.text = "Average Rating: %.1f ★".format(average)
                    binding.ratingBarAverage.rating = average

                    // Ipakita ang total feedback count
                    binding.textTotalFeedbacks.text = "Total Feedbacks: $feedbackCount"

                    // I-update ang bawat star progress bar at label para ipakita distribution
                    updateStarBar(binding.progress5, binding.label5, starCounts[4], feedbackCount)
                    updateStarBar(binding.progress4, binding.label4, starCounts[3], feedbackCount)
                    updateStarBar(binding.progress3, binding.label3, starCounts[2], feedbackCount)
                    updateStarBar(binding.progress2, binding.label2, starCounts[1], feedbackCount)
                    updateStarBar(binding.progress1, binding.label1, starCounts[0], feedbackCount)
                } else {
                    showNoFeedbackUI()  // Walang feedback, ipakita ang no feedback UI
                }
            }
            .addOnFailureListener { error ->
                // Kapag nag-error sa pagkuha ng data, ipakita ang Toast message
                Toast.makeText(this, "Error loading feedback: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // I-update ang progress bar at label ng star count base sa proportion ng feedback
    private fun updateStarBar(progressBar: ProgressBar, label: TextView, starCount: Int, total: Int) {
        val percentage = if (total > 0) (starCount * 100) / total else 0  // Compute percentage ng stars
        progressBar.progress = percentage   // I-set ang progress bar value
        label.text = "$starCount"           // Ipakita bilang ng stars sa label
    }

    // Ipakita ang UI kapag walang feedback entries
    private fun showNoFeedbackUI() {
        binding.textAverageRating.text = "No ratings yet"   // Text na walang rating
        binding.ratingBarAverage.rating = 0f                 // Walang rating sa RatingBar
        binding.textTotalFeedbacks.text = "Total Feedbacks: 0"  // Zero total feedbacks

        // I-reset lahat ng progress bars sa zero
        listOf(binding.progress1, binding.progress2, binding.progress3, binding.progress4, binding.progress5).forEach {
            it.progress = 0
        }

        // I-reset lahat ng labels sa zero
        listOf(binding.label1, binding.label2, binding.label3, binding.label4, binding.label5).forEach {
            it.text = "0"
        }
    }
}
