package com.tripplanner.mediwings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ContactActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarContact)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val tvOfficialEmail = findViewById<TextView>(R.id.tvOfficialEmail)
        val tvJaveedEmail = findViewById<TextView>(R.id.tvJaveedEmail)
        val tvPhone1 = findViewById<TextView>(R.id.tvPhone1)
        val tvPhone2 = findViewById<TextView>(R.id.tvPhone2)
        val btnWhatsApp1 = findViewById<android.widget.Button>(R.id.btnWhatsApp1)
        val btnWhatsApp2 = findViewById<android.widget.Button>(R.id.btnWhatsApp2)

        database = FirebaseDatabase.getInstance().reference.child("CMS").child("contact_info")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val officialEmail = snapshot.child("officialEmail").value?.toString() ?: "mediwingsoverseas@gmail.com"
                    val javeedEmail = snapshot.child("javeedEmail").value?.toString() ?: "javeedzoj@gmail.com"
                    val phone1 = snapshot.child("phone1").value?.toString() ?: "+91 8792207943"
                    val phone2 = snapshot.child("phone2").value?.toString() ?: "+91 9448234176"
                    
                    tvOfficialEmail.text = officialEmail
                    tvJaveedEmail.text = javeedEmail
                    tvPhone1.text = phone1
                    tvPhone2.text = phone2
                    
                    // Make emails clickable
                    tvOfficialEmail.setOnClickListener {
                        openEmail(officialEmail)
                    }
                    
                    tvJaveedEmail.setOnClickListener {
                        openEmail(javeedEmail)
                    }
                    
                    // Make phone numbers clickable
                    tvPhone1.setOnClickListener {
                        openDialer(phone1)
                    }
                    
                    tvPhone2.setOnClickListener {
                        openDialer(phone2)
                    }
                    
                    // WhatsApp buttons
                    btnWhatsApp1.setOnClickListener {
                        openWhatsApp(phone1)
                    }
                    
                    btnWhatsApp2.setOnClickListener {
                        openWhatsApp(phone2)
                    }
                } else {
                    Toast.makeText(this@ContactActivity, "Failed to load contact information", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ContactActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun openWhatsApp(phone: String) {
        // Remove spaces and special characters from phone number
        val phoneNumber = phone.replace(Regex("[^0-9+]"), "")
        val url = "https://wa.me/$phoneNumber"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openDialer(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No dialer app found", Toast.LENGTH_SHORT).show()
        }
    }
}