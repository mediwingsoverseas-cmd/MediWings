package com.tripplanner.mediwings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
    
    private var uploadType = "" // "passport" or "aadhar" or "profile"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = result.data?.data
            if (fileUri != null) {
                uploadImage(fileUri, uploadType)
            }
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
        supportActionBar?.setDisplayShowTitleEnabled(false) // Remove "Student Dimension" title

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupBottomNav()
        loadUserData()
        loadBanners()
        setupDocUploads()
        setupStatusTimeline()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val homeView = findViewById<View>(R.id.home_view)
        val docsView = findViewById<View>(R.id.docs_view)
        val profileView = findViewById<View>(R.id.profile_view)
        val statusView = findViewById<View>(R.id.status_view)

        bottomNav.setOnItemSelectedListener { item ->
            homeView.visibility = View.GONE
            docsView.visibility = View.GONE
            profileView.visibility = View.GONE
            statusView.visibility = View.GONE

            when (item.itemId) {
                R.id.bottom_universities -> homeView.visibility = View.VISIBLE
                R.id.bottom_status -> statusView.visibility = View.VISIBLE
                R.id.bottom_docs -> docsView.visibility = View.VISIBLE
                R.id.bottom_profile -> profileView.visibility = View.VISIBLE
            }
            true
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvMobile = findViewById<TextView>(R.id.tvProfileMobile)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val ivProfile = findViewById<ImageView>(R.id.ivProfilePic)

        database.child("users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    tvName.text = snapshot.child("name").value.toString()
                    tvMobile.text = snapshot.child("mobile").value.toString()
                    tvEmail.text = snapshot.child("email").value.toString()
                    val pic = snapshot.child("profilePic").value?.toString()
                    if (!pic.isNullOrEmpty()) Glide.with(this@StudentHomeActivity).load(pic).into(ivProfile)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        ivProfile.setOnClickListener {
            uploadType = "profile"
            openGallery()
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

    private fun setupDocUploads() {
        findViewById<Button>(R.id.btnUploadPassport).setOnClickListener {
            uploadType = "passport"
            openGallery()
        }
        findViewById<Button>(R.id.btnUploadAadhar).setOnClickListener {
            uploadType = "aadhar"
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("uploads/$userId/$type.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                if (type == "profile") {
                    database.child("users").child(userId).child("profilePic").setValue(downloadUrl.toString())
                } else {
                    database.child("users").child(userId).child("docs").child(type).setValue(downloadUrl.toString())
                }
                Toast.makeText(this, "Upload Successful!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupStatusTimeline() {
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateStep(findViewById(R.id.step_application), "Application", snapshot.child("application").value == true)
                updateStep(findViewById(R.id.step_documents), "Documents", snapshot.child("documents").value == true)
                updateStep(findViewById(R.id.step_verification), "Verification", snapshot.child("verification").value == true)
                
                val visaStatus = snapshot.child("visa").value?.toString() ?: "Not Applied"
                updateStep(findViewById(R.id.step_visa), "Visa", visaStatus == "Approved", visaStatus)
                
                updateStep(findViewById(R.id.step_flight), "Flight Schedule", snapshot.child("flight").value == true)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateStep(view: View, title: String, isDone: Boolean, desc: String? = null) {
        view.findViewById<TextView>(R.id.tvStepTitle).text = title
        view.findViewById<TextView>(R.id.tvStepDescription).text = desc ?: if (isDone) "Completed" else "In Progress"
        val indicator = view.findViewById<View>(R.id.view_indicator)
        indicator.setBackgroundColor(if (isDone) android.graphics.Color.GREEN else android.graphics.Color.GRAY)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_chat -> startActivity(Intent(this, ChatActivity::class.java))
            R.id.nav_logout -> {
                auth.signOut()
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
}