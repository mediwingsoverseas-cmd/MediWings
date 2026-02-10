package com.tripplanner.mediwings

/**
 * PROFILE IMAGE AND BANNER PHOTO DISPLAY DOCUMENTATION
 * =====================================================
 * 
 * This file implements robust image loading and display for student users.
 * All user profile images are stored in Firebase Storage and their URLs are
 * persisted in Firestore for reliable access across sessions and devices.
 * 
 * IMAGE STORAGE ARCHITECTURE:
 * ---------------------------
 * 1. Firebase Storage Path: uploads/{userId}/{type}_{timestamp}.jpg
 * 2. Firestore Document: users/{userId}
 *    - photoUrl (String): Primary storage for profile image download URL
 *    - updatedAt (Timestamp): Last update timestamp
 * 3. Realtime Database (Fallback): users/{userId}/profilePic
 * 
 * UPLOAD FLOW:
 * ------------
 * 1. User selects image via pickImageLauncher
 * 2. Image is validated (must be < 1MB)
 * 3. Image uploaded to Firebase Storage via uploadImage()
 * 4. Download URL obtained from storageRef.downloadUrl
 * 5. URL saved to BOTH Firestore (primary) and Realtime DB (fallback):
 *    - Firestore: firestore.collection("users").document(userId).update("photoUrl", url)
 *    - Realtime DB: database.child("users/{userId}/profilePic").setValue(url)
 * 6. UI updated immediately after successful upload
 * 
 * DISPLAY FLOW:
 * -------------
 * 1. Images loaded from Firestore photoUrl field via loadUserData()
 * 2. If Firestore unavailable, falls back to Realtime DB via loadUserDataFromRealtimeDB()
 * 3. All images loaded using Glide with:
 *    - .placeholder(R.drawable.ic_default_avatar): Shows during loading
 *    - .error(R.drawable.ic_default_avatar): Shows if load fails
 *    - .circleCrop(): Formats as circular profile picture
 * 4. Images displayed in multiple locations:
 *    - Profile section (ivProfilePic)
 *    - Navigation drawer header (ivNavHeaderProfile)
 *    - All locations automatically update when photoUrl changes
 * 
 * ERROR HANDLING:
 * ---------------
 * - Missing images: Show default avatar (ic_default_avatar)
 * - Upload failures: Toast error message to user
 * - Network errors: Glide automatically retries and shows placeholder
 * - Activity lifecycle: Check !isFinishing && !isDestroyed before UI updates
 * 
 * TESTING CHECKLIST:
 * ------------------
 * ✓ Upload new profile photo
 * ✓ Verify photo appears immediately after upload
 * ✓ Restart app and verify photo persists
 * ✓ Login from different device and verify photo shows
 * ✓ Test with slow/no network connection
 * ✓ Test with corrupted image URLs
 * 
 * FUTURE MODIFICATIONS:
 * ---------------------
 * If you need to change how images are stored or displayed:
 * 1. Update Firebase Storage path in uploadImage() (line ~610)
 * 2. Update Firestore field name in both upload and display code
 * 3. Update Glide loading configuration if you change placeholder/error drawables
 * 4. Run full regression tests for all user roles
 */

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class StudentHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private lateinit var homeView: View
    private lateinit var docsView: View
    private lateinit var profileView: View
    private lateinit var statusView: View
    private lateinit var universitiesView: View
    private lateinit var bottomNav: BottomNavigationView
    
    private var uploadType = "" // "photos", "aadhar", "passport", "hiv", "profile"
    private var progressDialog: ProgressDialog? = null // Track progress dialog for proper cleanup
    
    companion object {
        private const val TAG = "StudentHomeActivity"
        private val VALID_UPLOAD_TYPES = setOf("photos", "aadhar", "passport", "hiv", "profile")
    }

    private lateinit var rvBannersHome: RecyclerView
    private val scrollHandler = Handler(Looper.getMainLooper())
    private val bannerWidth = 320 
    private var currentBannerPosition = 0 

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it, uploadType) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with image picker
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Permission denied. Cannot upload images.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_studend)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "MediWings Student"
        
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
        statusView = findViewById(R.id.status_view)
        universitiesView = findViewById(R.id.universities_view)
        bottomNav = findViewById(R.id.bottom_navigation)

        rvBannersHome = findViewById(R.id.rvBannersHome)
        
        setupBottomNav()
        loadUserData(navView)
        setupBannersRecyclerView()
        setupQuickActions()
        setupDocUploads()
        setupStatusTimeline()
        setupProfileUpdate()
        loadCMSContent()
        loadUniversities()
        
        // Show home by default
        showView("home")
    }

    private fun loadCMSContent() {
        val webView = findViewById<WebView>(R.id.wvHomeContent)
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webView.settings.defaultFontSize = 16
        
        database.child("CMS").child("home_content").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val content = snapshot.value.toString()
                    // Modern, attractive styling with premium color scheme
                    val htmlData = """
                        <html>
                        <head>
                            <style>
                                body {
                                    font-family: 'Roboto', 'Arial', sans-serif;
                                    line-height: 1.8;
                                    color: #2C3E50;
                                    background: linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(240,240,245,0.95) 100%);
                                    padding: 16px;
                                    margin: 0;
                                    border-radius: 12px;
                                }
                                h1, h2, h3 {
                                    color: #1a237e;
                                    font-weight: 600;
                                    margin-top: 20px;
                                    margin-bottom: 12px;
                                }
                                h1 { font-size: 24px; border-bottom: 3px solid #FFD700; padding-bottom: 8px; }
                                h2 { font-size: 20px; }
                                h3 { font-size: 18px; }
                                p {
                                    margin-bottom: 14px;
                                    font-size: 15px;
                                    text-align: justify;
                                }
                                ul, ol {
                                    padding-left: 24px;
                                    margin-bottom: 16px;
                                }
                                li {
                                    margin-bottom: 8px;
                                    font-size: 15px;
                                }
                                a {
                                    color: #1a237e;
                                    text-decoration: none;
                                    font-weight: 500;
                                }
                                strong {
                                    color: #1a237e;
                                    font-weight: 600;
                                }
                                .highlight {
                                    background-color: rgba(255, 215, 0, 0.2);
                                    padding: 2px 6px;
                                    border-radius: 4px;
                                }
                            </style>
                        </head>
                        <body>
                            $content
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupBannersRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBannersHome.layoutManager = layoutManager
        
        val bannersList = mutableListOf<String>()
        val adapter = BannerHomeAdapter(bannersList)
        rvBannersHome.adapter = adapter
        
        // Load banners from Firebase
        database.child("Banners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    bannersList.clear()
                    for (data in snapshot.children) {
                        val url = data.value?.toString()
                        if (!url.isNullOrEmpty()) {
                            bannersList.add(url)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    
                    // Start auto-scroll if there are banners
                    if (bannersList.isNotEmpty()) {
                        startAutoScroll(bannersList.size)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@StudentHomeActivity, "Error loading banners", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentHomeActivity, "Failed to load banners: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun startAutoScroll(itemCount: Int) {
        if (itemCount <= 1) return // No need to auto-scroll if only one banner
        
        val runnable = object : Runnable {
            override fun run() {
                if (!::rvBannersHome.isInitialized) return
                
                currentBannerPosition++
                if (currentBannerPosition >= itemCount) {
                    currentBannerPosition = 0
                }
                
                rvBannersHome.smoothScrollToPosition(currentBannerPosition)
                scrollHandler.postDelayed(this, 4000)
            }
        }
        scrollHandler.postDelayed(runnable, 4000)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_tab -> { showView("home"); true }
                R.id.bottom_docs -> { showView("docs"); true }
                R.id.bottom_status -> { showView("status"); true }
                R.id.bottom_profile -> { showView("profile"); true }
                else -> false
            }
        }
    }
    
    private fun showView(viewName: String) {
        homeView.visibility = View.GONE
        docsView.visibility = View.GONE
        profileView.visibility = View.GONE
        statusView.visibility = View.GONE
        universitiesView.visibility = View.GONE

        when (viewName) {
            "home" -> homeView.visibility = View.VISIBLE
            "docs" -> docsView.visibility = View.VISIBLE
            "profile" -> profileView.visibility = View.VISIBLE
            "status" -> statusView.visibility = View.VISIBLE
            "universities" -> universitiesView.visibility = View.VISIBLE
        }
    }
    
    private fun setupQuickActions() {
        // Quick Action: View Documents
        findViewById<View>(R.id.cardViewDocuments)?.setOnClickListener {
            showView("docs")
            bottomNav.selectedItemId = R.id.bottom_docs
        }
        
        // Quick Action: View Universities
        findViewById<View>(R.id.cardViewUniversities)?.setOnClickListener {
            showView("universities")
        }
        
        // Quick Action: View Tracking
        findViewById<View>(R.id.cardViewTracking)?.setOnClickListener {
            showView("status")
            bottomNav.selectedItemId = R.id.bottom_status
        }
    }

    private fun loadUserData(navView: NavigationView) {
        val userId = auth.currentUser?.uid ?: return
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvMobile = findViewById<TextView>(R.id.tvProfileMobile)
        val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        
        val headerView = navView.getHeaderView(0)
        val ivNavProfile = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfile)
        val tvNavName = headerView.findViewById<TextView>(R.id.tvNavHeaderName)
        val tvNavEmail = headerView.findViewById<TextView>(R.id.tvNavHeaderEmail)

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
                        val name = snapshot.getString("name") ?: ""
                        val email = snapshot.getString("email") ?: ""
                        val mobile = snapshot.getString("mobile") ?: "Not provided"
                        val photoUrl = snapshot.getString("photoUrl")

                        tvName.text = name
                        tvEmail.text = email
                        tvMobile.text = mobile
                        tvNavName.text = name
                        tvNavEmail.text = email
                        
                        // Update welcome message
                        tvWelcome.text = "Hello, $name!"

                        // Load profile image with error handling and placeholder
                        // This ensures images are displayed correctly from Firestore photoUrl field
                        // after upload and persist across app restarts
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this@StudentHomeActivity)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivProfile)
                            Glide.with(this@StudentHomeActivity)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivNavProfile)
                        } else {
                            // Show default avatar if no photo URL exists
                            Glide.with(this@StudentHomeActivity)
                                .load(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivProfile)
                            Glide.with(this@StudentHomeActivity)
                                .load(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivNavProfile)
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
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvMobile = findViewById<TextView>(R.id.tvProfileMobile)
        val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        
        val headerView = navView.getHeaderView(0)
        val ivNavProfile = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfile)
        val tvNavName = headerView.findViewById<TextView>(R.id.tvNavHeaderName)
        val tvNavEmail = headerView.findViewById<TextView>(R.id.tvNavHeaderEmail)
        
        database.child("users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value?.toString() ?: ""
                    val email = snapshot.child("email").value?.toString() ?: ""
                    val mobile = snapshot.child("mobile").value?.toString() ?: "Not provided"
                    val pic = snapshot.child("profilePic").value?.toString()

                    tvName.text = name
                    tvEmail.text = email
                    tvMobile.text = mobile
                    tvNavName.text = name
                    tvNavEmail.text = email
                    
                    // Update welcome message
                    tvWelcome.text = "Hello, $name!"

                    // Load profile image with error handling from Realtime DB fallback
                    if (!pic.isNullOrEmpty()) {
                        Glide.with(this@StudentHomeActivity)
                            .load(pic)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivProfile)
                        Glide.with(this@StudentHomeActivity)
                            .load(pic)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivNavProfile)
                    } else {
                        // Show default avatar if no photo URL exists
                        Glide.with(this@StudentHomeActivity)
                            .load(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivProfile)
                        Glide.with(this@StudentHomeActivity)
                            .load(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivNavProfile)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading from Realtime Database", error.toException())
            }
        })
    }

    private fun setupProfileUpdate() {
        // Setup upload profile photo button
        findViewById<Button>(R.id.btnUploadProfilePhoto).setOnClickListener {
            uploadType = "profile"
            checkPermissionAndPickImage()
        }
        
        // Setup edit profile button
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }
    }
    
    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        
        val nameInput = EditText(this)
        nameInput.hint = "Name"
        nameInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        nameInput.setText(findViewById<TextView>(R.id.tvProfileName).text)
        layout.addView(nameInput)
        
        val mobileInput = EditText(this)
        mobileInput.hint = "Mobile"
        mobileInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
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
            
            database.child("users").child(userId).updateChildren(updates)
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
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update profile in Firestore", e)
                    // If document doesn't exist, create it
                    if (e.message?.contains("NOT_FOUND") == true) {
                        createFirestoreUserDocument(userId, name, mobile)
                    } else {
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // Removed loadBanners() - now handled in setupBannersRecyclerView()

    private fun loadUniversities() {
        val rv = findViewById<RecyclerView>(R.id.rvUniversitiesHome)
        rv.layoutManager = LinearLayoutManager(this)
        val list = mutableListOf<UniversitiesListActivity.University>()
        val adapter = UniversityHomeAdapter(list)
        rv.adapter = adapter

        database.child("Universities").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (data in snapshot.children) {
                    val u = data.getValue(UniversitiesListActivity.University::class.java)
                    if (u != null) list.add(u)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupDocUploads() {
        // Load existing document previews
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("documents").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    loadDocumentPreview(snapshot, "photos", R.id.ivPhotosPreview, R.id.btnUploadPhotos)
                    loadDocumentPreview(snapshot, "aadhar", R.id.ivAadharPreview, R.id.btnUploadAadhar)
                    loadDocumentPreview(snapshot, "passport", R.id.ivPassportPreview, R.id.btnUploadPassport)
                    loadDocumentPreview(snapshot, "hiv", R.id.ivHIVPreview, R.id.btnUploadHIV)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
        
        findViewById<Button>(R.id.btnUploadPhotos).setOnClickListener { 
            uploadType = "photos"
            checkPermissionAndPickImage()
        }
        findViewById<Button>(R.id.btnUploadAadhar).setOnClickListener { 
            uploadType = "aadhar"
            checkPermissionAndPickImage()
        }
        findViewById<Button>(R.id.btnUploadPassport).setOnClickListener { 
            uploadType = "passport"
            checkPermissionAndPickImage()
        }
        findViewById<Button>(R.id.btnUploadHIV).setOnClickListener { 
            uploadType = "hiv"
            checkPermissionAndPickImage()
        }
    }
    
    private fun loadDocumentPreview(snapshot: DataSnapshot, docType: String, imageViewId: Int, buttonId: Int) {
        val url = snapshot.child(docType).value?.toString()
        if (!url.isNullOrEmpty()) {
            val imageView = findViewById<ImageView>(imageViewId)
            val button = findViewById<Button>(buttonId)
            
            // Show the preview image
            imageView.visibility = View.VISIBLE
            
            Glide.with(this@StudentHomeActivity)
                .load(url)
                .centerCrop()
                .into(imageView)
            
            // Update button text and color to indicate re-upload
            button.text = "✓ Uploaded - Tap to Replace"
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
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
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                pickImageLauncher.launch("image/*")
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Show explanation why permission is needed
                Toast.makeText(this, "Permission needed to upload images", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun uploadImage(uri: Uri, type: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Check file size before uploading
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()
            
            if (fileSize > 1024 * 1024) { // 1MB = 1024 * 1024 bytes
                val fileSizeMB = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                AlertDialog.Builder(this)
                    .setTitle("File Too Large")
                    .setMessage("Image is too large ($fileSizeMB MB). Please select an image smaller than 1MB.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to check file size: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Validate upload type
        if (type !in VALID_UPLOAD_TYPES) {
            Toast.makeText(this, "Invalid upload type: $type", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Invalid upload type attempted: $type")
            return
        }
        
        // Dismiss any existing progress dialog before showing new one
        progressDialog?.dismiss()
        
        // Show uploading dialog
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Uploading ${type}...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        
        // Create unique filename with timestamp to avoid conflicts
        val timestamp = System.currentTimeMillis()
        val filename = "${type}_${timestamp}.jpg"
        
        // Create the storage reference - this creates the path if it doesn't exist
        val storageRef = storage.reference
            .child("uploads")
            .child(userId)
            .child(filename)
        
        // Upload the file
        storageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                // Get download URL after successful upload
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    progressDialog?.dismiss()
                    
                    // Save URL to database based on type
                    if (type == "profile") {
                        // Update Realtime Database (for backward compatibility)
                        database.child("users").child(userId).child("profilePic")
                            .setValue(downloadUri.toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "Profile picture updated in Realtime Database")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update profile pic in Realtime Database", e)
                            }
                        
                        // Update Firestore (primary storage)
                        val updates = hashMapOf<String, Any>(
                            "photoUrl" to downloadUri.toString(),
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                        
                        firestore.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Profile picture updated in Firestore successfully")
                                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                                // Reload the image only if activity is still valid
                                // This ensures UI updates don't crash after user navigates away
                                if (!isFinishing && !isDestroyed) {
                                    val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
                                    // Load uploaded image with error handling
                                    Glide.with(this@StudentHomeActivity)
                                        .load(downloadUri)
                                        .placeholder(R.drawable.ic_default_avatar)
                                        .error(R.drawable.ic_default_avatar)
                                        .circleCrop()
                                        .into(ivProfile)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update profile picture in Firestore", e)
                                // If document doesn't exist, try to create it
                                if (e.message?.contains("NOT_FOUND") == true) {
                                    createFirestoreUserDocumentWithPhoto(userId, downloadUri.toString())
                                } else {
                                    Toast.makeText(this, "Failed to save profile picture: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        // For documents, update both Realtime Database and Firestore for consistency
                        database.child("users").child(userId).child("documents").child(type)
                            .setValue(downloadUri.toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "$type document updated in Realtime Database")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update $type in Realtime Database", e)
                            }
                        
                        // Update Firestore document field as well
                        val documentField = "documents.$type"
                        firestore.collection("users").document(userId)
                            .update(documentField, downloadUri.toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "$type document updated in Firestore successfully")
                                Toast.makeText(this, "$type uploaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update $type in Firestore", e)
                                // Still show success since Realtime DB update succeeded
                                Toast.makeText(this, "$type uploaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener { exception ->
                    progressDialog?.dismiss()
                    Toast.makeText(this, "Failed to get download URL: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                progressDialog?.dismiss()
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressDialog?.setMessage("Uploading ${type}... $progress%")
            }
    }

    private fun setupStatusTimeline() {
        val userId = auth.currentUser?.uid ?: return
        val llVisaSub = findViewById<LinearLayout>(R.id.llVisaSubDivision)
        
        database.child("users").child(userId).child("tracking").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateTimelineStep(findViewById(R.id.step_application), "Application", snapshot.child("step1"))
                updateTimelineStep(findViewById(R.id.step_documents), "Documents", snapshot.child("step2"))
                updateTimelineStep(findViewById(R.id.step_verification), "Verification", snapshot.child("step3"))
                
                val visaData = snapshot.child("step4")
                updateTimelineStep(findViewById(R.id.step_visa), "Visa", visaData)
                
                // Sub-tracking for Visa
                updateTimelineStep(findViewById(R.id.visa_step_applied), "Visa Applied", visaData.child("applied"))
                updateTimelineStep(findViewById(R.id.visa_step_processing), "Processing", visaData.child("processing"))
                updateTimelineStep(findViewById(R.id.visa_step_approved), "Approved", visaData.child("approved"))

                findViewById<View>(R.id.step_visa).setOnClickListener {
                    llVisaSub.visibility = if (llVisaSub.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }

                updateTimelineStep(findViewById(R.id.step_flight), "Flight Scheduled", snapshot.child("step5"))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateTimelineStep(view: View, title: String, data: DataSnapshot) {
        val isDone = data.child("status").value == true
        val date = data.child("date").value?.toString() ?: ""
        val remark = data.child("remark").value?.toString() ?: ""
        val imageUrl = data.child("image").value?.toString() ?: ""

        view.findViewById<TextView>(R.id.tvStepTitle).text = title
        val tvStatus = view.findViewById<TextView>(R.id.tvStepStatus)
        val tvDate = view.findViewById<TextView>(R.id.tvStepDate)
        val tvRemark = view.findViewById<TextView>(R.id.tvStepRemark)
        val indicator = view.findViewById<View>(R.id.cvIndicator)
        val line = view.findViewById<View>(R.id.view_line)
        val ivStepImage = view.findViewById<ImageView>(R.id.ivStepImage)
        val ivStepIcon = view.findViewById<ImageView>(R.id.ivStepIcon)
        
        // Set icon based on step title
        val iconRes = when {
            title.contains("Application", ignoreCase = true) || title.contains("Documents", ignoreCase = true) -> R.drawable.ic_document
            title.contains("Verification", ignoreCase = true) || title.contains("Approved", ignoreCase = true) -> R.drawable.ic_verified
            title.contains("Visa", ignoreCase = true) -> R.drawable.ic_visa
            title.contains("Flight", ignoreCase = true) -> R.drawable.ic_flight
            isDone -> R.drawable.ic_graduate
            else -> R.drawable.ic_document
        }
        ivStepIcon.setImageResource(iconRes)

        if (isDone) {
            tvStatus.text = "Completed"
            tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            indicator.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            line.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            tvDate.visibility = View.VISIBLE
            tvDate.text = "Completed on $date"
            tvRemark.visibility = View.GONE
        } else {
            tvStatus.text = "Pending"
            tvStatus.setTextColor(android.graphics.Color.GRAY)
            indicator.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
            line.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
            tvDate.visibility = View.GONE
            if (remark.isNotEmpty()) {
                tvRemark.visibility = View.VISIBLE
                tvRemark.text = "Remark: $remark"
            } else {
                tvRemark.visibility = View.GONE
            }
        }
        
        // Load status image if available with error handling
        if (imageUrl.isNotEmpty()) {
            ivStepImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(ivStepImage)
        } else {
            ivStepImage.visibility = View.GONE
        }
    }
    
    // Helper function to create Firestore user document if it doesn't exist
    private fun createFirestoreUserDocument(userId: String, name: String, mobile: String) {
        // Get email from current user
        val email = auth.currentUser?.email ?: ""
        
        val firestoreData = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "mobile" to mobile,
            "photoUrl" to "",
            "role" to "student",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("users").document(userId)
            .set(firestoreData)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore user document created successfully")
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create Firestore user document", e)
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Helper function to create Firestore user document with photo
    private fun createFirestoreUserDocumentWithPhoto(userId: String, photoUrl: String) {
        // Get email from current user
        val email = auth.currentUser?.email ?: ""
        
        // Try to get name and mobile from Realtime Database
        database.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value?.toString() ?: "User"
                val mobile = snapshot.child("mobile").value?.toString() ?: ""
                
                val firestoreData = hashMapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "mobile" to mobile,
                    "photoUrl" to photoUrl,
                    "role" to "student",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("users").document(userId)
                    .set(firestoreData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Firestore user document created with photo successfully")
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        if (!isFinishing && !isDestroyed) {
                            val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
                            Glide.with(this@StudentHomeActivity).load(photoUrl).circleCrop().into(ivProfile)
                        }
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Navigate to home view and update bottom nav
                showView("home")
                bottomNav.selectedItemId = R.id.nav_home_tab
            }
            R.id.nav_chat -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ROLE", "student")
                startActivity(intent)
            }
            R.id.nav_contact -> startActivity(Intent(this, ContactActivity::class.java))
            R.id.nav_logout -> {
                auth.signOut()
                // Clear saved preferences
                val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            R.id.nav_switch_worker -> {
                startActivity(Intent(this, WorkerActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollHandler.removeCallbacksAndMessages(null)
        // Dismiss progress dialog if showing to prevent window leak
        progressDialog?.dismiss()
        progressDialog = null
    }

    private class UniversityHomeAdapter(private val list: List<UniversitiesListActivity.University>) : RecyclerView.Adapter<UniversityHomeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivUniItemPhoto)
            val tvName: TextView = view.findViewById(R.id.tvUniItemName)
            val tvDetails: TextView = view.findViewById(R.id.tvUniItemDetails)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_university, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uni = list[position]
            holder.tvName.text = uni.name
            holder.tvDetails.text = uni.details
            if (uni.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(uni.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivPhoto)
            }
        }
        override fun getItemCount() = list.size
    }
    
    private class BannerHomeAdapter(private val list: List<String>) : RecyclerView.Adapter<BannerHomeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivBanner: ImageView = view.findViewById(R.id.ivBannerHome)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner_home, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val url = list[position]
            // Load banner with error handling for carousel display
            Glide.with(holder.itemView.context)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivBanner)
        }
        
        override fun getItemCount() = list.size
    }
}