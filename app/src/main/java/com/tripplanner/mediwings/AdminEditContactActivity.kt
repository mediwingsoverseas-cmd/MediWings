package com.tripplanner.mediwings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AdminEditContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_contact)

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Contact Info"
        toolbar.setNavigationOnClickListener { finish() }

        val etOfficialEmail = findViewById<EditText>(R.id.etOfficialEmail)
        val etJaveedEmail = findViewById<EditText>(R.id.etJaveedEmail)
        val etPhone1 = findViewById<EditText>(R.id.etPhone1)
        val etPhone2 = findViewById<EditText>(R.id.etPhone2)
        val btnSave = findViewById<Button>(R.id.btnSaveContact)

        val database = FirebaseDatabase.getInstance().reference.child("CMS").child("contact_info")

        database.get().addOnSuccessListener {
            if (it.exists()) {
                etOfficialEmail.setText(it.child("officialEmail").value?.toString())
                etJaveedEmail.setText(it.child("javeedEmail").value?.toString())
                etPhone1.setText(it.child("phone1").value?.toString())
                etPhone2.setText(it.child("phone2").value?.toString())
            }
        }

        btnSave.setOnClickListener {
            val data = mapOf(
                "officialEmail" to etOfficialEmail.text.toString(),
                "javeedEmail" to etJaveedEmail.text.toString(),
                "phone1" to etPhone1.text.toString(),
                "phone2" to etPhone2.text.toString()
            )
            database.setValue(data).addOnSuccessListener {
                Toast.makeText(this, "Contact Info Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}