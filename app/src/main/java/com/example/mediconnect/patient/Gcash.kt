package com.example.mediconnect.patient

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class Gcash : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSaveQr: MaterialButton
    private lateinit var btnShareQr: MaterialButton
    private lateinit var btnPaymentCompleted: MaterialButton
    private lateinit var txtCancelPayment: TextView
    private lateinit var imgQr: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gcash)

        // Init views
        btnBack = findViewById(R.id.btn_back)
        btnSaveQr = findViewById(R.id.btn_save_qr)
        btnShareQr = findViewById(R.id.btn_share_qr)
        btnPaymentCompleted = findViewById(R.id.btn_payment_completed)
        txtCancelPayment = findViewById(R.id.txt_cancel_payment)
        imgQr = findViewById(R.id.img_gcash_qr)

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
            shareQr()
        }

        // Payment completed
        btnPaymentCompleted.setOnClickListener {
            Toast.makeText(this, "Payment Completed!", Toast.LENGTH_SHORT).show()
            // dito ka pwedeng maglagay ng Firestore update or balik sa main screen
            finish()
        }

        // Cancel Payment
        txtCancelPayment.setOnClickListener {
            Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveQrToGallery() {
        try {
            val drawable = imgQr.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap

            val filename = "gcash_qr_${System.currentTimeMillis()}.png"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, file.name, null)

            Toast.makeText(this, "QR saved to Gallery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareQr() {
        try {
            val drawable = imgQr.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap

            val file = File(externalCacheDir, "gcash_share.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            file.setReadable(true, false)

            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "Share QR via"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share QR", Toast.LENGTH_SHORT).show()
        }
    }
}
