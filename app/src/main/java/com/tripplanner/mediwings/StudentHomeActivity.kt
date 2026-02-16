package com.tripplanner.mediwings

import android.Manifest
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
    
    private var uploadType = "" 
    
    companion object {
        private const val TAG = "StudentHomeActivity"
    }

    private lateinit var rvBannersHome: RecyclerView
    private val scrollHandler = Handler(Looper.getMainLooper())
    private var currentBannerPosition = 0 

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it, uploadType) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
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
                    val htmlData = """
                        <html>
                        <head>
                            <style>
                                body { font-family: 'Roboto', sans-serif; line-height: 1.6; color: #333; padding: 10px; }
                                h1 { color: #1a237e; border-bottom: 2px solid #FFD700; }
                                .highlight { background: #fffde7; padding: 2px 5px; }
                            </style>
                        </head>
                        <body>$content</body>
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
        
        database.child("Banners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    bannersList.clear()
                    for (data in snapshot.children) {
                        val url = data.value?.toString()
                        if (!url.isNullOrEmpty()) bannersList.add(url)
                    }
                    adapter.notifyDataSetChanged()
                    if (bannersList.isNotEmpty()) startAutoScroll(bannersList.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading banners", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    private fun startAutoScroll(itemCount: Int) {
        scrollHandler.removeCallbacksAndMessages(null)
        if (itemCount <= 1) return 
        
        val runnable = object : Runnable {
            override fun run() {
                currentBannerPosition++
                if (currentBannerPosition >= itemCount) currentBannerPosition = 0
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
                R.id.bottom_universities -> { showView("universities"); true }
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
        findViewById<View>(R.id.cardViewDocuments)?.setOnClickListener {
            showView("docs")
            bottomNav.selectedItemId = R.id.bottom_docs
        }
        findViewById<View>(R.id.cardViewUniversities)?.setOnClickListener {
            showView("universities")
        }
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

        // Try Firestore first
        firestore.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("name") ?: ""
                val email = snapshot.getString("email") ?: ""
                val mobile = snapshot.getString("mobile") ?: ""
                val photoUrl = snapshot.getString("photoUrl")

                tvName?.text = name
                tvEmail?.text = email
                tvMobile?.text = mobile
                tvNavName?.text = name
                tvNavEmail?.text = email
                tvWelcome?.text = "Hello, $name!"

                if (!photoUrl.isNullOrEmpty()) {
                    updateProfileImages(photoUrl, ivProfile, ivNavProfile)
                }
            } else {
                // Fallback to Realtime DB
                loadUserDataFromRealtimeDB(navView, userId)
            }
        }
    }
    
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
                    val mobile = snapshot.child("mobile").value?.toString() ?: ""
                    val pic = snapshot.child("profilePic").value?.toString()

                    tvName?.text = name
                    tvEmail?.text = email
                    tvMobile?.text = mobile
                    tvNavName?.text = name
                    tvNavEmail?.text = email
                    tvWelcome?.text = "Hello, $name!"

                    if (!pic.isNullOrEmpty()) {
                        updateProfileImages(pic, ivProfile, ivNavProfile)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateProfileImages(url: String, ivProfile: ImageView?, ivNavProfile: ImageView?) {
        if (!isFinishing && !isDestroyed) {
            ivProfile?.let {
                Glide.with(this).load(url).placeholder(R.drawable.ic_default_avatar).circleCrop().into(it)
            }
            ivNavProfile?.let {
                Glide.with(this).load(url).placeholder(R.drawable.ic_default_avatar).circleCrop().into(it)
            }
        }
    }

    private fun setupProfileUpdate() {
        findViewById<Button>(R.id.btnUploadProfilePhoto)?.setOnClickListener {
            uploadType = "profile"
            checkPermissionAndPickImage()
        }
        findViewById<Button>(R.id.btnEditProfile)?.setOnClickListener {
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
            
            val updates = hashMapOf<String, Any>("name" to name, "mobile" to mobile)
            database.child("users").child(userId).updateChildren(updates)
            firestore.collection("users").document(userId).update(updates)
                .addOnSuccessListener { Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show() }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

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
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("documents").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loadDocumentPreview(snapshot, "photos", R.id.ivPhotosPreview, R.id.btnUploadPhotos)
                loadDocumentPreview(snapshot, "aadhar", R.id.ivAadharPreview, R.id.btnUploadAadhar)
                loadDocumentPreview(snapshot, "passport", R.id.ivPassportPreview, R.id.btnUploadPassport)
                loadDocumentPreview(snapshot, "hiv", R.id.ivHIVPreview, R.id.btnUploadHIV)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        findViewById<Button>(R.id.btnUploadPhotos)?.setOnClickListener { uploadType = "photos"; checkPermissionAndPickImage() }
        findViewById<Button>(R.id.btnUploadAadhar)?.setOnClickListener { uploadType = "aadhar"; checkPermissionAndPickImage() }
        findViewById<Button>(R.id.btnUploadPassport)?.setOnClickListener { uploadType = "passport"; checkPermissionAndPickImage() }
        findViewById<Button>(R.id.btnUploadHIV)?.setOnClickListener { uploadType = "hiv"; checkPermissionAndPickImage() }
    }
    
    private fun loadDocumentPreview(snapshot: DataSnapshot, docType: String, imageViewId: Int, buttonId: Int) {
        val url = snapshot.child(docType).value?.toString()
        if (!url.isNullOrEmpty()) {
            val imageView = findViewById<ImageView>(imageViewId)
            val button = findViewById<Button>(buttonId)
            imageView?.visibility = View.VISIBLE
            Glide.with(this).load(url).centerCrop().into(imageView)
            button?.text = "✓ Uploaded"
        }
    }
    
    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    @Suppress("DEPRECATION")
    private fun uploadImage(uri: Uri, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading...")
        progressDialog.show()
        
        val filename = "${type}_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("uploads").child(userId).child(filename)
        
        storageRef.putFile(uri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                progressDialog.dismiss()
                val url = downloadUri.toString()
                if (type == "profile") {
                    database.child("users").child(userId).child("profilePic").setValue(url)
                    firestore.collection("users").document(userId).update("photoUrl", url)
                } else {
                    database.child("users").child(userId).child("documents").child(type).setValue(url)
                }
                Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { progressDialog.dismiss() }
    }

    private fun setupStatusTimeline() {
        val userId = auth.currentUser?.uid ?: return
        
        // Read from Tracking/{uid} as per the requirements
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
        
        // Also check legacy path for backwards compatibility
        database.child("users").child(userId).child("tracking").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    updateTimelineStep(findViewById(R.id.step_application), "Application", snapshot.child("step1"))
                    updateTimelineStep(findViewById(R.id.step_documents), "Documents", snapshot.child("step2"))
                    updateTimelineStep(findViewById(R.id.step_verification), "Admission", snapshot.child("step3"))
                    val visaData = snapshot.child("step4")
                    updateTimelineStep(findViewById(R.id.step_visa), "Visa", visaData)
                    updateTimelineStep(findViewById(R.id.visa_step_applied), "Visa Applied", visaData.child("applied"))
                    updateTimelineStep(findViewById(R.id.visa_step_processing), "Processing", visaData.child("processing"))
                    updateTimelineStep(findViewById(R.id.visa_step_approved), "Approved", visaData.child("approved"))
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
        val date = data.child("date").value?.toString() ?: data.child("completedDate").value?.toString() ?: ""
        val remark = data.child("remark").value?.toString() ?: ""
        val imageUrl = data.child("image").value?.toString() ?: ""

        view.findViewById<TextView>(R.id.tvStepTitle).text = title
        val tvStatus = view.findViewById<TextView>(R.id.tvStepStatus)
        val tvDate = view.findViewById<TextView>(R.id.tvStepDate)
        val tvRemark = view.findViewById<TextView>(R.id.tvStepRemark)
        val indicator = view.findViewById<CardView>(R.id.cvIndicator)
        val line = view.findViewById<View>(R.id.view_line)
        val ivStepImage = view.findViewById<ImageView>(R.id.ivStepImage)
        
        // Use timeline colors from resources: #4CAF50 for completed, #BDBDBD for pending
        val completedColor = ContextCompat.getColor(this, R.color.timeline_completed)
        val pendingColor = ContextCompat.getColor(this, R.color.timeline_pending)

        if (isDone) {
            tvStatus.text = "Completed"
            tvStatus.setTextColor(completedColor)
            indicator.setCardBackgroundColor(completedColor)
            line.setBackgroundColor(completedColor)
            tvDate.visibility = View.VISIBLE
            tvDate.text = date
        } else {
            tvStatus.text = "Pending"
            tvStatus.setTextColor(pendingColor)
            indicator.setCardBackgroundColor(pendingColor)
            line.setBackgroundColor(pendingColor)
            tvDate.visibility = View.GONE
            if (remark.isNotEmpty()) { tvRemark.visibility = View.VISIBLE; tvRemark.text = remark }
        }
        
        if (imageUrl.isNotEmpty()) {
            ivStepImage.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).into(ivStepImage)
        } else ivStepImage.visibility = View.GONE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> { showView("home"); bottomNav.selectedItemId = R.id.nav_home_tab }
            R.id.nav_chat -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ROLE", "student")
                startActivity(intent)
            }
            R.id.nav_logout -> { auth.signOut(); startActivity(Intent(this, MainActivity::class.java)); finish() }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START) else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollHandler.removeCallbacksAndMessages(null)
    }

    private class UniversityHomeAdapter(private val list: List<UniversitiesListActivity.University>) : RecyclerView.Adapter<UniversityHomeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivUniItemPhoto)
            val tvName: TextView = view.findViewById(R.id.tvUniItemName)
            val tvDetails: TextView = view.findViewById(R.id.tvUniItemDetails)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_university, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uni = list[position]
            holder.tvName.text = uni.name; holder.tvDetails.text = uni.details
            if (uni.imageUrl.isNotEmpty()) Glide.with(holder.itemView.context).load(uni.imageUrl).into(holder.ivPhoto)
        }
        override fun getItemCount() = list.size
    }
    
    private class BannerHomeAdapter(private val list: List<String>) : RecyclerView.Adapter<BannerHomeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivBanner: ImageView = view.findViewById(R.id.ivBannerHome)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_banner_home, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context).load(list[position]).centerCrop().into(holder.ivBanner)
        }
        override fun getItemCount() = list.size
    }
}