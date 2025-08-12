package com.example.mediconnect.doctor_adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.databinding.ItemFeedbackBinding
import com.example.mediconnect.models.FeedbackModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter para ipakita ang listahan ng feedback sa RecyclerView
class Doctor_feedbackAdapter(private val feedbackList: List<FeedbackModel>) :
    RecyclerView.Adapter<Doctor_feedbackAdapter.FeedbackViewHolder>() {

    // ViewHolder class na nagho-hold ng view binding sa bawat item
    inner class FeedbackViewHolder(val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Gumagawa ng bagong ViewHolder at ini-inflate ang layout para sa bawat item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    // Binubuo at nilalagay ang data sa bawat ViewHolder base sa posisyon
    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]  // Kunin ang feedback sa current position

        // Itakda ang rating value sa RatingBar
        holder.binding.ratingBar.rating = feedback.rating

        // Itakda ang comment at pangalan ng pasyente (o user ID)
        holder.binding.textComment.text = feedback.comment
        holder.binding.textUserId.text = "User ID: ${feedback.patientName}"

        // Kung may timestamp, i-format ito bilang petsa at oras
        feedback.timestamp?.let {
            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())  // Format ng petsa
            val dateString = sdf.format(Date(it))  // I-format ang timestamp bilang string

            // NOTE: Walang ginagawa dito sa dateString pero pwede itong ipakita sa UI kung gusto
        } ?: run {
            // Kapag walang timestamp, walang gagawin dito
        }
    }

    // Ibalik ang bilang ng mga feedback sa list
    override fun getItemCount(): Int = feedbackList.size
}
