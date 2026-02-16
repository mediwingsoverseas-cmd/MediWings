package com.tripplanner.mediwings

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class StudentControlActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var trackingRef: DatabaseReference
    private var userId: String? = null
    private val statusOptions = arrayOf("pending", "completed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_control)

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Student Control"
        toolbar.setNavigationOnClickListener { finish() }

        userId = intent.getStringExtra("USER_ID") ?: return
        database = FirebaseDatabase.getInstance().reference.child("users").child(userId!!)
        trackingRef = FirebaseDatabase.getInstance().reference.child("Tracking").child(userId!!)

        val tvName = findViewById<TextView>(R.id.tvControlStudentName)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateStatus)
        
        // Document Previews
        val ivPhotos = findViewById<ImageView>(R.id.ivAdminPhotos)
        val ivAadhar = findViewById<ImageView>(R.id.ivAdminAadhar)
        val ivPassport = findViewById<ImageView>(R.id.ivAdminPassport)
        val ivHIV = findViewById<ImageView>(R.id.ivAdminHIV)

        // Setup Controls for tracking steps (5 main steps as per requirements)
        // Note: Using existing layout IDs but relabeling them for the 5-step workflow
        val controlApp = findViewById<View>(R.id.control_application)        // Step 1: Application
        val controlDocs = findViewById<View>(R.id.control_verification)      // Step 2: Documents (reuses verification ID)
        val controlAdm = findViewById<View>(R.id.control_admission)          // Step 3: Admission
        val controlVisa = findViewById<View>(R.id.control_visa)              // Step 4: Visa
        val controlFlight = findViewById<View>(R.id.control_flight)          // Step 5: Flight

        // Setup Spinners for each control
        setupControlWithSpinner(controlApp, "Application")
        setupControlWithSpinner(controlDocs, "Documents")
        setupControlWithSpinner(controlAdm, "Admission")
        setupControlWithSpinner(controlVisa, "Visa")
        setupControlWithSpinner(controlFlight, "Flight")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                tvName.text = snapshot.child("name").value.toString()
                
                val docs = snapshot.child("documents")
                loadDocImage(docs.child("photos").value?.toString(), ivPhotos)
                loadDocImage(docs.child("aadhar").value?.toString(), ivAadhar)
                loadDocImage(docs.child("passport").value?.toString(), ivPassport)
                loadDocImage(docs.child("hiv").value?.toString(), ivHIV)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        // Load tracking data from Tracking/{uid}
        trackingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    loadStepData(controlApp, snapshot.child("application"))
                    loadStepData(controlDocs, snapshot.child("docs"))
                    loadStepData(controlAdm, snapshot.child("admission"))
                    loadStepData(controlVisa, snapshot.child("visa"))
                    loadStepData(controlFlight, snapshot.child("flight"))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnUpdate.setOnClickListener {
            val trackingUpdates = mapOf(
                "application" to getStepData(controlApp),
                "docs" to getStepData(controlDocs),
                "admission" to getStepData(controlAdm),
                "visa" to getStepData(controlVisa),
                "flight" to getStepData(controlFlight)
            )

            // Update to Tracking/{student_uid} as per requirements
            trackingRef.setValue(trackingUpdates).addOnSuccessListener {
                Toast.makeText(this, "Tracking Updated Successfully!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupControlWithSpinner(view: View, title: String) {
        view.findViewById<TextView>(R.id.tvAdminStepTitle).text = title
        val spinner = view.findViewById<Spinner>(R.id.spinnerStepStatus)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun loadStepData(view: View, data: DataSnapshot) {
        val status = data.child("status").value?.toString() ?: "pending"
        val date = data.child("completedDate").value?.toString() ?: data.child("date").value?.toString() ?: ""
        val remark = data.child("remark").value?.toString() ?: ""

        val spinner = view.findViewById<Spinner>(R.id.spinnerStepStatus)
        val position = statusOptions.indexOf(status)
        if (position >= 0) {
            spinner.setSelection(position)
        }
        view.findViewById<EditText>(R.id.etStepDate).setText(date)
        view.findViewById<EditText>(R.id.etStepRemark).setText(remark)
    }

    private fun getStepData(view: View): Map<String, Any> {
        val spinner = view.findViewById<Spinner>(R.id.spinnerStepStatus)
        val status = spinner.selectedItem?.toString() ?: "pending"
        return mapOf(
            "status" to status,
            "completedDate" to view.findViewById<EditText>(R.id.etStepDate).text.toString(),
            "remark" to view.findViewById<EditText>(R.id.etStepRemark).text.toString()
        )
    }

    private fun loadDocImage(url: String?, imageView: ImageView) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this).load(url).into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }
}