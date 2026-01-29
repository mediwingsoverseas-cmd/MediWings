package com.tripplanner.mediwings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AdminDashboardActivity : AppCompatActivity() {

    private var currentBannerIndex: Int = 1
    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                uploadBanner(imageUri, currentBannerIndex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<CardView>(R.id.btnAdminStudents).setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "control")
            startActivity(intent)
        }

        findViewById<CardView>(R.id.btnAdminMessages).setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "chat")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnUploadBanner1).setOnClickListener {
            currentBannerIndex = 1
            openGallery()
        }

        findViewById<Button>(R.id.btnUploadBanner2).setOnClickListener {
            currentBannerIndex = 2
            openGallery()
        }

        findViewById<Button>(R.id.btnUploadBanner3).setOnClickListener {
            currentBannerIndex = 3
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadBanner(uri: Uri, index: Int) {
        val bannerRef = storage.reference.child("Banners/banner$index.jpg")
        bannerRef.putFile(uri).addOnSuccessListener {
            bannerRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                database.reference.child("Banners").child("banner$index").setValue(downloadUrl.toString())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Banner $index uploaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}