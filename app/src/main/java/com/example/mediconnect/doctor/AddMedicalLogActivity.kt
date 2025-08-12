package com.example.mediconnect.doctor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mediconnect.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddMedicalLogActivity : AppCompatActivity() {

    // UI elements
    private lateinit var etDiagnosis: EditText          // Input para sa diagnosis
    private lateinit var etDoctorNotes: EditText        // Input para sa notes ng doctor
    private lateinit var btnUploadFile: Button          // Button para mag-upload ng file
    private lateinit var tvSelectedFile: TextView       // TextView para ipakita ang napiling file
    private lateinit var btnSave: Button                 // Button para i-save ang medical log

    // Firebase instances
    private val db = FirebaseFirestore.getInstance()    // Firestore database instance
    private val storageRef = FirebaseStorage.getInstance().reference  // Firebase Storage reference

    // Variables para sa file upload
    private var selectedFileUri: Uri? = null            // URI ng napiling file mula sa device
    private var uploadedFileUrl: String? = null         // URL ng na-upload na file sa Firebase Storage

    companion object {
        private const val PICK_FILE_REQUEST_CODE = 1001  // Request code para sa file picker intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()                               // Para sa full edge-to-edge display
        setContentView(R.layout.activity_add_medical_log) // Itakda ang layout ng activity

        // I-bind ang mga UI element sa variables gamit findViewById
        etDiagnosis = findViewById(R.id.etDiagnosis)
        etDoctorNotes = findViewById(R.id.etDoctorNotes)
        btnUploadFile = findViewById(R.id.btnUploadFile)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        btnSave = findViewById(R.id.btnSave)

        // Kunin ang mga intent extras na ipapasa sa activity (appointmentId, patientId, doctorId)
        val appointmentId = intent.getStringExtra("appointmentId") ?: ""
        val patientId = intent.getStringExtra("patientId") ?: ""
        val doctorId = intent.getStringExtra("doctorId") ?: ""

        // Button para buksan ang file picker
        btnUploadFile.setOnClickListener {
            openFilePicker()
        }

        // Button para i-save ang medical log
        btnSave.setOnClickListener {
            val diagnosis = etDiagnosis.text.toString().trim()       // Kunin ang diagnosis input
            val doctorNotes = etDoctorNotes.text.toString().trim()   // Kunin ang doctor notes input

            // Validation kung empty ang mga fields
            if (diagnosis.isEmpty()) {
                etDiagnosis.error = "Please enter diagnosis"         // Error message kapag walang diagnosis
                etDiagnosis.requestFocus()
                return@setOnClickListener
            }
            if (doctorNotes.isEmpty()) {
                etDoctorNotes.error = "Please enter doctor notes"    // Error message kapag walang notes
                etDoctorNotes.requestFocus()
                return@setOnClickListener
            }

            btnSave.isEnabled = false    // I-disable ang save button para maiwasan multiple clicks

            if (selectedFileUri != null) {
                // Kapag may file na napili, i-upload muna ang file bago mag-save ng log
                uploadFileThenSaveLog(appointmentId, patientId, doctorId, diagnosis, doctorNotes)
            } else {
                // Direktang i-save ang medical log kung walang file upload
                saveMedicalLog(
                    appointmentId,
                    patientId,
                    doctorId,
                    diagnosis,
                    doctorNotes,
                    fileUrl = null
                )
            }
        }
    }

    // Function para magbukas ng file picker para pumili ng pdf o image file
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)   // Intent para pumili ng file
        intent.type = "*/*"                               // Accept lahat ng file types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*")) // Limit lang sa pdf at images
        startActivityForResult(Intent.createChooser(intent, "Select PDF or Image"), PICK_FILE_REQUEST_CODE)
    }

    // Result handler kapag nakapili ng file sa picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data     // I-save ang URI ng napiling file
            if (selectedFileUri != null) {
                // Ipakita ang pangalan ng napiling file sa TextView
                val fileName = selectedFileUri!!.lastPathSegment ?: "selected_file"
                tvSelectedFile.text = "Selected: $fileName"
            }
        }
    }

    // Function para i-upload ang file sa Firebase Storage tapos i-save ang medical log
    private fun uploadFileThenSaveLog(
        appointmentId: String,
        patientId: String,
        doctorId: String,
        diagnosis: String,
        doctorNotes: String
    ) {
        btnUploadFile.isEnabled = false    // I-disable ang upload button habang nag-upload
        tvSelectedFile.text = "Uploading..."   // I-update ang status ng file upload

        // Gumawa ng reference sa Storage folder na "medical_logs_files" gamit timestamp bilang filename
        val fileRef: StorageReference = storageRef.child("medical_logs_files/${System.currentTimeMillis()}")

        selectedFileUri?.let { uri ->
            fileRef.putFile(uri)    // I-upload ang file sa Firebase Storage
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedFileUrl = downloadUri.toString()  // Kunin ang download URL ng uploaded file
                        tvSelectedFile.text = "Upload complete"  // Ipakita na tapos ang upload

                        // Tawagin ang function para i-save ang medical log sa Firestore kasama ang file URL
                        saveMedicalLog(
                            appointmentId,
                            patientId,
                            doctorId,
                            diagnosis,
                            doctorNotes,
                            uploadedFileUrl
                        )
                    }.addOnFailureListener { e ->
                        // Kapag nabigo makuha ang URL ng file, enable ulit ang buttons at ipakita error
                        btnUploadFile.isEnabled = true
                        btnSave.isEnabled = true
                        tvSelectedFile.text = "Upload failed"
                        Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    // Kapag nabigo ang pag-upload, enable ulit ang buttons at ipakita error
                    btnUploadFile.isEnabled = true
                    btnSave.isEnabled = true
                    tvSelectedFile.text = "Upload failed"
                    Toast.makeText(this, "File upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Function para i-save ang medical log data sa Firestore
    private fun saveMedicalLog(
        appointmentId: String,
        patientId: String,
        doctorId: String,
        diagnosis: String,
        doctorNotes: String,
        fileUrl: String?     // Optional URL ng uploaded file
    ) {
        // Gumawa ng map ng mga field at value para i-save sa Firestore
        val log = hashMapOf(
            "appointmentId" to appointmentId,
            "patientId" to patientId,
            "doctorId" to doctorId,
            "diagnosis" to diagnosis,
            "doctorNotes" to doctorNotes,
            "fileUrl" to fileUrl,
            "date" to FieldValue.serverTimestamp(),  // Gamitin ang server timestamp para sa date
            "status" to "Completed"                   // Status ng medical log
        )

        // I-save ang medical log sa "medical_logs" collection ng Firestore
        db.collection("medical_logs")
            .add(log)
            .addOnSuccessListener {
                Toast.makeText(this, "Medical Log Saved", Toast.LENGTH_SHORT).show()  // Ipakita success message
                finish()  // Isara ang activity
            }
            .addOnFailureListener { e ->
                // Kapag nag-error sa save, ipakita error at i-enable ulit ang buttons
                Toast.makeText(this, "Error saving log: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnUploadFile.isEnabled = true
            }
    }
}
