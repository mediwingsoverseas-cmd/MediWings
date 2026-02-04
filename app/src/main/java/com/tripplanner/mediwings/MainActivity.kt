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
        
        // Initialize Student button as selected by default
        btnRoleStudent.setBackgroundColor(getColor(R.color.student_button_selected))
        btnRoleStudent.setTextColor(getColor(R.color.white))
        btnRoleWorker.setBackgroundColor(getColor(R.color.worker_button))
        btnRoleWorker.setTextColor(getColor(R.color.white))
        
        // Set initial title for Student
        tvAppName.text = "MediWings Student Portal"
        tvTagline.text = "Your Gateway to Medical Education Abroad"
        
        // Toggle button behavior - Student is selected by default
        btnRoleStudent.setOnClickListener {
            if (isWorkerSelected) {
                isWorkerSelected = false
                btnRoleStudent.setBackgroundColor(getColor(R.color.student_button_selected))
                btnRoleStudent.setTextColor(getColor(R.color.white))
                btnRoleWorker.setBackgroundColor(getColor(R.color.worker_button))
                btnRoleWorker.setTextColor(getColor(R.color.white))
                
                // Animate title sliding from left to right
                animateTitleChange(tvAppName, tvTagline, "MediWings Student Portal", 
                    "Your Gateway to Medical Education Abroad", true)
            }
        }
        
        btnRoleWorker.setOnClickListener {
            if (!isWorkerSelected) {
                isWorkerSelected = true
                btnRoleWorker.setBackgroundColor(getColor(R.color.worker_button_selected))
                btnRoleWorker.setTextColor(getColor(R.color.white))
                btnRoleStudent.setBackgroundColor(getColor(R.color.student_button))
                btnRoleStudent.setTextColor(getColor(R.color.white))
                
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
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_LONG).show()
                        
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
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun animateTitleChange(tvAppName: TextView, tvTagline: TextView, newTitle: String, newTagline: String, isLeftToRight: Boolean) {
        val startTranslation = if (isLeftToRight) -tvAppName.width.toFloat() else tvAppName.width.toFloat()
        
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