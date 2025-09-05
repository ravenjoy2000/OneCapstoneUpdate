package com.example.mediconnect.patient

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.models.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MedicationLogActivity : AppCompatActivity() {

    private lateinit var medicationContainer: LinearLayout
    private lateinit var btnAddDrug: Button
    private lateinit var btnSaveMedication: Button
    private lateinit var progressDialog: AlertDialog

    private lateinit var reminderAdapter: ReminderAdapter
    private val reminderList = mutableListOf<Reminder>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medication_log)

        medicationContainer = findViewById(R.id.medicationContainer)
        btnAddDrug = findViewById(R.id.btnAddDrug)
        btnSaveMedication = findViewById(R.id.btnSaveMedication)

        val progressView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 20, 40, 20)
            addView(ProgressBar(this@MedicationLogActivity))
            addView(TextView(this@MedicationLogActivity).apply {
                text = "Saving medications..."
                setPadding(20, 0, 0, 0)
            })
        }
        progressDialog = AlertDialog.Builder(this)
            .setView(progressView)
            .setCancelable(false)
            .create()

        createNotificationChannel()

        btnAddDrug.setOnClickListener { addMedicationEntry() }
        btnSaveMedication.setOnClickListener { saveAllMedications() }

        addMedicationEntry()

        val rvReminders = findViewById<RecyclerView>(R.id.recyclerSavedMedications)

        reminderAdapter = ReminderAdapter(
            reminderList,
            onMarkTaken = { reminder -> confirmMedicationTaken(reminder) },
            onCancelReminder = { reminder -> cancelMedicationReminder(reminder) }
        )

        rvReminders.layoutManager = LinearLayoutManager(this)
        rvReminders.adapter = reminderAdapter

        loadMedications()
    }

    private fun cancelMedicationReminder(reminder: Reminder) {
        val patientId = auth.currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Cancel Reminder")
            .setMessage("Do you want to cancel the reminder for ${reminder.name}?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("patients").document(patientId)
                    .collection("medications")
                    .document(reminder.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "${reminder.name} reminder canceled ❌", Toast.LENGTH_SHORT).show()
                        loadMedications()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun addMedicationEntry() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_medication, medicationContainer, false)

        val btnAddTime = view.findViewById<Button>(R.id.btnAddTime)
        val btnRemove = view.findViewById<Button>(R.id.btnRemoveMedication)
        val layoutSelectedTimes = view.findViewById<LinearLayout>(R.id.item_medication_times)

        val selectedTimes = mutableListOf<String>()

        btnAddTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, m ->
                val storeFormat = String.format(Locale.getDefault(), "%02d:%02d", h, m)

                if (storeFormat in selectedTimes) {
                    Toast.makeText(this, "Time already added!", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                selectedTimes.add(storeFormat)

                val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }

                val tv = TextView(this).apply {
                    text = "🕒 ${displayFormat.format(cal.time)}"
                    setPadding(8, 8, 8, 8)
                    setTextColor(ContextCompat.getColor(this@MedicationLogActivity, android.R.color.black))
                }
                layoutSelectedTimes.addView(tv)
            }, hour, minute, false).show()
        }

        btnRemove.setOnClickListener {
            medicationContainer.removeView(view)
        }

        view.tag = selectedTimes
        medicationContainer.addView(view)
    }

    private fun saveAllMedications() {
        val patientId = auth.currentUser?.uid ?: return

        if (medicationContainer.childCount == 0) {
            Toast.makeText(this, "Please add at least one medication entry", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        val entries = mutableListOf<Map<String, Any>>()

        for (i in 0 until medicationContainer.childCount) {
            val view = medicationContainer.getChildAt(i)
            val medName = view.findViewById<EditText>(R.id.etMedicationName)?.text?.toString()?.trim().orEmpty()
            val medDosage = view.findViewById<EditText>(R.id.etDosage)?.text?.toString()?.trim().orEmpty()
            val medFrequency = view.findViewById<EditText>(R.id.etFrequency)?.text?.toString()?.trim().orEmpty()
            val selectedTimes = view.tag as? MutableList<String> ?: mutableListOf()

            if (medName.isBlank() || medDosage.isBlank() || medFrequency.isBlank() || selectedTimes.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and add times for each medication", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
                return
            }

            val medId = db.collection("patients").document(patientId).collection("medications").document().id
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())


            val medMap = mapOf(
                "id" to medId,
                "name" to medName,
                "dosage" to medDosage,
                "frequency" to medFrequency,
                "times" to selectedTimes,
                "timestamp" to timestamp,
                "status" to "pending",
                "date" to currentDate   // 👈 store date
            )
            entries.add(medMap)

            scheduleNotifications(medName, selectedTimes)
        }

        val batch = db.batch()
        val medCollection = db.collection("patients").document(patientId).collection("medications")
        entries.forEach { med ->
            val docRef = medCollection.document(med["id"] as String)
            batch.set(docRef, med)
        }

        batch.commit()
            .addOnSuccessListener {
                progressDialog.dismiss()
                showSummary(entries)
                medicationContainer.removeAllViews()
                addMedicationEntry()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save medications", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMedications() {
        val patientId = auth.currentUser?.uid ?: return
        db.collection("patients").document(patientId).collection("medications")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                reminderList.clear()
                for (doc in result) {
                    val id = doc.getString("id") ?: ""
                    val name = doc.getString("name") ?: ""
                    val dosage = doc.getString("dosage") ?: ""
                    val frequency = doc.getString("frequency") ?: ""
                    val times = (doc.get("times") as? List<String>)?.joinToString(", ") ?: "N/A"
                    val status = doc.getString("status") ?: "pending"
                    val createdAt = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val date = doc.getString("date") ?: "N/A"

                    reminderList.add(Reminder(id, name, dosage, frequency, times, status, createdAt))
                }
                reminderAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load medications", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSummary(entries: List<Map<String, Any>>) {
        reminderList.clear()

        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        entries.forEach { med ->
            val times = (med["times"] as List<String>).map {
                val (h, m) = it.split(":").map(String::toInt)
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }.let { cal -> displayFormat.format(cal.time) }
            }

            reminderList.add(
                Reminder(
                    id = med["id"].toString(),
                    name = med["name"].toString(),
                    dosage = med["dosage"].toString(),
                    frequency = med["frequency"].toString(),
                    times = times.joinToString(", "),
                    status = "pending",
                    createdAt = med["timestamp"] as Long
                )
            )
        }

        reminderAdapter.notifyDataSetChanged()

        AlertDialog.Builder(this)
            .setTitle("Saved Medications ✅")
            .setMessage("Your medications have been saved and reminders scheduled.\n\nTap a medication below when you have taken it.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmMedicationTaken(reminder: Reminder) {
        val patientId = auth.currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Confirm Medication")
            .setMessage("Did you take ${reminder.name}?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("patients").document(patientId)
                    .collection("medications")
                    .document(reminder.id)
                    .update("status", "taken")
                    .addOnSuccessListener {
                        Toast.makeText(this, "${reminder.name} marked as taken ✅", Toast.LENGTH_SHORT).show()
                        loadMedications()
                    }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()  // 👈 Do nothing, just close the dialog
            }
            .show()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "medication_channel",
                "Medication Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication reminders"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotifications(medName: String, times: List<String>) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val patientId = auth.currentUser?.uid ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please allow exact alarms in settings for reminders to work.", Toast.LENGTH_LONG).show()
                return
            }
        }

        times.forEach { timeStr ->
            val (hour, minute) = timeStr.split(":").map(String::toInt)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
            }

            val intent = Intent(this, MedicationAlarmReceiver::class.java).apply {
                putExtra("medName", medName)
                putExtra("patientId", patientId)
            }

            val requestCode = (medName + timeStr).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, "Cannot schedule alarms: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
