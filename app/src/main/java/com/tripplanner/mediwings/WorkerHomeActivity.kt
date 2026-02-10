package com.tripplanner.mediwings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class WorkerHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private lateinit var homeView: View
    private lateinit var docsView: View
    private lateinit var profileView: View
    private lateinit var bottomNav: BottomNavigationView
    
    private var uploadType = "" // "resume", "certificate", "profile"
    
    companion object {
        private const val TAG = "WorkerHomeActivity"
    }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadDocument(it, uploadType) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickImageLauncher.launch("*/*")
        } else {
            Toast.makeText(this, "Permission denied. Cannot upload files.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "MediWings Worker"
        
        val goldColor = ContextCompat.getColor(this, R.color.gold_premium)
        toolbar.setTitleTextColor(goldColor)
        toolbar.navigationIcon?.setTint(goldColor)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = goldColor
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Get view references
        homeView = findViewById(R.id.home_view)
        docsView = findViewById(R.id.docs_view)
        profileView = findViewById(R.id.profile_view)
        bottomNav = findViewById(R.id.bottom_navigation)

        setupBottomNav()
        loadUserData(navView)
        setupQuickActions()
        setupProfileSection()
        loadWorkerDocuments()
        
        // Show home by default
        showView("home")
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showView("home")
                    true
                }
                R.id.nav_docs -> {
                    showView("docs")
                    true
                }
                R.id.nav_chat -> {
                    openChat()
                    true
                }
                R.id.nav_profile -> {
                    showView("profile")
                    true
                }
                else -> false
            }
        }
    }

    private fun showView(viewName: String) {
        homeView.visibility = if (viewName == "home") View.VISIBLE else View.GONE
        docsView.visibility = if (viewName == "docs") View.VISIBLE else View.GONE
        profileView.visibility = if (viewName == "profile") View.VISIBLE else View.GONE
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("IS_ADMIN", false)
        intent.putExtra("USER_ROLE", "worker")
        startActivity(intent)
    }

    private fun loadUserData(navView: NavigationView) {
        val userId = auth.currentUser?.uid ?: return
        
        // Load user data from Firestore (primary source)
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading user data from Firestore", error)
                    Toast.makeText(this, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val name = snapshot.getString("name") ?: "Worker"
                        val email = snapshot.getString("email") ?: ""
                        val mobile = snapshot.getString("mobile") ?: "Not provided"
                        val photoUrl = snapshot.getString("photoUrl")
                        
                        // Update nav header
                        val headerView = navView.getHeaderView(0)
                        headerView.findViewById<TextView>(R.id.tvNavHeaderName).text = name
                        headerView.findViewById<TextView>(R.id.tvNavHeaderEmail).text = email
                        
                        if (!photoUrl.isNullOrEmpty()) {
                            val ivProfilePic = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfile)
                            Glide.with(this@WorkerHomeActivity).load(photoUrl).circleCrop().into(ivProfilePic)
                        }
                        
                        // Update greeting
                        findViewById<TextView>(R.id.tvWelcome).text = "Hello, $name!"
                        
                        // Update profile view
                        findViewById<TextView>(R.id.tvProfileName).text = name
                        findViewById<TextView>(R.id.tvProfileEmail).text = email
                        findViewById<TextView>(R.id.tvProfileMobile).text = mobile
                        
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this@WorkerHomeActivity)
                                .load(photoUrl)
                                .circleCrop()
                                .into(findViewById(R.id.ivProfilePhoto))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data", e)
                        Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "User document does not exist in Firestore, trying Realtime Database")
                    // Fallback to Realtime Database if Firestore data doesn't exist
                    loadUserDataFromRealtimeDB(navView, userId)
                }
            }
    }
    
    // Fallback method to load from Realtime Database
    private fun loadUserDataFromRealtimeDB(navView: NavigationView, userId: String) {
        database.child("workers").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value?.toString() ?: "Worker"
                val email = snapshot.child("email").value?.toString() ?: ""
                val profilePic = snapshot.child("profilePic").value?.toString()
                
                // Update nav header
                val headerView = navView.getHeaderView(0)
                headerView.findViewById<TextView>(R.id.tvNavHeaderName).text = name
                headerView.findViewById<TextView>(R.id.tvNavHeaderEmail).text = email
                
                profilePic?.let {
                    val ivProfilePic = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfile)
                    Glide.with(this@WorkerHomeActivity).load(it).circleCrop().into(ivProfilePic)
                }
                
                // Update greeting
                findViewById<TextView>(R.id.tvWelcome).text = "Hello, $name!"
                
                // Update profile view
                findViewById<TextView>(R.id.tvProfileName).text = name
                findViewById<TextView>(R.id.tvProfileEmail).text = email
                val mobile = snapshot.child("mobile").value?.toString() ?: "Not provided"
                findViewById<TextView>(R.id.tvProfileMobile).text = mobile
                
                profilePic?.let {
                    Glide.with(this@WorkerHomeActivity)
                        .load(it)
                        .circleCrop()
                        .into(findViewById(R.id.ivProfilePhoto))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading from Realtime Database", error.toException())
                Toast.makeText(this@WorkerHomeActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupQuickActions() {
        findViewById<androidx.cardview.widget.CardView>(R.id.cardSubmitResume).setOnClickListener {
            requestUpload("resume")
        }
        
        findViewById<androidx.cardview.widget.CardView>(R.id.cardFindOpportunities).setOnClickListener {
            Toast.makeText(this, "Opportunities feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<androidx.cardview.widget.CardView>(R.id.cardNotifications).setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProfileSection() {
        findViewById<Button>(R.id.btnUploadProfilePhoto).setOnClickListener {
            requestUpload("profile")
        }
        
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }
        
        // Setup document upload buttons
        findViewById<Button>(R.id.btnUploadResume).setOnClickListener {
            requestUpload("resume")
        }
        
        findViewById<Button>(R.id.btnUploadCertificate).setOnClickListener {
            requestUpload("certificate")
        }
    }

    private fun requestUpload(type: String) {
        uploadType = type
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                pickImageLauncher.launch("*/*")
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                pickImageLauncher.launch("*/*")
            }
        }
    }

    private fun uploadDocument(uri: Uri, docType: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Validate file size (<1MB)
        val inputStream = contentResolver.openInputStream(uri)
        val fileSize = inputStream?.available() ?: 0
        inputStream?.close()

        if (fileSize > 1024 * 1024) {
            Toast.makeText(this, "File too large! Please select a file smaller than 1MB (${fileSize / 1024}KB selected)", Toast.LENGTH_LONG).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Uploading...")
            .setMessage("Please wait while we upload your file")
            .setCancelable(false)
            .create()
        progressDialog.show()

        val storageRef = storage.reference.child("workers/$userId/$docType/${System.currentTimeMillis()}")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val path = when (docType) {
                        "profile" -> "workers/$userId/profilePic"
                        else -> "workers/$userId/documents/$docType"
                    }
                    
                    // Update Realtime Database (for backward compatibility)
                    database.child(path).setValue(downloadUri.toString())
                        .addOnSuccessListener {
                            Log.d(TAG, "Document uploaded to Realtime Database")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to save to Realtime Database", e)
                        }
                    
                    // Update Firestore (primary storage) if it's a profile picture
                    if (docType == "profile") {
                        val updates = hashMapOf<String, Any>(
                            "photoUrl" to downloadUri.toString(),
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                        
                        firestore.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Profile picture updated in Firestore successfully")
                                progressDialog.dismiss()
                                Toast.makeText(this, "${docType.capitalize()} uploaded successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Reload data
                                loadWorkerDocuments()
                                loadUserData(findViewById(R.id.nav_view))
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update profile picture in Firestore", e)
                                // If document doesn't exist, try to create it
                                if (e.message?.contains("NOT_FOUND") == true) {
                                    createFirestoreWorkerDocumentWithPhoto(userId, downloadUri.toString())
                                    progressDialog.dismiss()
                                } else {
                                    progressDialog.dismiss()
                                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(this, "${docType.capitalize()} uploaded successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Reload data
                        loadWorkerDocuments()
                        loadUserData(findViewById(R.id.nav_view))
                    }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadWorkerDocuments() {
        val userId = auth.currentUser?.uid ?: return
        
        database.child("workers").child(userId).child("documents")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Load resume preview
                    val resumeUrl = snapshot.child("resume").value?.toString()
                    if (!resumeUrl.isNullOrEmpty()) {
                        findViewById<ImageView>(R.id.ivResumePreview).visibility = View.VISIBLE
                        findViewById<Button>(R.id.btnUploadResume).text = "✓ Uploaded - Tap to Replace"
                        findViewById<Button>(R.id.btnUploadResume).setBackgroundColor(
                            ContextCompat.getColor(this@WorkerHomeActivity, R.color.success)
                        )
                    }
                    
                    // Load certificate preview
                    val certUrl = snapshot.child("certificate").value?.toString()
                    if (!certUrl.isNullOrEmpty()) {
                        findViewById<ImageView>(R.id.ivCertificatePreview).visibility = View.VISIBLE
                        findViewById<Button>(R.id.btnUploadCertificate).text = "✓ Uploaded - Tap to Replace"
                        findViewById<Button>(R.id.btnUploadCertificate).setBackgroundColor(
                            ContextCompat.getColor(this@WorkerHomeActivity, R.color.success)
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Ignore
                }
            })
    }

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        
        val nameInput = EditText(this)
        nameInput.hint = "Name"
        nameInput.setText(findViewById<TextView>(R.id.tvProfileName).text)
        layout.addView(nameInput)
        
        val mobileInput = EditText(this)
        mobileInput.hint = "Mobile"
        mobileInput.setText(findViewById<TextView>(R.id.tvProfileMobile).text)
        layout.addView(mobileInput)
        
        builder.setView(layout)
        
        builder.setPositiveButton("Save") { _, _ ->
            val userId = auth.currentUser?.uid ?: return@setPositiveButton
            val name = nameInput.text.toString().trim()
            val mobile = mobileInput.text.toString().trim()
            
            if (name.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(this, "Please enter name and mobile", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            // Update Realtime Database (for backward compatibility)
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "mobile" to mobile
            )
            
            database.child("workers").child(userId).updateChildren(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated in Realtime Database")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update Realtime Database", e)
                }
            
            // Update Firestore (primary storage)
            val firestoreUpdates = hashMapOf<String, Any>(
                "name" to name,
                "mobile" to mobile,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("users").document(userId)
                .update(firestoreUpdates)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated in Firestore successfully")
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    loadUserData(findViewById(R.id.nav_view))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update profile in Firestore", e)
                    // If document doesn't exist, create it
                    if (e.message?.contains("NOT_FOUND") == true) {
                        createFirestoreWorkerDocument(userId, name, mobile)
                    } else {
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home_drawer -> {
                showView("home")
                bottomNav.selectedItemId = R.id.nav_home
            }
            R.id.nav_documents -> {
                showView("docs")
                bottomNav.selectedItemId = R.id.nav_docs
            }
            R.id.nav_chat_drawer -> {
                openChat()
            }
            R.id.nav_profile_drawer -> {
                showView("profile")
                bottomNav.selectedItemId = R.id.nav_profile
            }
            R.id.nav_logout -> {
                auth.signOut()
                val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    // Helper function to create Firestore user document if it doesn't exist
    private fun createFirestoreWorkerDocument(userId: String, name: String, mobile: String) {
        // Get email from current user
        val email = auth.currentUser?.email ?: ""
        
        val firestoreData = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "mobile" to mobile,
            "photoUrl" to "",
            "role" to "worker",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("users").document(userId)
            .set(firestoreData)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore user document created successfully")
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                loadUserData(findViewById(R.id.nav_view))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create Firestore user document", e)
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Helper function to create Firestore user document with photo
    private fun createFirestoreWorkerDocumentWithPhoto(userId: String, photoUrl: String) {
        // Get email from current user
        val email = auth.currentUser?.email ?: ""
        
        // Try to get name and mobile from Realtime Database
        database.child("workers").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value?.toString() ?: "Worker"
                val mobile = snapshot.child("mobile").value?.toString() ?: ""
                
                val firestoreData = hashMapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "mobile" to mobile,
                    "photoUrl" to photoUrl,
                    "role" to "worker",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("users").document(userId)
                    .set(firestoreData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Firestore user document created with photo successfully")
                        Toast.makeText(this, "Profile picture uploaded successfully!", Toast.LENGTH_SHORT).show()
                        loadUserData(findViewById(R.id.nav_view))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to create Firestore user document with photo", e)
                        Toast.makeText(this, "Failed to save profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get user data from Realtime Database", e)
                Toast.makeText(this, "Failed to save profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
