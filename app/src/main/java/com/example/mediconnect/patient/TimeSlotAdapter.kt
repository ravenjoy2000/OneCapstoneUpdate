package com.example.mediconnect.patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeSlotAdapter(
    private val timeSlots: List<String>,
    private val bookedSlots: List<String>,
    private val hasActiveAppointment: Boolean,
    private val selectedDate: String,  // ✅ added
    private val onSlotSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val context = holder.itemView.context
        val timeSlot = timeSlots[position]
        holder.tvTimeSlot.text = timeSlot

        val isBooked = bookedSlots.contains(timeSlot)
        val isPastSlot = isPastTimeSlot(timeSlot)

        when {
            isPastSlot -> {
                holder.itemView.isEnabled = false
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.gray))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
            }
            isBooked -> {
                holder.itemView.isEnabled = false
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.red))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            }
            else -> {
                holder.itemView.isEnabled = !hasActiveAppointment
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.black))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
        }

        // Highlight selected
        if (selectedPosition == position && holder.itemView.isEnabled) {
            holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
        }

        holder.itemView.setOnClickListener {
            if (!holder.itemView.isEnabled) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onSlotSelected(timeSlot)
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    inner class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTimeSlot: TextView = itemView.findViewById(R.id.tv_slot_time)
        val slotIndicator: View = itemView.findViewById(R.id.slot_indicator)
    }

    private fun isPastTimeSlot(slotTime: String): Boolean {
        return try {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val selectedDateParsed = dateFormat.parse(selectedDate) ?: return true
            val slotTimeParsed = timeFormat.parse(slotTime) ?: return true

            val slotDateTime = Calendar.getInstance().apply {
                time = selectedDateParsed
                val timeOnly = Calendar.getInstance().apply { time = slotTimeParsed }
                set(Calendar.HOUR_OF_DAY, timeOnly.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeOnly.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = Calendar.getInstance()
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)

            now.after(slotDateTime)
        } catch (e: Exception) {
            true
        }
    }

}