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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import jp.wasabeef.richeditor.RichEditor

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var richEditor: RichEditor
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        richEditor = findViewById(R.id.richEditor)
        
        // Configure RichEditor
        richEditor.setEditorHeight(200)
        richEditor.setEditorFontSize(16)
        richEditor.setPadding(10, 10, 10, 10)
        richEditor.setPlaceholder("Enter content here...")
        richEditor.setInputEnabled(true)
        richEditor.focusEditor()
        
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

        findViewById<Button>(R.id.btnManageBanners).setOnClickListener {
            startActivity(Intent(this, AdminBannerManagementActivity::class.java))
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

    private fun loadDashboardStats() {
        val tvTotalStudents = findViewById<TextView>(R.id.tvTotalStudents)
        val tvActiveChats = findViewById<TextView>(R.id.tvActiveChats)
        
        // Count total students (single read)
        database.reference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
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
        database.reference.child("Chats").addListenerForSingleValueEvent(object : ValueEventListener {
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
                Toast.makeText(this, "Please enter a valid image URL (http:// or https://)", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Please enter valid URL (http:// or https://) and link text", Toast.LENGTH_SHORT).show()
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