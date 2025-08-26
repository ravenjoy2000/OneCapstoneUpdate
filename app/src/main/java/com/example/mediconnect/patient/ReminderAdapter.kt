package com.example.mediconnect.patient

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Reminder

class ReminderAdapter(
    private val reminderList: MutableList<Reminder>,
    private val onMarkTaken: (Reminder) -> Unit,
    private val onCancelReminder: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedDetails: TextView = itemView.findViewById(R.id.tvMedDetails)
        val tvMedTimes: TextView = itemView.findViewById(R.id.tvMedTimes)
        val btnMarkTaken: Button = itemView.findViewById(R.id.btnMarkTaken)
        val btnCancelReminder: Button = itemView.findViewById(R.id.btnCancelReminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminderList[position]

        // Medicine details
        holder.tvMedDetails.text =
            "${reminder.name} - ${reminder.dosage} - ${reminder.frequency}"

        // Reminder times
        holder.tvMedTimes.text = "⏰ ${reminder.times}"

        // Button logic
        when {
            reminder.isTaken() -> {
                holder.btnMarkTaken.text = "✅ Taken"
                holder.btnMarkTaken.isEnabled = false
                holder.btnMarkTaken.setBackgroundColor(Color.GRAY)
            }
            reminder.isMissed() -> {
                holder.btnMarkTaken.text = "❌ Missed"
                holder.btnMarkTaken.isEnabled = false
                holder.btnMarkTaken.setBackgroundColor(Color.RED)
            }
            reminder.isPending() -> {
                holder.btnMarkTaken.text = "Mark as Taken"
                holder.btnMarkTaken.isEnabled = true
                holder.btnMarkTaken.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.teal_700)
                )
                holder.btnMarkTaken.setOnClickListener {
                    onMarkTaken(reminder)
                }
            }
        }

        // Cancel Reminder button
        holder.btnCancelReminder.setOnClickListener {
            onCancelReminder(reminder)
        }
    }

    override fun getItemCount() = reminderList.size

    // ✅ Helper to refresh list when updated
    fun updateList(newList: List<Reminder>) {
        reminderList.clear()
        reminderList.addAll(newList)
        notifyDataSetChanged()
    }
}
