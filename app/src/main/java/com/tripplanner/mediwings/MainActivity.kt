package com.tripplanner.mediwings

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        
        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, check role and navigate
            val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
            val isWorker = sharedPref.getBoolean("isWorker", false)
            val intent = if (isWorker) {
                Intent(this, WorkerActivity::class.java)
            } else {
                Intent(this, StudentHomeActivity::class.java)
            }
            startActivity(intent)
            finish()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val btnRoleStudent = findViewById<Button>(R.id.btnRoleStudent)
        val btnRoleWorker = findViewById<Button>(R.id.btnRoleWorker)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        
        var isWorkerSelected = false
        
        // Get elevation values from resources for consistency
        // resources.getDimension() automatically converts dp to pixels for use with View.elevation
        val elevationSelected = resources.getDimension(R.dimen.button_elevation_selected)
        val elevationInactive = resources.getDimension(R.dimen.button_elevation_inactive)
        
        // Initialize Student button as selected by default with elevation and visual effects
        btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_selected)
        btnRoleStudent.setTextColor(getColor(R.color.white))
        btnRoleStudent.elevation = elevationSelected
        btnRoleStudent.alpha = 1.0f
        
        btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_inactive)
        btnRoleWorker.setTextColor(getColor(R.color.white))
        btnRoleWorker.elevation = elevationInactive
        btnRoleWorker.alpha = 0.6f
        
        // Set initial title for Student
        tvAppName.text = "MediWings Student Portal"
        tvTagline.text = "Your Gateway to Medical Education Abroad"
        
        // Toggle button behavior - Student is selected by default
        btnRoleStudent.setOnClickListener {
            if (isWorkerSelected) {
                isWorkerSelected = false
                
                // Set backgrounds first to avoid flicker
                btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_selected)
                btnRoleStudent.elevation = elevationSelected
                btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_inactive)
                btnRoleWorker.elevation = elevationInactive
                
                // Then animate Student button to selected state
                btnRoleStudent.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
                
                // Animate Worker button to inactive state
                btnRoleWorker.animate()
                    .alpha(0.6f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(200)
                    .start()
                
                // Animate title sliding from left to right
                animateTitleChange(tvAppName, tvTagline, "MediWings Student Portal", 
                    "Your Gateway to Medical Education Abroad", true)
            }
        }
        
        btnRoleWorker.setOnClickListener {
            if (!isWorkerSelected) {
                isWorkerSelected = true
                
                // Set backgrounds first to avoid flicker
                btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_selected)
                btnRoleWorker.elevation = elevationSelected
                btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_inactive)
                btnRoleStudent.elevation = elevationInactive
                
                // Then animate Worker button to selected state
                btnRoleWorker.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
                
                // Animate Student button to inactive state
                btnRoleStudent.animate()
                    .alpha(0.6f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(200)
                    .start()
                
                // Animate title sliding from right to left
                animateTitleChange(tvAppName, tvTagline, "MediWings Worker Portal", 
                    "Professional Opportunities Await You", false)
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt Firebase authentication first
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        
                        // Check if user is admin in Firebase Database
                        val database = FirebaseDatabase.getInstance()
                        database.getReference("users").child(userId).child("isAdmin")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                                    
                                    if (isAdmin) {
                                        // User is admin - show mode selection dialog
                                        val options = arrayOf("Student Admin", "Worker Admin")
                                        val builder = android.app.AlertDialog.Builder(this@MainActivity)
                                        builder.setTitle("Select Admin Mode")
                                        builder.setItems(options) { dialog, which ->
                                            val adminMode = if (which == 0) "student" else "worker"
                                            Toast.makeText(this@MainActivity, "Admin Login Successful!", Toast.LENGTH_SHORT).show()
                                            
                                            val intent = Intent(this@MainActivity, AdminDashboardActivity::class.java)
                                            intent.putExtra("ADMIN_MODE", adminMode)
                                            startActivity(intent)
                                            finish()
                                        }
                                        builder.setOnCancelListener {
                                            // User cancelled - sign out and stay on login page
                                            auth.signOut()
                                        }
                                        builder.show()
                                    } else {
                                        // Regular user login
                                        Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        
                                        // Save the role preference
                                        val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
                                        sharedPref.edit().putBoolean("isWorker", isWorkerSelected).apply()
                                        
                                        val intent = if (isWorkerSelected) {
                                            Intent(this@MainActivity, WorkerActivity::class.java)
                                        } else {
                                            Intent(this@MainActivity, StudentHomeActivity::class.java)
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                                
                                override fun onCancelled(error: DatabaseError) {
                                    // Database error - treat as regular user
                                    Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                    
                                    val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
                                    sharedPref.edit().putBoolean("isWorker", isWorkerSelected).apply()
                                    
                                    val intent = if (isWorkerSelected) {
                                        Intent(this@MainActivity, WorkerActivity::class.java)
                                    } else {
                                        Intent(this@MainActivity, StudentHomeActivity::class.java)
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            })
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("no user record", ignoreCase = true) == true -> 
                                "No account found with this email. Please register first."
                            task.exception?.message?.contains("password is invalid", ignoreCase = true) == true -> 
                                "Incorrect password. Please try again."
                            task.exception?.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your connection."
                            else -> "Login Failed: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun animateTitleChange(tvAppName: TextView, tvTagline: TextView, newTitle: String, newTagline: String, isLeftToRight: Boolean) {
        // Use post to ensure view is measured before animation
        tvAppName.post {
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val startTranslation = if (isLeftToRight) -screenWidth else screenWidth
            
            // Slide out
            val slideOut1 = ObjectAnimator.ofFloat(tvAppName, View.TRANSLATION_X, 0f, -startTranslation)
            val slideOut2 = ObjectAnimator.ofFloat(tvTagline, View.TRANSLATION_X, 0f, -startTranslation)
            slideOut1.duration = 200
            slideOut2.duration = 200
            
            slideOut1.start()
            slideOut2.start()
            
            // Change text and slide in
            tvAppName.postDelayed({
                tvAppName.text = newTitle
                tvTagline.text = newTagline
                tvAppName.translationX = startTranslation
                tvTagline.translationX = startTranslation
                
                val slideIn1 = ObjectAnimator.ofFloat(tvAppName, View.TRANSLATION_X, startTranslation, 0f)
                val slideIn2 = ObjectAnimator.ofFloat(tvTagline, View.TRANSLATION_X, startTranslation, 0f)
                slideIn1.duration = 200
                slideIn2.duration = 200
                
                slideIn1.start()
                slideIn2.start()
            }, 200)
        }
    }
}