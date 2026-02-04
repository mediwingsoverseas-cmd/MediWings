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
        
        // Toggle button behavior - Student is selected by default
        btnRoleStudent.setOnClickListener {
            isWorkerSelected = false
            btnRoleStudent.setBackgroundColor(getColor(R.color.student_button_selected))
            btnRoleStudent.setTextColor(getColor(R.color.white))
            btnRoleWorker.setBackgroundColor(getColor(R.color.worker_button))
            btnRoleWorker.setTextColor(getColor(R.color.white))
        }
        
        btnRoleWorker.setOnClickListener {
            isWorkerSelected = true
            btnRoleWorker.setBackgroundColor(getColor(R.color.worker_button_selected))
            btnRoleWorker.setTextColor(getColor(R.color.white))
            btnRoleStudent.setBackgroundColor(getColor(R.color.student_button))
            btnRoleStudent.setTextColor(getColor(R.color.white))
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
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Database Error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Error: User ID is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Authentication Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }
}
