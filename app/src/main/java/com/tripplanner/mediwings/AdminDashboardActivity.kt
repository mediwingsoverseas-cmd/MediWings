package com.tripplanner.mediwings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AdminDashboardActivity : AppCompatActivity() {

    private var currentBannerId = 0
    private lateinit var etHomeContent: EditText
    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadBannerToFirebase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        etHomeContent = findViewById(R.id.etHomeContent)

        findViewById<Button>(R.id.btnAdminStudents).setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "control")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAdminMessages).setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "chat")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAddUniversity).setOnClickListener {
            startActivity(Intent(this, AdminAddUniversityActivity::class.java))
        }

        findViewById<Button>(R.id.btnEditContact).setOnClickListener {
            startActivity(Intent(this, AdminEditContactActivity::class.java))
        }

        findViewById<Button>(R.id.btnUploadBanner1).setOnClickListener {
            currentBannerId = 1
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnUploadBanner2).setOnClickListener {
            currentBannerId = 2
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnUploadBanner3).setOnClickListener {
            currentBannerId = 3
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnInsertBold).setOnClickListener {
            val start = etHomeContent.selectionStart
            val end = etHomeContent.selectionEnd
            etHomeContent.text.insert(start, "<b>")
            etHomeContent.text.insert(end + 3, "</b>")
        }

        findViewById<Button>(R.id.btnInsertImage).setOnClickListener {
            val start = etHomeContent.selectionStart
            etHomeContent.text.insert(start, "<img src=\"URL_HERE\" width=\"100%\">")
        }

        findViewById<Button>(R.id.btnSaveCMS).setOnClickListener {
            val content = etHomeContent.text.toString()
            if (content.isNotEmpty()) {
                database.reference.child("CMS").child("home_content").setValue(content)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Home Page Updated!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        database.reference.child("CMS").child("home_content").get().addOnSuccessListener {
            if (it.exists()) {
                etHomeContent.setText(it.value.toString())
            }
        }
        
        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            // Clear saved preferences
            val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun uploadBannerToFirebase(fileUri: Uri) {
        Toast.makeText(this, "Uploading Banner $currentBannerId...", Toast.LENGTH_SHORT).show()
        val storageRef = storage.reference.child("banners/banner_$currentBannerId.jpg")

        storageRef.putFile(fileUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                saveBannerUrlToDatabase(downloadUri.toString())
            } else {
                Toast.makeText(this, "Upload Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveBannerUrlToDatabase(url: String) {
        database.reference.child("Banners").child("banner$currentBannerId").setValue(url)
            .addOnSuccessListener {
                Toast.makeText(this, "Banner $currentBannerId Saved!", Toast.LENGTH_SHORT).show()
            }
    }
}