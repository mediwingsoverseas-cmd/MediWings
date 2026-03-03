package com.tripplanner.mediwings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import jp.wasabeef.richeditor.RichEditor
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var richEditor: RichEditor
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var adminMode: String = "student" // "student" or "worker"

    // Realtime listener references (registered in onStart, removed in onStop)
    private var usersListener: ValueEventListener? = null
    private var chatsListener: ValueEventListener? = null
    private var usersRef: DatabaseReference? = null
    private var chatsRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        adminMode = intent.getStringExtra("ADMIN_MODE") ?: "student"

        richEditor = findViewById(R.id.richEditor)
        
        // Configure RichEditor
        richEditor.setEditorHeight(200)
        richEditor.setEditorFontSize(16)
        richEditor.setPadding(10, 10, 10, 10)
        richEditor.setPlaceholder("Enter content here...")
        richEditor.setInputEnabled(true)
        richEditor.focusEditor()
        
        // Set title and subtitle based on admin mode
        val roleLabel = if (adminMode == "worker") "Worker Admin" else "Student Admin"
        supportActionBar?.title = "$roleLabel Dashboard"
        val tvRoleChip = findViewById<TextView>(R.id.tvAdminRoleChip)
        tvRoleChip.text = roleLabel
        
        // Update button labels based on mode
        val btnAdminStudents = findViewById<Button>(R.id.btnAdminStudents)
        btnAdminStudents.text = if (adminMode == "worker") "WORKERS" else "STUDENTS"

        btnAdminStudents.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "control")
            intent.putExtra("ROLE", adminMode)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAdminMessages).setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("MODE", "chat")
            intent.putExtra("ROLE", adminMode)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAddUniversity).setOnClickListener {
            startActivity(Intent(this, AdminAddUniversityActivity::class.java))
        }

        findViewById<Button>(R.id.btnEditContact).setOnClickListener {
            startActivity(Intent(this, AdminEditContactActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageBanners).setOnClickListener {
            startActivity(Intent(this, AdminBannerManagementActivity::class.java))
        }

        // Rich text formatting buttons
        findViewById<Button>(R.id.btnBold).setOnClickListener { richEditor.setBold() }
        findViewById<Button>(R.id.btnItalic).setOnClickListener { richEditor.setItalic() }
        findViewById<Button>(R.id.btnUnderline).setOnClickListener { richEditor.setUnderline() }
        findViewById<Button>(R.id.btnH1).setOnClickListener { richEditor.setHeading(1) }
        findViewById<Button>(R.id.btnH2).setOnClickListener { richEditor.setHeading(2) }
        findViewById<Button>(R.id.btnBullets).setOnClickListener { richEditor.setBullets() }
        findViewById<Button>(R.id.btnNumbers).setOnClickListener { richEditor.setNumbers() }
        findViewById<Button>(R.id.btnInsertImage).setOnClickListener { showImageUrlDialog() }
        findViewById<Button>(R.id.btnInsertLink).setOnClickListener { showLinkDialog() }

        // Save CMS with confirmation dialog
        findViewById<Button>(R.id.btnSaveCMS).setOnClickListener {
            val content = richEditor.html ?: ""
            if (content.isEmpty()) {
                Toast.makeText(this, "Cannot save empty content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Publish Content")
                .setMessage("Are you sure you want to publish this content to the home page?")
                .setPositiveButton("Publish") { _, _ -> saveCmsContent(content) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Load existing CMS content
        database.reference.child("CMS").child("home_content").get()
            .addOnSuccessListener {
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                try {
                    if (it.exists()) {
                        richEditor.html = it.value.toString()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading content: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                if (isFinishing || isDestroyed) return@addOnFailureListener
                Toast.makeText(
                    this,
                    "Failed to load content. Check network/permissions: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        
        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        registerRealtimeListeners()
    }

    override fun onStop() {
        super.onStop()
        removeRealtimeListeners()
    }

    private fun registerRealtimeListeners() {
        val tvTotalStudents = findViewById<TextView>(R.id.tvTotalStudents)
        val tvActiveChats = findViewById<TextView>(R.id.tvActiveChats)
        tvTotalStudents.text = "..."
        tvActiveChats.text = "..."

        // Realtime listener for user count
        val dbNode = if (adminMode == "worker") "workers" else "users"
        usersRef = database.reference.child(dbNode)
        val uRef = usersRef ?: return
        val uListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                if (!snapshot.exists()) {
                    tvTotalStudents.text = "0"
                    return
                }
                var userCount = 0
                for (userSnapshot in snapshot.children) {
                    val role = userSnapshot.child("role").value?.toString()
                    if (role != "admin") userCount++
                }
                tvTotalStudents.text = userCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                if (isFinishing || isDestroyed) return
                tvTotalStudents.text = "!"
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Failed to load user stats. Check network/permissions: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        usersListener = uListener
        uRef.addValueEventListener(uListener)

        // Realtime listener for chat count
        chatsRef = database.reference.child("Chats")
        val cRef = chatsRef ?: return
        val cListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                if (!snapshot.exists()) {
                    tvActiveChats.text = "0"
                    return
                }
                var chatCount = 0
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    if (chatId.endsWith("_$adminMode")) chatCount++
                }
                tvActiveChats.text = chatCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                if (isFinishing || isDestroyed) return
                tvActiveChats.text = "!"
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Failed to load chat stats. Check network/permissions: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        chatsListener = cListener
        cRef.addValueEventListener(cListener)
    }

    private fun removeRealtimeListeners() {
        usersListener?.let { usersRef?.removeEventListener(it) }
        chatsListener?.let { chatsRef?.removeEventListener(it) }
        usersListener = null
        chatsListener = null
    }

    private fun saveCmsContent(content: String) {
        val userEmail = auth.currentUser?.email ?: "unknown"
        val roleLabel = if (adminMode == "worker") "Worker Admin" else "Student Admin"
        val timestamp = System.currentTimeMillis()

        database.reference.child("CMS").child("home_content").setValue(content)
            .addOnSuccessListener {
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                Toast.makeText(this, "Home Page Updated Successfully!", Toast.LENGTH_SHORT).show()
                // Write lightweight audit entry
                val auditEntry = mapOf(
                    "userEmail" to userEmail,
                    "role" to roleLabel,
                    "action" to "publish_home_content",
                    "timestamp" to timestamp
                )
                database.reference.child("CMS").child("audit").child(timestamp.toString())
                    .setValue(auditEntry)
            }
            .addOnFailureListener { exception ->
                if (isFinishing || isDestroyed) return@addOnFailureListener
                Toast.makeText(
                    this,
                    "Failed to update. Check network/permissions: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
                val screenWidth = resources.displayMetrics.widthPixels
                val imageWidth = (screenWidth * 0.9).toInt()
                richEditor.insertImage(url, "image", imageWidth)
            } else {
                Toast.makeText(this, "Please enter a valid image URL (http:// or https://)", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        
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
                Toast.makeText(this, "Please enter valid URL (http:// or https://) and link text", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        
        builder.show()
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    private fun isValidImageUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}

