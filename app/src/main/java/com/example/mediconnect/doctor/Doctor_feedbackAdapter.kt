package com.example.mediconnect.doctor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.databinding.ItemFeedbackBinding
import com.example.mediconnect.models.FeedbackModel
import java.text.SimpleDateFormat
import java.util.*

class Doctor_feedbackAdapter(private val feedbackList: List<FeedbackModel>) :
    RecyclerView.Adapter<Doctor_feedbackAdapter.FeedbackViewHolder>() {

    inner class FeedbackViewHolder(val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]

        // Set the rating on the RatingBar
        holder.binding.ratingBar.rating = feedback.rating

        // Set the comment and patient name
        holder.binding.textComment.text = feedback.comment
        holder.binding.textUserId.text = "User ID: ${feedback.patientName}"

        // Format and display timestamp if available
        feedback.timestamp?.let {
            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val dateString = sdf.format(Date(it))
        } ?: run {
        }
    }


    override fun getItemCount(): Int = feedbackList.size
}
