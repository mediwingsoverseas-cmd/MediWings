package com.tripplanner.mediwings

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AdminAddUniversityActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etDetails: EditText
    private lateinit var ivPreview: ImageView
    private var imageUri: Uri? = null

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            ivPreview.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_university)

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add University"
        toolbar.setNavigationOnClickListener { finish() }

        etName = findViewById(R.id.etUniName)
        etDetails = findViewById(R.id.etUniDetails)
        ivPreview = findViewById(R.id.ivUniPhotoPreview)

        findViewById<Button>(R.id.btnPickUniPhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnSaveUniversity).setOnClickListener {
            saveUniversity()
        }
    }

    private fun saveUniversity() {
        val name = etName.text.toString().trim()
        val details = etDetails.text.toString().trim()

        if (name.isEmpty() || details.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill all fields and pick an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Create unique filename with timestamp
        val timestamp = System.currentTimeMillis()
        val fileName = "university_${timestamp}.jpg"
        
        // Use correct storage path
        val storageRef = storage.reference
            .child("universities")
            .child(fileName)

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
        
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val uniId = database.reference.child("Universities").push().key ?: return@addOnSuccessListener
                    val uniData = mapOf(
                        "id" to uniId,
                        "name" to name,
                        "details" to details,
                        "imageUrl" to downloadUrl.toString()
                    )
                    database.reference.child("Universities").child(uniId).setValue(uniData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "University Added!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}