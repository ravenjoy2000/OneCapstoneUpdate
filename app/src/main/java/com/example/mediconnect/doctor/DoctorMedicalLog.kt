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

class DoctorMedicalLog : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: doctor_MedicalLogAdapter
    private lateinit var searchEditText: EditText

    private val db = FirebaseFirestore.getInstance()
    private val medicalLogs = mutableListOf<MedicalLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_medical_log)

        // Fullscreen
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

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs(s.toString())
            }
        })
    }

    private fun fetchMedicalLogs() {
        db.collection("medical_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                medicalLogs.clear()
                for (doc in documents) {

                    // ✅ Safe handling of appointmentDate
                    val dateField = doc.get("appointmentDate")
                    val appointmentDate: com.google.firebase.Timestamp? = when (dateField) {
                        is com.google.firebase.Timestamp -> dateField
                        is String -> try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val parsed = sdf.parse(dateField)
                            parsed?.let { com.google.firebase.Timestamp(it) }
                        } catch (e: Exception) {
                            null
                        }
                        is Long -> com.google.firebase.Timestamp(dateField, 0)
                        else -> null
                    }

                    val log = MedicalLog(
                        medicalLogId = doc.getString("medicalLogId") ?: "",
                        patientName = doc.getString("patientName") ?: "",
                        appointmentDate = appointmentDate, // ✅ fixed

                        diagnosis = doc.getString("diagnosis") ?: "",
                        doctorNotes = doc.getString("doctorNotes") ?: "No Notes Provided",
                        status = doc.getString("status") ?: "",
                        date = doc.getTimestamp("timestamp")
                            ?.toDate()
                            ?.let { dateObj ->
                                java.text.SimpleDateFormat(
                                    "MMMM d, yyyy 'at' hh:mm:ss a z",
                                    java.util.Locale.getDefault()
                                ).format(dateObj)
                            }
                            ?: "No Date",

                        doctorName = doc.getString("doctorName") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        appointmentId = doc.getString("appointmentId") ?: "",
                        appointmentTime = doc.getString("appointmentTime") ?: "",

                        appointmentDay = null,
                        appointmentMonth = null,
                        appointmentYear = null,
                        appointmentHour = null,
                        appointmentMinute = null

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
                    it.date?.contains(query, ignoreCase = true) == true // ✅ safe call
        }
        adapter.updateList(filtered)
    }



}
