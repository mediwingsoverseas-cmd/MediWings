package com.tripplanner.mediwings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import jp.wasabeef.richeditor.RichEditor

class AdminDashboardActivity : AppCompatActivity() {

    private var currentBannerId = 0
    private lateinit var richEditor: RichEditor
    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadBannerToFirebase(it) }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, proceed with image picker
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Permission denied. Cannot upload banners.", Toast.LENGTH_LONG)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        richEditor = findViewById(R.id.richEditor)

        // Configure RichEditor
        richEditor.setEditorHeight(200)
        richEditor.setEditorFontSize(16)
        richEditor.setPadding(10, 10, 10, 10)
        richEditor.setPlaceholder("Enter content here...")

        // Load dashboard stats
        loadDashboardStats()

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
            checkPermissionAndPickImage()
        }

        findViewById<Button>(R.id.btnUploadBanner2).setOnClickListener {
            currentBannerId = 2
            checkPermissionAndPickImage()
        }

        findViewById<Button>(R.id.btnUploadBanner3).setOnClickListener {
            currentBannerId = 3
            checkPermissionAndPickImage()
        }

        // Rich text formatting buttons
        findViewById<Button>(R.id.btnBold).setOnClickListener {
            richEditor.setBold()
        }

        findViewById<Button>(R.id.btnItalic).setOnClickListener {
            richEditor.setItalic()
        }

        findViewById<Button>(R.id.btnUnderline).setOnClickListener {
            richEditor.setUnderline()
        }

        findViewById<Button>(R.id.btnH1).setOnClickListener {
            richEditor.setHeading(1)
        }

        findViewById<Button>(R.id.btnH2).setOnClickListener {
            richEditor.setHeading(2)
        }

        findViewById<Button>(R.id.btnBullets).setOnClickListener {
            richEditor.setBullets()
        }

        findViewById<Button>(R.id.btnNumbers).setOnClickListener {
            richEditor.setNumbers()
        }

        findViewById<Button>(R.id.btnInsertImage).setOnClickListener {
            showImageUrlDialog()
        }

        findViewById<Button>(R.id.btnInsertLink).setOnClickListener {
            showLinkDialog()
        }

        findViewById<Button>(R.id.btnSaveCMS).setOnClickListener {
            val content = richEditor.html ?: ""
            if (content.isNotEmpty()) {
                database.reference.child("CMS").child("home_content").setValue(content)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Home Page Updated!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Load existing content
        database.reference.child("CMS").child("home_content").get().addOnSuccessListener {
            if (it.exists()) {
                richEditor.html = it.value.toString()
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

        val timestamp = System.currentTimeMillis()
        val storageRef = storage.reference
            .child("banners")
            .child("banner_${currentBannerId}_${timestamp}.jpg")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveBannerUrlToDatabase(downloadUri.toString())
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Failed to get download URL: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload Failed: ${exception.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun saveBannerUrlToDatabase(url: String) {
        database.reference.child("Banners").child("banner$currentBannerId").setValue(url)
            .addOnSuccessListener {
                Toast.makeText(this, "Banner $currentBannerId Saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to save banner: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun checkPermissionAndPickImage() {
        // For Android 13+ (API 33/TIRAMISU and higher), use READ_MEDIA_IMAGES
        // For older versions (API 32 and below), use READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                pickImageLauncher.launch("image/*")
            }

            shouldShowRequestPermissionRationale(permission) -> {
                // Show explanation why permission is needed
                Toast.makeText(this, "Permission needed to upload banners", Toast.LENGTH_LONG)
                    .show()
                requestPermissionLauncher.launch(permission)
            }

            else -> {
                // Request permission
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadDashboardStats() {
        val tvTotalStudents = findViewById<TextView>(R.id.tvTotalStudents)
        val tvActiveChats = findViewById<TextView>(R.id.tvActiveChats)

        // Count total students (single read)
        database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var studentCount = 0
                    for (userSnapshot in snapshot.children) {
                        val role = userSnapshot.child("role").value?.toString()
                        if (role == "student") {
                            studentCount++
                        }
                    }
                    tvTotalStudents.text = studentCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Count active chats (single read)
        database.reference.child("Chats")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatCount = snapshot.childrenCount.toInt()
                    tvActiveChats.text = chatCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showImageUrlDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Insert Image")

        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(50, 20, 50, 20)

        val input = EditText(this)
        input.hint = "Enter image URL (http:// or https://)"
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Insert") { dialog, _ ->
            val url = input.text.toString().trim()
            if (url.isNotEmpty() && isValidImageUrl(url)) {
                // Use screen width for responsive image sizing
                val screenWidth = resources.displayMetrics.widthPixels
                val imageWidth = (screenWidth * 0.9).toInt() // 90% of screen width
                richEditor.insertImage(url, "image", imageWidth)
            } else {
                Toast.makeText(
                    this,
                    "Please enter a valid image URL (http:// or https://)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showLinkDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Insert Link")

        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(50, 20, 50, 20)

        val urlInput = EditText(this)
        urlInput.hint = "Enter URL (http:// or https://)"
        container.addView(urlInput)

        val textInput = EditText(this)
        textInput.hint = "Enter link text"
        container.addView(textInput)

        builder.setView(container)

        builder.setPositiveButton("Insert") { dialog, _ ->
            val url = urlInput.text.toString().trim()
            val text = textInput.text.toString().trim()
            if (url.isNotEmpty() && text.isNotEmpty() && isValidUrl(url)) {
                richEditor.insertLink(url, text)
            } else {
                Toast.makeText(
                    this,
                    "Please enter valid URL (http:// or https://) and link text",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun isValidImageUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
