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

    // RecyclerView para ipakita ang listahan ng medical logs
    private lateinit var recyclerView: RecyclerView

    // Adapter para sa medical logs
    private lateinit var adapter: doctor_MedicalLogAdapter

    // EditText para sa search/filter ng logs
    private lateinit var searchEditText: EditText

    // Firestore instance para kunin ang data
    private val db = FirebaseFirestore.getInstance()

    // Mutable list para i-store ang medical logs na nakuha
    private val medicalLogs = mutableListOf<MedicalLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_medical_log)  // I-set ang layout ng activity

        // Itago ang status bar para maging fullscreen ang activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())  // Android 11+
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )  // Para sa mas lumang Android versions
        }

        // Kunin ang toolbar mula sa layout at i-set bilang action bar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_medical_log)
        setSupportActionBar(toolbar)

        // Kapag pinindot ang back arrow sa toolbar, i-handle ang back press
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Kunin ang mga views para sa RecyclerView at EditText sa search
        recyclerView = findViewById(R.id.recycler_medical_log)
        searchEditText = findViewById(R.id.search_edit_text)

        // I-setup ang RecyclerView na may LinearLayoutManager para vertical list
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Gumawa ng adapter na walang laman muna (empty list)
        adapter = doctor_MedicalLogAdapter(mutableListOf())

        // I-assign ang adapter sa RecyclerView
        recyclerView.adapter = adapter

        // Kuhanin ang medical logs mula sa Firestore
        fetchMedicalLogs()

        // Magdagdag ng listener para sa pagbabago sa EditText para sa search filter
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Kapag may nagbago sa search text, i-filter ang logs gamit ang query
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs(s.toString())
            }
        })
    }

    // Kuhanin ang medical logs mula sa Firestore collection na "appointments"
    private fun fetchMedicalLogs() {
        db.collection("appointments")
            .orderBy("date", Query.Direction.DESCENDING)  // I-order by date descending (pinakabago unahan)
            .get()
            .addOnSuccessListener { documents ->
                medicalLogs.clear()  // Linisin muna ang existing list

                // I-loop ang bawat dokumento mula sa Firestore
                for (doc in documents) {
                    // Gumawa ng MedicalLog object gamit ang data mula sa Firestore document
                    val log = MedicalLog(
                        patientName = doc.getString("patientName") ?: "",
                        appointmentDate = doc.getString("appointmentDate") ?: "",
                        diagnosis = doc.getString("diagnosis") ?: "",
                        notes = doc.getString("notes") ?: "",
                        status = doc.getString("status") ?: "",
                        doctorNotes = doc.getString("doctorNotes") ?: "",
                        date = doc.getString("date") ?: "",
                        doctorName = doc.getString("doctorName") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        appointmentId = doc.getString("appointmentId") ?: "",
                        appointmentTime = doc.getString("appointmentTime") ?: "",
                        appointmentDay = doc.getString("appointmentDay") ?: "",
                        appointmentMonth = doc.getString("appointmentMonth") ?: "",
                        appointmentYear = doc.getString("appointmentYear") ?: "",
                        appointmentHour = doc.getString("appointmentHour") ?: "",
                        appointmentMinute = doc.getString("appointmentMinute") ?: ""
                    )
                    medicalLogs.add(log)  // Idagdag ang log sa listahan
                }

                // I-update ang adapter gamit ang bagong listahan ng medical logs
                adapter.updateList(medicalLogs)
            }
    }

    // I-filter ang medical logs base sa search query
    private fun filterLogs(query: String) {
        // I-filter ang mga logs kung ang patientName o appointmentDate ay naglalaman ng query (case-insensitive)
        val filtered = medicalLogs.filter {
            it.patientName?.contains(query, ignoreCase = true) == true ||
                    it.appointmentDate?.contains(query, ignoreCase = true) == true
        }

        // I-update ang adapter gamit ang filtered list
        adapter.updateList(filtered)
    }
}
