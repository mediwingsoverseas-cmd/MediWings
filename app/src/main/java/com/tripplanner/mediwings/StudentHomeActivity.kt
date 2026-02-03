package com.tripplanner.mediwings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
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
import com.google.firebase.storage.FirebaseStorage

class StudentHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    
    private var uploadType = "" // "photos", "aadhar", "passport", "hiv", "profile"

    private lateinit var bannerScroll: HorizontalScrollView
    private val scrollHandler = Handler(Looper.getMainLooper())
    private val bannerWidth = 320 

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
        storage = FirebaseStorage.getInstance()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "MediWings"
        
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

        findViewById<HorizontalScrollView>(R.id.banner_scroll)?.let {
            bannerScroll = it
            startAutoScroll()
        }

        setupBottomNav()
        loadUserData(navView)
        loadBanners()
        setupDocUploads()
        setupStatusTimeline()
        loadCMSContent()
        setupProfileUpdate()
        loadUniversities()
    }

    private fun loadCMSContent() {
        val webView = findViewById<WebView>(R.id.wvHomeContent)
        database.child("CMS").child("home_content").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val content = snapshot.value.toString()
                    val htmlData = "<html><body style='color:#333333;font-family:serif;line-height:1.6;background-color:transparent;'>$content</body></html>"
                    webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startAutoScroll() {
        val runnable = object : Runnable {
            override fun run() {
                if (!::bannerScroll.isInitialized || bannerScroll.childCount == 0) return
                val innerLayout = bannerScroll.getChildAt(0) as? LinearLayout ?: return
                val maxScroll = innerLayout.width - bannerScroll.width
                if (maxScroll <= 0) return
                val step = (bannerWidth + 12) * resources.displayMetrics.density
                var nextX = bannerScroll.scrollX + step.toInt()
                if (nextX >= maxScroll - 10) nextX = 0
                bannerScroll.smoothScrollTo(nextX, 0)
                scrollHandler.postDelayed(this, 4000)
            }
        }
        scrollHandler.postDelayed(runnable, 4000)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val homeView = findViewById<View>(R.id.home_view)
        val docsView = findViewById<View>(R.id.docs_view)
        val profileView = findViewById<View>(R.id.profile_view)
        val statusView = findViewById<View>(R.id.status_view)
        val uniView = findViewById<View>(R.id.universities_view)

        bottomNav.setOnItemSelectedListener { item ->
            homeView.visibility = View.GONE
            docsView.visibility = View.GONE
            profileView.visibility = View.GONE
            statusView.visibility = View.GONE
            uniView.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_home_tab -> { homeView.visibility = View.VISIBLE; true }
                R.id.nav_universities -> { uniView.visibility = View.VISIBLE; true }
                R.id.bottom_profile -> { profileView.visibility = View.VISIBLE; true }
                R.id.bottom_docs -> { docsView.visibility = View.VISIBLE; true }
                R.id.bottom_status -> { statusView.visibility = View.VISIBLE; true }
                else -> false
            }
        }
    }

    private fun loadUserData(navView: NavigationView) {
        val userId = auth.currentUser?.uid ?: return
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val etName = findViewById<EditText>(R.id.etProfileName)
        val etMobile = findViewById<EditText>(R.id.etProfileMobile)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
        
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

                    tvName.text = name
                    if (!etName.hasFocus()) etName.setText(name)
                    if (!etMobile.hasFocus()) etMobile.setText(mobile)
                    tvEmail.text = email
                    tvNavName.text = name
                    tvNavEmail.text = email

                    if (!pic.isNullOrEmpty()) {
                        Glide.with(this@StudentHomeActivity).load(pic).circleCrop().into(ivProfile)
                        Glide.with(this@StudentHomeActivity).load(pic).circleCrop().into(ivNavProfile)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        ivProfile.setOnClickListener {
            uploadType = "profile"
            checkPermissionAndPickImage()
        }
    }

    private fun setupProfileUpdate() {
        findViewById<Button>(R.id.btnUpdateProfile).setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val name = findViewById<EditText>(R.id.etProfileName).text.toString().trim()
            val mobile = findViewById<EditText>(R.id.etProfileMobile).text.toString().trim()
            
            if (name.isNotEmpty() && mobile.isNotEmpty()) {
                val updates = mapOf("name" to name, "mobile" to mobile)
                database.child("users").child(userId).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter name and mobile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBanners() {
        val ivBanner1 = findViewById<ImageView>(R.id.ivBanner1)
        val ivBanner2 = findViewById<ImageView>(R.id.ivBanner2)
        val ivBanner3 = findViewById<ImageView>(R.id.ivBanner3)

        database.child("Banners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val b1 = snapshot.child("banner1").value?.toString()
                val b2 = snapshot.child("banner2").value?.toString()
                val b3 = snapshot.child("banner3").value?.toString()
                if (!b1.isNullOrEmpty()) Glide.with(this@StudentHomeActivity).load(b1).into(ivBanner1)
                if (!b2.isNullOrEmpty()) Glide.with(this@StudentHomeActivity).load(b2).into(ivBanner2)
                if (!b3.isNullOrEmpty()) Glide.with(this@StudentHomeActivity).load(b3).into(ivBanner3)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
        
        // Show uploading toast
        Toast.makeText(this, "Uploading $type...", Toast.LENGTH_SHORT).show()
        
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
                    // Save URL to database based on type
                    if (type == "profile") {
                        database.child("users").child(userId).child("profilePic")
                            .setValue(downloadUri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                                // Reload the image
                                val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)
                                Glide.with(this).load(downloadUri).circleCrop().into(ivProfile)
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Failed to save profile picture: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        database.child("users").child(userId).child("documents").child(type)
                            .setValue(downloadUri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "$type uploaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Failed to save $type: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to get download URL: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                // Could update a progress bar here if needed
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
        
        // Load status image if available
        if (imageUrl.isNotEmpty()) {
            ivStepImage.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).centerCrop().into(ivStepImage)
        } else {
            ivStepImage.visibility = View.GONE
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_chat -> startActivity(Intent(this, ChatActivity::class.java))
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
            if (uni.imageUrl.isNotEmpty()) Glide.with(holder.itemView.context).load(uni.imageUrl).into(holder.ivPhoto)
        }
        override fun getItemCount() = list.size
    }
}