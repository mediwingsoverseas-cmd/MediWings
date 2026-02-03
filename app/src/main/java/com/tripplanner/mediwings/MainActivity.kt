package com.tripplanner.mediwings

import android.content.Intent
import android.os.Bundle
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
        
        var isWorkerSelected = false
        
        // Toggle button behavior
        btnRoleStudent.setOnClickListener {
            isWorkerSelected = false
            btnRoleStudent.setBackgroundColor(getColor(R.color.gold_premium))
            btnRoleStudent.setTextColor(getColor(R.color.primary_premium))
            btnRoleWorker.setBackgroundColor(getColor(android.R.color.transparent))
            btnRoleWorker.setTextColor(getColor(R.color.gold_premium))
        }
        
        btnRoleWorker.setOnClickListener {
            isWorkerSelected = true
            btnRoleWorker.setBackgroundColor(getColor(R.color.gold_premium))
            btnRoleWorker.setTextColor(getColor(R.color.primary_premium))
            btnRoleStudent.setBackgroundColor(getColor(android.R.color.transparent))
            btnRoleStudent.setTextColor(getColor(R.color.gold_premium))
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
                Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
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
}