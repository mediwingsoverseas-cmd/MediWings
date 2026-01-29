package com.tripplanner.mediwings

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class StudentControlActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_control)

        userId = intent.getStringExtra("USER_ID") ?: return
        database = FirebaseDatabase.getInstance().reference.child("users").child(userId!!)

        val tvName = findViewById<TextView>(R.id.tvControlStudentName)
        val cbApp = findViewById<CheckBox>(R.id.cbApplication)
        val cbDocs = findViewById<CheckBox>(R.id.cbDocuments)
        val cbVer = findViewById<CheckBox>(R.id.cbVerification)
        val rgVisa = findViewById<RadioGroup>(R.id.rgVisa)
        val cbFlight = findViewById<CheckBox>(R.id.cbFlight)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateStatus)
        val ivPassport = findViewById<ImageView>(R.id.ivAdminPassport)
        val ivAadhar = findViewById<ImageView>(R.id.ivAdminAadhar)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvName.text = snapshot.child("name").value.toString()
                
                val status = snapshot.child("status")
                cbApp.isChecked = status.child("application").value == true
                cbDocs.isChecked = status.child("documents").value == true
                cbVer.isChecked = status.child("verification").value == true
                cbFlight.isChecked = status.child("flight").value == true
                
                when(status.child("visa").value?.toString()) {
                    "Applied" -> rgVisa.check(R.id.rbVisaApplied)
                    "Processing" -> rgVisa.check(R.id.rbVisaProcessing)
                    "Approved" -> rgVisa.check(R.id.rbVisaApproved)
                }

                val docs = snapshot.child("docs")
                val pUrl = docs.child("passport").value?.toString()
                val aUrl = docs.child("aadhar").value?.toString()
                if (!pUrl.isNullOrEmpty()) Glide.with(this@StudentControlActivity).load(pUrl).into(ivPassport)
                if (!aUrl.isNullOrEmpty()) Glide.with(this@StudentControlActivity).load(aUrl).into(ivAadhar)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnUpdate.setOnClickListener {
            val visaStatus = when(rgVisa.checkedRadioButtonId) {
                R.id.rbVisaApplied -> "Applied"
                R.id.rbVisaProcessing -> "Processing"
                R.id.rbVisaApproved -> "Approved"
                else -> "Not Applied"
            }

            val statusUpdates = mapOf(
                "application" to cbApp.isChecked,
                "documents" to cbDocs.isChecked,
                "verification" to cbVer.isChecked,
                "visa" to visaStatus,
                "flight" to cbFlight.isChecked
            )

            database.child("status").updateChildren(statusUpdates).addOnSuccessListener {
                Toast.makeText(this, "Status Updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}