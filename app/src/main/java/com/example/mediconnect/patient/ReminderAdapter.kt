package com.example.mediconnect.patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Reminder

class ReminderAdapter (private var reminders: List<Reminder>) :
    RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDrugName: TextView = itemView.findViewById(R.id.tvDrugName)
        val tvDosage: TextView = itemView.findViewById(R.id.tvDosage)
        val tvNextReminder: TextView = itemView.findViewById(R.id.tvNextReminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.tvDrugName.text = reminder.drugName
        holder.tvDosage.text = "${reminder.dosage} - ${reminder.frequency}"
        holder.tvNextReminder.text = "Next: ${reminder.nextReminder}"
    }

    override fun getItemCount(): Int = reminders.size

    fun updateList(newList: List<Reminder>) {
        reminders = newList
        notifyDataSetChanged()
    }
}