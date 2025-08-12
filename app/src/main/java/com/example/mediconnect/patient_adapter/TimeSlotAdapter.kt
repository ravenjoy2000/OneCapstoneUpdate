package com.example.mediconnect.patient_adapter
// 📦 Package kung saan nakalagay ang TimeSlotAdapter class

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
// 🔽 Mga import ng mga kailangang klase at utility

class TimeSlotAdapter(
    private val timeSlots: List<String>,       // 📅 Listahan ng mga available na time slots
    private val bookedSlots: List<String>,     // ❌ Mga slot na naka-book na
    private val hasActiveAppointment: Boolean, // 🔒 May active appointment na ba ang user (para i-disable selection)
    private val selectedDate: String,          // 📆 Napiling petsa para sa appointment (importanteng idagdag)
    private val onSlotSelected: (String) -> Unit  // 🖱️ Callback function kapag may piniling slot
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1  // 🔵 Track ng kasalukuyang napiling posisyon sa listahan

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        // 🔨 Gumagawa ng bagong ViewHolder gamit ang item_time_slot layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        // 🖍️ Nagba-bind ng data sa bawat item ng RecyclerView
        val context = holder.itemView.context
        val timeSlot = timeSlots[position]  // ⏰ Time slot sa kasalukuyang posisyon
        holder.tvTimeSlot.text = timeSlot   // Ipakita ang oras sa TextView

        val isBooked = bookedSlots.contains(timeSlot)   // ❌ Check kung booked na ang slot
        val isPastSlot = isPastTimeSlot(timeSlot)       // ⏳ Check kung ang slot ay nasa nakaraan na

        when {
            isPastSlot -> {
                // ⬇️ Disable at gawing gray ang mga past slots
                holder.itemView.isEnabled = false
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.gray))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
            }
            isBooked -> {
                // ❌ Disable at gawing pula ang mga booked slots
                holder.itemView.isEnabled = false
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.red))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            }
            else -> {
                // ✅ Enable ang mga available slots, i-black ang text, green ang indicator
                holder.itemView.isEnabled = !hasActiveAppointment
                holder.tvTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.black))
                holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
        }

        // 🟦 I-highlight ang kasalukuyang napiling slot (kung enabled)
        if (selectedPosition == position && holder.itemView.isEnabled) {
            holder.slotIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
        }

        // 👆 Handle click event sa slot
        holder.itemView.setOnClickListener {
            if (!holder.itemView.isEnabled) return@setOnClickListener  // Huwag gawin kung disabled

            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition  // I-update ang selected position
            notifyItemChanged(previousPosition)         // I-refresh ang dati nang napili para mawala highlight
            notifyItemChanged(selectedPosition)         // I-refresh ang bagong napili para i-highlight

            onSlotSelected(timeSlot)  // Tawagin ang callback na may napiling slot
        }
    }

    override fun getItemCount(): Int = timeSlots.size
    // 🔢 Ibalik ang dami ng time slots para sa RecyclerView

    inner class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 🧱 ViewHolder class para hawakan ang mga UI component ng bawat item
        val tvTimeSlot: TextView = itemView.findViewById(R.id.tv_slot_time)    // TextView para sa oras ng slot
        val slotIndicator: View = itemView.findViewById(R.id.slot_indicator)  // Maliit na indicator view
    }

    private fun isPastTimeSlot(slotTime: String): Boolean {
        // ⏳ Tinitingnan kung ang slot time ay nasa nakaraan na kumpara sa ngayon
        return try {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())   // Format ng oras
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Format ng petsa

            val selectedDateParsed = dateFormat.parse(selectedDate) ?: return true
            // I-parse ang napiling petsa; kung mali, ituturing na past

            val slotTimeParsed = timeFormat.parse(slotTime) ?: return true
            // I-parse ang slot time; kung mali, ituturing na past

            val slotDateTime = Calendar.getInstance().apply {
                time = selectedDateParsed
                val timeOnly = Calendar.getInstance().apply { time = slotTimeParsed }
                set(Calendar.HOUR_OF_DAY, timeOnly.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeOnly.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Pinagsama ang petsa at oras para makabuo ng buong datetime ng slot

            val now = Calendar.getInstance()
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            // Kunin ang kasalukuyang oras (ngayon) at i-clear ang seconds at milliseconds

            now.after(slotDateTime)
            // Bumalik ng true kung ang ngayon ay mas huli kaysa sa slotDateTime (past na)
        } catch (e: Exception) {
            true  // Sa error, ituturing na past para hindi ma-select
        }
    }

}
