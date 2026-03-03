package com.tripplanner.mediwings

import android.Manifest
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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

/**
 * HomeActivity - Unified home screen for both student and worker roles.
 *
 * Role is determined by:
 * 1. Primary: Firestore users/{uid}.role
 * 2. Fallback: Intent extra USER_ROLE
 *
 * Layout: activity_worker_home.xml (worker architecture base)
 * Student-only sections (status_view, universities_view, student_docs_section,
 * student_home_section) are shown/hidden at runtime based on role.
 */
class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

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
    private lateinit var navView: NavigationView

    private var userRole = ROLE_STUDENT
    private var uploadType = ""
    private val scrollHandler = Handler(Looper.getMainLooper())
    private var currentBannerPosition = 0

    companion object {
        private const val TAG = "HomeActivity"
        const val EXTRA_USER_ROLE = "USER_ROLE"
        const val ROLE_WORKER = "worker"
        const val ROLE_STUDENT = "student"
        private const val DB_WORKERS = "workers"
        private const val DB_USERS = "users"
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadDocument(it, uploadType) }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) pickImageLauncher.launch("*/*")
            else Toast.makeText(this, "Permission denied. Cannot upload files.", Toast.LENGTH_LONG).show()
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

        val goldColor = ContextCompat.getColor(this, R.color.gold_premium)
        toolbar.setTitleTextColor(goldColor)
        toolbar.navigationIcon?.setTint(goldColor)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = goldColor
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        homeView = findViewById(R.id.home_view)
        docsView = findViewById(R.id.docs_view)
        profileView = findViewById(R.id.profile_view)
        statusView = findViewById(R.id.status_view)
        universitiesView = findViewById(R.id.universities_view)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Use intent extra as fallback role while Firestore loads
        userRole = intent.getStringExtra(EXTRA_USER_ROLE) ?: ROLE_STUDENT

        // Load authoritative role from Firestore, then initialize
        loadRoleFromFirestore()
    }

    private fun loadRoleFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            initializeActivity()
            return
        }

        firestore.collection(DB_USERS).document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val role = doc.getString("role")
                    if (!role.isNullOrEmpty()) userRole = role
                }
                initializeActivity()
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to load role from Firestore, using fallback: $userRole")
                initializeActivity()
            }
    }

    private fun initializeActivity() {
        configureForRole()
        setupBottomNav()
        loadUserData()
        setupProfileSection()
        if (userRole == ROLE_WORKER) {
            setupWorkerFeatures()
        } else {
            setupStudentFeatures()
        }
        showView("home")
    }

    private fun configureForRole() {
        val workerDocsSection = findViewById<View>(R.id.worker_docs_section)
        val studentDocsSection = findViewById<View>(R.id.student_docs_section)
        val workerHomeActions = findViewById<View>(R.id.worker_home_actions)
        val studentHomeSection = findViewById<View>(R.id.student_home_section)

        if (userRole == ROLE_WORKER) {
            supportActionBar?.title = "MediWings Worker"
            workerDocsSection?.visibility = View.VISIBLE
            studentDocsSection?.visibility = View.GONE
            workerHomeActions?.visibility = View.VISIBLE
            studentHomeSection?.visibility = View.GONE
            // Hide student-only bottom nav item
            bottomNav.menu.findItem(R.id.nav_status)?.isVisible = false
            // Hide student-only drawer items
            navView.menu.findItem(R.id.nav_universities)?.isVisible = false
            navView.menu.findItem(R.id.nav_status_drawer)?.isVisible = false
        } else {
            supportActionBar?.title = "MediWings Student"
            workerDocsSection?.visibility = View.GONE
            studentDocsSection?.visibility = View.VISIBLE
            workerHomeActions?.visibility = View.GONE
            studentHomeSection?.visibility = View.VISIBLE
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_tab -> { showView("home"); true }
                R.id.nav_docs -> { showView("docs"); true }
                R.id.nav_chat -> { openChat(); true }
                R.id.nav_profile -> { showView("profile"); true }
                R.id.nav_status -> { showView("status"); true }
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
            "status" -> if (userRole == ROLE_STUDENT) statusView.visibility = View.VISIBLE
            "universities" -> if (userRole == ROLE_STUDENT) universitiesView.visibility = View.VISIBLE
        }
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("IS_ADMIN", false)
        intent.putExtra(EXTRA_USER_ROLE, userRole)
        startActivity(intent)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection(DB_USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading user data from Firestore", error)
                    loadUserDataFromRealtimeDB(userId)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name")
                        ?: if (userRole == ROLE_WORKER) "Worker" else "Student"
                    val email = snapshot.getString("email") ?: ""
                    val mobile = snapshot.getString("mobile") ?: "Not provided"
                    val photoUrl = snapshot.getString("photoUrl")
                    updateUserUI(name, email, mobile, photoUrl)
                } else {
                    loadUserDataFromRealtimeDB(userId)
                }
            }
    }

    private fun loadUserDataFromRealtimeDB(userId: String) {
        val path = if (userRole == ROLE_WORKER) DB_WORKERS else DB_USERS
        database.child(path).child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").value?.toString()
                        ?: if (userRole == ROLE_WORKER) "Worker" else "Student"
                    val email = snapshot.child("email").value?.toString() ?: ""
                    val mobile = snapshot.child("mobile").value?.toString() ?: "Not provided"
                    val profilePic = snapshot.child("profilePic").value?.toString()
                    updateUserUI(name, email, mobile, profilePic)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading from Realtime Database", error.toException())
                }
            })
    }

    private fun updateUserUI(name: String, email: String, mobile: String, photoUrl: String?) {
        if (isFinishing || isDestroyed) return

        val headerView = navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvNavHeaderName).text = name
        headerView.findViewById<TextView>(R.id.tvNavHeaderEmail).text = email
        val ivNavProfile = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfile)

        findViewById<TextView>(R.id.tvWelcome).text = "Hello, $name!"
        findViewById<TextView>(R.id.tvProfileName).text = name
        findViewById<TextView>(R.id.tvProfileEmail).text = email
        findViewById<TextView>(R.id.tvProfileMobile).text = mobile

        loadProfileImages(photoUrl, ivNavProfile, findViewById(R.id.ivProfilePhoto))
    }

    private fun loadProfileImages(photoUrl: String?, vararg imageViews: ImageView?) {
        if (isFinishing || isDestroyed) return
        imageViews.forEach { iv ->
            iv ?: return@forEach
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(this).load(photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop().into(iv)
            } else {
                Glide.with(this).load(R.drawable.ic_default_avatar).circleCrop().into(iv)
            }
        }
    }

    private fun setupProfileSection() {
        findViewById<Button>(R.id.btnUploadProfilePhoto)?.setOnClickListener {
            uploadType = "profile"
            requestUpload()
        }
        findViewById<Button>(R.id.btnEditProfile)?.setOnClickListener {
            showEditProfileDialog()
        }
    }

    // ---- Worker-specific features ----

    private fun setupWorkerFeatures() {
        findViewById<CardView>(R.id.cardSubmitResume)?.setOnClickListener {
            uploadType = "resume"; requestUpload()
        }
        findViewById<CardView>(R.id.cardFindOpportunities)?.setOnClickListener {
            Toast.makeText(this, "Opportunities feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.cardNotifications)?.setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnUploadResume)?.setOnClickListener {
            uploadType = "resume"; requestUpload()
        }
        findViewById<Button>(R.id.btnUploadCertificate)?.setOnClickListener {
            uploadType = "certificate"; requestUpload()
        }
        loadWorkerDocuments()
    }

    private fun loadWorkerDocuments() {
        val userId = auth.currentUser?.uid ?: return
        database.child(DB_WORKERS).child(userId).child("documents")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val resumeUrl = snapshot.child("resume").value?.toString()
                    if (!resumeUrl.isNullOrEmpty()) {
                        findViewById<ImageView>(R.id.ivResumePreview)?.visibility = View.VISIBLE
                        findViewById<Button>(R.id.btnUploadResume)?.text = "✓ Uploaded - Tap to Replace"
                        findViewById<Button>(R.id.btnUploadResume)?.setBackgroundColor(
                            ContextCompat.getColor(this@HomeActivity, R.color.success)
                        )
                    }
                    val certUrl = snapshot.child("certificate").value?.toString()
                    if (!certUrl.isNullOrEmpty()) {
                        findViewById<ImageView>(R.id.ivCertificatePreview)?.visibility = View.VISIBLE
                        findViewById<Button>(R.id.btnUploadCertificate)?.text = "✓ Uploaded - Tap to Replace"
                        findViewById<Button>(R.id.btnUploadCertificate)?.setBackgroundColor(
                            ContextCompat.getColor(this@HomeActivity, R.color.success)
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ---- Student-specific features ----

    private fun setupStudentFeatures() {
        // Quick action navigation cards
        findViewById<View>(R.id.cardViewDocuments)?.setOnClickListener {
            showView("docs"); bottomNav.selectedItemId = R.id.nav_docs
        }
        findViewById<View>(R.id.cardViewUniversities)?.setOnClickListener {
            showView("universities")
        }
        findViewById<View>(R.id.cardViewTracking)?.setOnClickListener {
            showView("status"); bottomNav.selectedItemId = R.id.nav_status
        }
        setupStudentDocUploads()
        setupBannersRecyclerView()
        loadUniversities()
        setupStatusTimeline()
    }

    private fun setupStudentDocUploads() {
        val userId = auth.currentUser?.uid ?: return
        database.child(DB_USERS).child(userId).child("documents")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    loadDocPreview(snapshot, "photos", R.id.ivPhotosPreview, R.id.btnUploadPhotos)
                    loadDocPreview(snapshot, "aadhar", R.id.ivAadharPreview, R.id.btnUploadAadhar)
                    loadDocPreview(snapshot, "passport", R.id.ivPassportPreview, R.id.btnUploadPassport)
                    loadDocPreview(snapshot, "hiv", R.id.ivHIVPreview, R.id.btnUploadHIV)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        findViewById<Button>(R.id.btnUploadPhotos)?.setOnClickListener {
            uploadType = "photos"; requestUpload()
        }
        findViewById<Button>(R.id.btnUploadAadhar)?.setOnClickListener {
            uploadType = "aadhar"; requestUpload()
        }
        findViewById<Button>(R.id.btnUploadPassport)?.setOnClickListener {
            uploadType = "passport"; requestUpload()
        }
        findViewById<Button>(R.id.btnUploadHIV)?.setOnClickListener {
            uploadType = "hiv"; requestUpload()
        }
    }

    private fun loadDocPreview(snapshot: DataSnapshot, docType: String, imageViewId: Int, buttonId: Int) {
        val url = snapshot.child(docType).value?.toString()
        if (!url.isNullOrEmpty()) {
            val imageView = findViewById<ImageView>(imageViewId)
            val button = findViewById<Button>(buttonId)
            imageView?.visibility = View.VISIBLE
            imageView?.let { Glide.with(this).load(url).centerCrop().into(it) }
            button?.text = "✓ Uploaded"
        }
    }

    private fun setupBannersRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvBannersHome) ?: return
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val bannersList = mutableListOf<String>()
        val adapter = BannerHomeAdapter(bannersList)
        rv.adapter = adapter

        database.child("Banners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bannersList.clear()
                for (data in snapshot.children) {
                    val url = data.value?.toString()
                    if (!url.isNullOrEmpty()) bannersList.add(url)
                }
                adapter.notifyDataSetChanged()
                if (bannersList.size > 1) startAutoScroll(rv, bannersList.size)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startAutoScroll(rv: RecyclerView, itemCount: Int) {
        scrollHandler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                currentBannerPosition = (currentBannerPosition + 1) % itemCount
                rv.smoothScrollToPosition(currentBannerPosition)
                scrollHandler.postDelayed(this, 4000)
            }
        }
        scrollHandler.postDelayed(runnable, 4000)
    }

    private fun loadUniversities() {
        val rv = findViewById<RecyclerView>(R.id.rvUniversitiesHome) ?: return
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

    private fun setupStatusTimeline() {
        val userId = auth.currentUser?.uid ?: return
        // Primary: Tracking/{uid}
        database.child("Tracking").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateTimelineStep(findViewById(R.id.step_application), "Application", snapshot.child("application"))
                updateTimelineStep(findViewById(R.id.step_documents), "Documents", snapshot.child("docs"))
                updateTimelineStep(findViewById(R.id.step_verification), "Admission", snapshot.child("admission"))
                updateTimelineStep(findViewById(R.id.step_visa), "Visa", snapshot.child("visa"))
                updateTimelineStep(findViewById(R.id.step_flight), "Flight", snapshot.child("flight"))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        // Legacy fallback: users/{uid}/tracking
        database.child(DB_USERS).child(userId).child("tracking")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        updateTimelineStep(findViewById(R.id.step_application), "Application", snapshot.child("step1"))
                        updateTimelineStep(findViewById(R.id.step_documents), "Documents", snapshot.child("step2"))
                        updateTimelineStep(findViewById(R.id.step_verification), "Admission", snapshot.child("step3"))
                        updateTimelineStep(findViewById(R.id.step_visa), "Visa", snapshot.child("step4"))
                        updateTimelineStep(findViewById(R.id.step_flight), "Flight", snapshot.child("step5"))
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateTimelineStep(view: View?, title: String, data: DataSnapshot) {
        if (view == null) return
        val status = data.child("status").value?.toString() ?: "pending"
        val isDone = status == "completed" || data.child("status").value == true
        val date = data.child("date").value?.toString()
            ?: data.child("completedDate").value?.toString() ?: ""
        val remark = data.child("remark").value?.toString() ?: ""
        val imageUrl = data.child("image").value?.toString() ?: ""

        view.findViewById<TextView>(R.id.tvStepTitle)?.text = title
        val tvStatus = view.findViewById<TextView>(R.id.tvStepStatus)
        val tvDate = view.findViewById<TextView>(R.id.tvStepDate)
        val tvRemark = view.findViewById<TextView>(R.id.tvStepRemark)
        val indicator = view.findViewById<CardView>(R.id.cvIndicator)
        val line = view.findViewById<View>(R.id.view_line)
        val ivStepImage = view.findViewById<ImageView>(R.id.ivStepImage)

        val completedColor = ContextCompat.getColor(this, R.color.timeline_completed)
        val pendingColor = ContextCompat.getColor(this, R.color.timeline_pending)

        if (isDone) {
            tvStatus?.text = "Completed"
            tvStatus?.setTextColor(completedColor)
            indicator?.setCardBackgroundColor(completedColor)
            line?.setBackgroundColor(completedColor)
            tvDate?.visibility = View.VISIBLE
            tvDate?.text = date
        } else {
            tvStatus?.text = "Pending"
            tvStatus?.setTextColor(pendingColor)
            indicator?.setCardBackgroundColor(pendingColor)
            line?.setBackgroundColor(pendingColor)
            tvDate?.visibility = View.GONE
            if (remark.isNotEmpty()) {
                tvRemark?.visibility = View.VISIBLE
                tvRemark?.text = remark
            }
        }

        if (imageUrl.isNotEmpty()) {
            ivStepImage?.visibility = View.VISIBLE
            ivStepImage?.let { Glide.with(this).load(imageUrl).into(it) }
        } else {
            ivStepImage?.visibility = View.GONE
        }
    }

    // ---- Upload flow (unified) ----

    private fun requestUpload() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("*/*")
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun uploadDocument(uri: Uri, docType: String) {
        val userId = auth.currentUser?.uid ?: return

        // Validate file size (<1MB)
        val fileSize = contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
        if (fileSize > 1024 * 1024) {
            Toast.makeText(
                this,
                "File too large! Please select a file smaller than 1MB (${fileSize / 1024}KB selected)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Uploading...")
            .setMessage("Please wait while we upload your file")
            .setCancelable(false)
            .create()
        progressDialog.show()

        val storagePath = if (userRole == ROLE_WORKER) {
            "$DB_WORKERS/$userId/$docType/${System.currentTimeMillis()}"
        } else {
            "uploads/$userId/${docType}_${System.currentTimeMillis()}.jpg"
        }

        val storageRef = storage.reference.child(storagePath)
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    handleUploadSuccess(userId, docType, url, progressDialog)
                }
            }
            .addOnFailureListener { e ->
                if (!isFinishing && !isDestroyed) {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---- Helpers ----

    /** Returns a Map with the updatedAt timestamp added. */
    private fun withTimestamp(map: Map<String, Any>): Map<String, Any> =
        map + mapOf("updatedAt" to com.google.firebase.Timestamp.now())

    private fun handleUploadSuccess(
        userId: String,
        docType: String,
        url: String,
        progressDialog: AlertDialog
    ) {
        if (docType == "profile") {
            // Update Firestore with new photoUrl
            val firestoreUpdates = withTimestamp(mapOf("photoUrl" to url))
            firestore.collection(DB_USERS).document(userId)
                .update(firestoreUpdates)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile photo updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update photoUrl in Firestore", e)
                }
            // Also update Realtime DB
            val dbPath = if (userRole == ROLE_WORKER) "$DB_WORKERS/$userId/profilePic"
            else "$DB_USERS/$userId/profilePic"
            database.child(dbPath).setValue(url)

            if (!isFinishing && !isDestroyed) {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                loadUserData()
            }
        } else {
            // Document upload
            val dbPath = if (userRole == ROLE_WORKER) "$DB_WORKERS/$userId/documents/$docType"
            else "$DB_USERS/$userId/documents/$docType"
            database.child(dbPath).setValue(url)

            if (!isFinishing && !isDestroyed) {
                progressDialog.dismiss()
                val label = docType.replaceFirstChar { it.uppercase() }
                Toast.makeText(this, "$label uploaded successfully!", Toast.LENGTH_SHORT).show()
                if (userRole == ROLE_WORKER) loadWorkerDocuments()
            }
        }
    }

    // ---- Profile edit ----

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val nameInput = EditText(this)
        nameInput.hint = "Name"
        nameInput.setText(findViewById<TextView>(R.id.tvProfileName)?.text)
        layout.addView(nameInput)

        val mobileInput = EditText(this)
        mobileInput.hint = "Mobile"
        mobileInput.setText(findViewById<TextView>(R.id.tvProfileMobile)?.text)
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
            val updates = hashMapOf<String, Any>("name" to name, "mobile" to mobile)
            val dbPath = if (userRole == ROLE_WORKER) DB_WORKERS else DB_USERS
            database.child(dbPath).child(userId).updateChildren(updates)

            val firestoreUpdates = withTimestamp(updates)
            firestore.collection(DB_USERS).document(userId)
                .update(firestoreUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    loadUserData()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update profile in Firestore", e)
                    Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // ---- Drawer navigation ----

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home_drawer -> {
                showView("home")
                bottomNav.selectedItemId = R.id.nav_home_tab
            }
            R.id.nav_documents -> {
                showView("docs")
                bottomNav.selectedItemId = R.id.nav_docs
            }
            R.id.nav_chat_drawer -> openChat()
            R.id.nav_profile_drawer -> {
                showView("profile")
                bottomNav.selectedItemId = R.id.nav_profile
            }
            R.id.nav_universities -> showView("universities")
            R.id.nav_status_drawer -> {
                showView("status")
                bottomNav.selectedItemId = R.id.nav_status
            }
            R.id.nav_logout -> {
                auth.signOut()
                getSharedPreferences("MediWingsPrefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Suppress("DEPRECATION")
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
    }

    // ---- Inner Adapters ----

    private class UniversityHomeAdapter(
        private val list: List<UniversitiesListActivity.University>
    ) : RecyclerView.Adapter<UniversityHomeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivUniItemPhoto)
            val tvName: TextView = view.findViewById(R.id.tvUniItemName)
            val tvDetails: TextView = view.findViewById(R.id.tvUniItemDetails)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_university, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uni = list[position]
            holder.tvName.text = uni.name
            holder.tvDetails.text = uni.details
            if (uni.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(uni.imageUrl).into(holder.ivPhoto)
            }
        }

        override fun getItemCount() = list.size
    }

    private class BannerHomeAdapter(
        private val list: List<String>
    ) : RecyclerView.Adapter<BannerHomeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivBanner: ImageView = view.findViewById(R.id.ivBannerHome)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_banner_home, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context).load(list[position]).centerCrop().into(holder.ivBanner)
        }

        override fun getItemCount() = list.size
    }
}
