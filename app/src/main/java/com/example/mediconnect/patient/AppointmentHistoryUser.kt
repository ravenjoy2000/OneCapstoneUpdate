package com.example.mediconnect.patient

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediconnect.R
import com.example.mediconnect.activities.BaseActivity
import com.example.mediconnect.models.Appointment
import com.example.mediconnect.patient_adapter.AppointmentHistoryAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

// Activity para ipakita ang kasaysayan ng appointment ng user (pasyente)
class AppointmentHistoryUser : BaseActivity() {

    private lateinit var recyclerView: RecyclerView   // RecyclerView para sa listahan ng appointment history
    private val historyList = mutableListOf<Appointment>() // Listahan ng mga appointment
    private lateinit var adapter: AppointmentHistoryAdapter  // Adapter para sa RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Para sa edge-to-edge na UI display (fullscreen)
        setContentView(R.layout.activity_appointment_history_user)  // I-set ang layout ng activity

        // Itago ang status bar depende sa Android version (fullscreen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()      // I-setup ang toolbar/action bar
        setupRecyclerView()   // I-setup ang RecyclerView para sa appointment history

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return  // Kunin ang kasalukuyang user ID
        loadAppointmentHistory(userId)  // Kuhanin ang appointment history mula Firestore
        monitorLateAppointments(userId) // Bantayan kung may late appointments at markahan
        checkCancellationLimit(userId)  // Tsek kung sobra na ang cancelations ngayon
    }

    // I-setup ang toolbar na may back button at title
    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_appointment)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)  // Enable ang back button
            setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_new_24)  // Icon ng back button
            title = getString(R.string.my_appointment_title)  // Title ng toolbar
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()  // Back navigation kapag pinindot ang back button
        }
    }

    // I-setup ang RecyclerView at ang adapter nito
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_appointment_history)  // Kunin ang RecyclerView sa layout
        recyclerView.layoutManager = LinearLayoutManager(this)    // Linear layout ng list
        adapter = AppointmentHistoryAdapter(historyList)          // I-assign ang adapter
        recyclerView.adapter = adapter                             // Itakda ang adapter sa RecyclerView
    }

    // Kunin ang appointment history ng pasyente mula sa Firestore database
    private fun loadAppointmentHistory(userId: String) {
        val db = FirebaseFirestore.getInstance()  // Kunin ang Firestore instance
        db.collection("appointments")
            .whereEqualTo("patientId", userId)   // Kunin lang mga appointment ng kasalukuyang pasyente
            .whereIn("status", listOf("completed", "cancelled", "late", "no_show")) // Mga status na kasama sa history
            .get()
            .addOnSuccessListener { documents ->
                historyList.clear()  // Linisin ang list bago mag-load ng bagong data
                for (doc in documents) {
                    // Gumawa ng Appointment object mula sa data ng Firestore document
                    val appointment = Appointment(
                        date = doc.getString("date") ?: "",
                        time = doc.getString("timeSlot") ?: "",
                        mode = doc.getString("mode") ?: "",
                        status = doc.getString("status") ?: "",
                        doctorName = doc.getString("doctorName") ?: "Unknown",
                        reason = doc.getString("reason") ?: "No reason provided",
                        note = doc.getString("notes") ?: "",
                        location = doc.getString("doctorAddress") ?: doc.getString("location") ?: "N/A",
                        bookedAt = doc.getTimestamp("bookedAt")?.toDate()
                    )
                    historyList.add(appointment)  // Idagdag sa listahan ng history
                }
                adapter.notifyDataSetChanged()  // I-refresh ang RecyclerView para ipakita ang bagong data
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to load appointment history", e)  // Log kapag may error
                Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
    }

    // Bantayan ang mga nakatakdang appointment kung late na at markahan ang status na 'late'
    private fun monitorLateAppointments(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereIn("status", listOf("booked", "rescheduled", "rescheduled_once")) // Mga appointment na aktibo
            .get()
            .addOnSuccessListener { documents ->
                val currentTime = Calendar.getInstance().time  // Kunin ang kasalukuyang oras
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())  // Format ng date-time
                var anyLateMarked = false  // Flag kung may na-markang late

                for (doc in documents) {
                    try {
                        val date = doc.getString("date") ?: continue
                        val time = doc.getString("timeSlot") ?: continue
                        val appointmentDateTime = sdf.parse("$date $time") ?: continue  // I-parse ang datetime

                        val difference = currentTime.time - appointmentDateTime.time  // Compute ang delay (ms)
                        if (difference > 15 * 60 * 1000) {  // Kapag higit sa 15 minuto na late
                            db.collection("appointments").document(doc.id)
                                .update("status", "late")  // I-update ang status sa "late"
                                .addOnSuccessListener {
                                    Log.d("AppointmentStatus", "Marked late: ${doc.id}")
                                    anyLateMarked = true
                                }
                        }
                    } catch (e: ParseException) {
                        Log.e("DateParseError", "Failed to parse appointment datetime", e)
                    }
                }

                // Ipabatid sa user kung may mga late na appointment na na-mark
                if (anyLateMarked) {
                    Toast.makeText(
                        this,
                        "⚠️ Some of your appointments were marked late. Please rebook or contact support.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to monitor late appointments", Toast.LENGTH_SHORT).show()
            }
    }

    // Tsek kung nakapangalawang tatlong cancellation na sa araw na ito at limitahan ang booking kung oo
    private fun checkCancellationLimit(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())  // Kuhanin ang petsa ngayon

        db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereEqualTo("status", "cancelled")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() >= 3) {  // Kapag 3 o higit pang cancellation ngayong araw
                    val restrictedUntil = Timestamp(Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))  // 48 oras mula ngayon
                    val userRef = db.collection("users").document(userId)

                    // Kunin ang user doc para i-check ang kasalukuyang restriction
                    userRef.get().addOnSuccessListener { userSnap ->
                        val existingRestriction = userSnap.getTimestamp("bookingRestrictedUntil")
                        val now = Timestamp.now()

                        // Kung wala pang restriction o expired na, i-update ang restriction timestamp
                        if (existingRestriction == null || existingRestriction.toDate().before(Date())) {
                            userRef.update("bookingRestrictedUntil", restrictedUntil)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "🚫 You have canceled 3 appointments today. Booking is disabled for 48 hours.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("FirestoreError", "Failed to check cancellation limit")  // Log kapag may error
            }
    }
}
