package com.tripplanner.mediwings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isWorkerSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Account"
        toolbar.setNavigationOnClickListener { finish() }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val etMobile = findViewById<EditText>(R.id.etMobile)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val btnRoleStudent = findViewById<Button>(R.id.btnRoleStudent)
        val btnRoleWorker = findViewById<Button>(R.id.btnRoleWorker)
        
        // Get elevation values from resources for consistency
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
        
        // Toggle button behavior - Student is selected by default
        btnRoleStudent.setOnClickListener {
            if (isWorkerSelected) {
                isWorkerSelected = false
                
                // Set backgrounds first to avoid flicker
                btnRoleStudent.setBackgroundResource(R.drawable.bg_button_student_selected)
                btnRoleStudent.elevation = elevationSelected
                btnRoleWorker.setBackgroundResource(R.drawable.bg_button_worker_inactive)
                btnRoleWorker.elevation = elevationInactive
                
                // Animate Student button to selected state
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
                
                // Animate Worker button to selected state
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
            }
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val mobile = etMobile.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        if (userId != null) {
                            val userType = if (isWorkerSelected) "workers" else "users"
                            val userRef = database.reference.child(userType).child(userId)
                            val userData = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "mobile" to mobile,
                                "role" to if (isWorkerSelected) "worker" else "student"
                            )
                            userRef.setValue(userData)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Registration failed: User ID is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("email address is already", ignoreCase = true) == true -> 
                                "This email is already registered. Please login instead."
                            task.exception?.message?.contains("badly formatted", ignoreCase = true) == true -> 
                                "Invalid email format. Please check and try again."
                            task.exception?.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your connection."
                            else -> "Registration failed: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }
}
