package com.example.mediconnect.doctor

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.doctor_adapter.doctor_MedicalLogAdapter
import com.example.mediconnect.models.MedicalLog
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorMedicalLog : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: doctor_MedicalLogAdapter
    private lateinit var searchEditText: EditText

    private val db = FirebaseFirestore.getInstance()
    private val medicalLogs = mutableListOf<MedicalLog>()

    // debounce handler for search
    private var searchRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_medical_log)

        // ✅ Fullscreen mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_medical_log)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.recycler_medical_log)
        searchEditText = findViewById(R.id.search_edit_text)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = doctor_MedicalLogAdapter(mutableListOf())
        recyclerView.adapter = adapter

        fetchMedicalLogs()

        // ✅ Debounced search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable { filterLogs(s.toString()) }
                handler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun fetchMedicalLogs() {
        db.collection("medical_logs")
            .orderBy("appointmentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                medicalLogs.clear()
                for (doc in snapshot.documents) {

                    // ✅ Safe parsing of appointmentDate
                    val appointmentDate = when (val dateField = doc.get("appointmentDate")) {
                        is com.google.firebase.Timestamp -> dateField
                        is Long -> com.google.firebase.Timestamp(dateField, 0)
                        is String -> runCatching {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateField)
                        }.getOrNull()?.let { com.google.firebase.Timestamp(it) }
                        else -> null
                    }

                    // ✅ Format into readable string
                    val formattedDate = appointmentDate?.toDate()?.let { dateObj ->
                        SimpleDateFormat(
                            "MMMM d, yyyy 'at' hh:mm a",
                            Locale.getDefault()
                        ).format(dateObj)
                    } ?: "No Date"

                    val log = MedicalLog(
                        medicalLogId = doc.getString("medicalLogId"),
                        patientName = doc.getString("patientName"),
                        appointmentDate = appointmentDate,
                        diagnosis = doc.getString("diagnosis"),
                        doctorNotes = doc.getString("doctorNotes") ?: "No Notes Provided",
                        status = doc.getString("status"),
                        date = formattedDate,
                        doctorName = doc.getString("doctorName"),
                        doctorId = doc.getString("doctorId"),
                        patientId = doc.getString("patientId"),
                        appointmentId = doc.getString("appointmentId"),
                        appointmentTime = doc.getString("appointmentTime"),
                        appointmentDay = doc.getString("appointmentDay"),
                        appointmentMonth = doc.getString("appointmentMonth"),
                        appointmentYear = doc.getString("appointmentYear"),
                        appointmentHour = doc.getString("appointmentHour"),
                        appointmentMinute = doc.getString("appointmentMinute")
                    )
                    medicalLogs.add(log)
                }
                adapter.updateList(medicalLogs)
            }
    }

    private fun filterLogs(query: String) {
        val filtered = medicalLogs.filter {
            it.patientName?.contains(query, ignoreCase = true) == true ||
                    it.diagnosis?.contains(query, ignoreCase = true) == true ||
                    it.doctorNotes?.contains(query, ignoreCase = true) == true ||
                    it.date?.contains(query, ignoreCase = true) == true
        }
        adapter.updateList(filtered)
    }
}
