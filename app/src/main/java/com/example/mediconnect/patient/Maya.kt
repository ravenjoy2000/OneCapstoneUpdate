package com.example.mediconnect.patient

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class Maya : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSaveQr: MaterialButton
    private lateinit var btnShareQr: MaterialButton
    private lateinit var btnPaymentCompleted: MaterialButton
    private lateinit var txtCancelPayment: TextView
    private lateinit var imgMayaQr: ImageView

    // Payment summary fields
    private lateinit var tvReason: TextView
    private lateinit var tvServicePrice: TextView
    private lateinit var tvMayaFee: TextView
    private lateinit var tvMayaFeePrice: TextView
    private lateinit var tvTotalAmount: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maya)

        // Initialize views
        btnBack = findViewById(R.id.btn_back)
        btnSaveQr = findViewById(R.id.btn_save_qr)
        btnShareQr = findViewById(R.id.btn_share_qr)
        btnPaymentCompleted = findViewById(R.id.btn_payment_completed)
        txtCancelPayment = findViewById(R.id.txt_cancel_payment)
        imgMayaQr = findViewById(R.id.img_maya_qr)

        // Payment summary
        tvReason = findViewById(R.id.tv_consultation_fee)
        tvServicePrice = findViewById(R.id.tv_consultation_fee_price)
        tvMayaFee = findViewById(R.id.tv_maya_fee)
        tvMayaFeePrice = findViewById(R.id.tv_maya_fee_price)
        tvTotalAmount = findViewById(R.id.tv_total_amount)


        // Kunin ang appointmentId mula sa Intent
        val appointmentId = intent.getStringExtra("appointmentId")
        if (appointmentId != null) {
            loadAppointmentDetails(appointmentId)
        } else {
            Toast.makeText(this, "No appointmentId provided", Toast.LENGTH_SHORT).show()
        }

        // Back button
        btnBack.setOnClickListener { finish() }

        // Save QR button
        btnSaveQr.setOnClickListener { saveQrToGallery() }

        // Share QR button
        btnShareQr.setOnClickListener { shareQrImage() }

        // Payment completed
        btnPaymentCompleted.setOnClickListener {
            Toast.makeText(this, "Payment confirmed! Thank you.", Toast.LENGTH_LONG).show()
            // TODO: Update Firestore appointment status or navigate to confirmation screen
            finish()
        }

        // Cancel payment
        txtCancelPayment.setOnClickListener {
            Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadAppointmentDetails(appointmentId: String) {
        db.collection("appointments")
            .document(appointmentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get appointment fields
                    val reason = document.getString("reason") ?: "N/A"
                    val servicePrice = document.getLong("servicePrice")?.toInt() ?: 0
                    val doctorName = document.getString("doctorName") ?: "N/A"
                    val doctorPhone = document.getString("doctorPhone") ?: "N/A"
                    val doctorAddress = document.getString("doctorAddress") ?: "N/A"
                    val patientName = document.getString("patientName") ?: "N/A"
                    val date = document.getString("date") ?: "N/A"
                    val timeSlot = document.getString("timeSlot") ?: "N/A"
                    val mode = document.getString("mode") ?: "N/A"
                    val status = document.getString("status") ?: "N/A"


                    // Payment summary UI
                    tvReason.text = "Reason:"
                    tvServicePrice.text = reason

                    tvMayaFee.text = "Service Price:"
                    tvMayaFeePrice.text = "₱$servicePrice.00"

                    val mayaFee = 15
                    val total = servicePrice + mayaFee
                    tvTotalAmount.text = "₱$total.00"

                    // OPTIONAL: kung may extra TextViews ka sa layout
                    // pwede mong i-set ganito:
                    //
                    // tvPatientName.text = "Patient: $currentUserName"
                    // tvPatientEmail.text = "Email: $currentUserEmail"
                    // tvDoctorName.text = "Doctor: $doctorName"
                    // tvAppointmentDate.text = "Date: $date $timeSlot"
                    // tvMode.text = "Mode: $mode"
                    // tvStatus.text = "Status: $status"
                    // tvDoctorAddress.text = doctorAddress
                    // tvDoctorPhone.text = doctorPhone

                } else {
                    Toast.makeText(this, "No appointment data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveQrToGallery() {
        val drawable = imgMayaQr.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap
        if (bitmap != null) {
            MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "Maya_QR_${System.currentTimeMillis()}",
                "Maya QR Code"
            )
            Toast.makeText(this, "QR saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareQrImage() {
        val drawable = imgMayaQr.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap
        if (bitmap != null) {
            val uri = getImageUri(bitmap)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Scan this QR to pay via Maya")
            }
            startActivity(Intent.createChooser(shareIntent, "Share QR via"))
        } else {
            Toast.makeText(this, "No QR image to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "QR_Code", null)
        return Uri.parse(path)
    }
}
