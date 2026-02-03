package com.tripplanner.mediwings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            ivPreview.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_university)

        etName = findViewById(R.id.etUniName)
        etDetails = findViewById(R.id.etUniDetails)
        ivPreview = findViewById(R.id.ivUniPhotoPreview)

        findViewById<Button>(R.id.btnPickUniPhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
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

        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("universities/$fileName.jpg")

        ref.putFile(imageUri!!).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
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
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }
}