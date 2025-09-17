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
import java.io.ByteArrayOutputStream

class Maya : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSaveQr: MaterialButton
    private lateinit var btnShareQr: MaterialButton
    private lateinit var btnPaymentCompleted: MaterialButton
    private lateinit var txtCancelPayment: TextView
    private lateinit var imgMayaQr: ImageView

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

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Save QR button
        btnSaveQr.setOnClickListener {
            saveQrToGallery()
        }

        // Share QR button
        btnShareQr.setOnClickListener {
            shareQrImage()
        }

        // Payment completed
        btnPaymentCompleted.setOnClickListener {
            Toast.makeText(this, "Payment confirmed! Thank you.", Toast.LENGTH_LONG).show()
            // TODO: Update Firestore or navigate to appointment confirmation screen
            finish()
        }

        // Cancel payment
        txtCancelPayment.setOnClickListener {
            Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show()
            finish()
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
