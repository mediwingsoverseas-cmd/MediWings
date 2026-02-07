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
        
        // Initialize Student button as selected by default with elevation and visual effects
        btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_selected)
        btnRoleStudent.setTextColor(getColor(R.color.white))
        btnRoleStudent.elevation = 8f
        btnRoleStudent.alpha = 1.0f
        
        btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_inactive)
        btnRoleWorker.setTextColor(getColor(R.color.white))
        btnRoleWorker.elevation = 0f
        btnRoleWorker.alpha = 0.6f
        
        // Set initial title for Student
        tvAppName.text = "MediWings Student Portal"
        tvTagline.text = "Your Gateway to Medical Education Abroad"
        
        // Toggle button behavior - Student is selected by default
        btnRoleStudent.setOnClickListener {
            if (isWorkerSelected) {
                isWorkerSelected = false
                
                // Animate Student button to selected state with elevation
                btnRoleStudent.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
                btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_selected)
                btnRoleStudent.elevation = 8f
                
                // Animate Worker button to inactive state
                btnRoleWorker.animate()
                    .alpha(0.6f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(200)
                    .start()
                btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_inactive)
                btnRoleWorker.elevation = 0f
                
                // Animate title sliding from left to right
                animateTitleChange(tvAppName, tvTagline, "MediWings Student Portal", 
                    "Your Gateway to Medical Education Abroad", true)
            }
        }
        
        btnRoleWorker.setOnClickListener {
            if (!isWorkerSelected) {
                isWorkerSelected = true
                
                // Animate Worker button to selected state with elevation
                btnRoleWorker.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
                btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_selected)
                btnRoleWorker.elevation = 8f
                
                // Animate Student button to inactive state
                btnRoleStudent.animate()
                    .alpha(0.6f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(200)
                    .start()
                btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_inactive)
                btnRoleStudent.elevation = 0f
                
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

            // Hardcoded Admin Login
            if (email == "javeedzoj@gmail.com" && password == "javeedJaV") {
                // Show dialog for admin to select Student or Worker admin mode
                val options = arrayOf("Student Admin", "Worker Admin")
                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("Select Admin Mode")
                builder.setItems(options) { dialog, which ->
                    val adminMode = if (which == 0) "student" else "worker"
                    Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("ADMIN_MODE", adminMode)
                    startActivity(intent)
                    finish()
                }
                builder.setOnCancelListener {
                    // User cancelled, do nothing
                }
                builder.show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        
                        // Save the role preference
                        val sharedPref = getSharedPreferences("MediWingsPrefs", MODE_PRIVATE)
                        sharedPref.edit().putBoolean("isWorker", isWorkerSelected).apply()
                        
                        val intent = if (isWorkerSelected) {
                            Intent(this, WorkerActivity::class.java)
                        } else {
                            Intent(this, StudentHomeActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
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