package com.tripplanner.mediwings

import android.os.Bundle
import android.widget.TextView
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

        database = FirebaseDatabase.getInstance().reference.child("CMS").child("contact_info")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    tvOfficialEmail.text = snapshot.child("officialEmail").value?.toString() ?: "mediwingsoverseas@gmail.com"
                    tvJaveedEmail.text = snapshot.child("javeedEmail").value?.toString() ?: "javeedzoj@gmail.com"
                    tvPhone1.text = snapshot.child("phone1").value?.toString() ?: "+91 8792207943"
                    tvPhone2.text = snapshot.child("phone2").value?.toString() ?: "+91 9448234176"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}