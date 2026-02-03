package com.tripplanner.mediwings

import android.os.Bundle
import android.view.View
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

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Student Control"
        toolbar.setNavigationOnClickListener { finish() }

        userId = intent.getStringExtra("USER_ID") ?: return
        database = FirebaseDatabase.getInstance().reference.child("users").child(userId!!)

        val tvName = findViewById<TextView>(R.id.tvControlStudentName)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateStatus)
        
        // Document Previews
        val ivPhotos = findViewById<ImageView>(R.id.ivAdminPhotos)
        val ivAadhar = findViewById<ImageView>(R.id.ivAdminAadhar)
        val ivPassport = findViewById<ImageView>(R.id.ivAdminPassport)
        val ivHIV = findViewById<ImageView>(R.id.ivAdminHIV)

        // Setup Controls for tracking steps
        val controlApp = findViewById<View>(R.id.control_application)
        val controlVer = findViewById<View>(R.id.control_verification)
        val controlAdm = findViewById<View>(R.id.control_admission)
        val controlVisa = findViewById<View>(R.id.control_visa)
        val controlVisaApplied = findViewById<View>(R.id.control_visa_applied)
        val controlVisaProcessing = findViewById<View>(R.id.control_visa_processing)
        val controlVisaApproved = findViewById<View>(R.id.control_visa_approved)
        val controlFlight = findViewById<View>(R.id.control_flight)

        setupControlTitle(controlApp, "Application")
        setupControlTitle(controlVer, "Document Verification")
        setupControlTitle(controlAdm, "University Admission")
        setupControlTitle(controlVisa, "Visa")
        setupControlTitle(controlVisaApplied, "• Visa Applied")
        setupControlTitle(controlVisaProcessing, "• Processing")
        setupControlTitle(controlVisaApproved, "• Approved")
        setupControlTitle(controlFlight, "Flight Scheduled")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                tvName.text = snapshot.child("name").value.toString()
                
                val tracking = snapshot.child("tracking")
                loadStepData(controlApp, tracking.child("step1"))
                loadStepData(controlVer, tracking.child("step2"))
                loadStepData(controlAdm, tracking.child("step3"))
                
                val visaData = tracking.child("step4")
                loadStepData(controlVisa, visaData)
                loadStepData(controlVisaApplied, visaData.child("applied"))
                loadStepData(controlVisaProcessing, visaData.child("processing"))
                loadStepData(controlVisaApproved, visaData.child("approved"))
                
                loadStepData(controlFlight, tracking.child("step5"))

                val docs = snapshot.child("docs")
                loadDocImage(docs.child("photos").value?.toString(), ivPhotos)
                loadDocImage(docs.child("aadhar").value?.toString(), ivAadhar)
                loadDocImage(docs.child("passport").value?.toString(), ivPassport)
                loadDocImage(docs.child("hiv").value?.toString(), ivHIV)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnUpdate.setOnClickListener {
            val visaStep = getStepData(controlVisa).toMutableMap()
            visaStep["applied"] = getStepData(controlVisaApplied)
            visaStep["processing"] = getStepData(controlVisaProcessing)
            visaStep["approved"] = getStepData(controlVisaApproved)

            val trackingUpdates = mapOf(
                "step1" to getStepData(controlApp),
                "step2" to getStepData(controlVer),
                "step3" to getStepData(controlAdm),
                "step4" to visaStep,
                "step5" to getStepData(controlFlight)
            )

            database.child("tracking").setValue(trackingUpdates).addOnSuccessListener {
                Toast.makeText(this, "Tracking Updated Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupControlTitle(view: View, title: String) {
        view.findViewById<TextView>(R.id.tvAdminStepTitle).text = title
    }

    private fun loadStepData(view: View, data: DataSnapshot) {
        val status = data.child("status").value == true
        val date = data.child("date").value?.toString() ?: ""
        val remark = data.child("remark").value?.toString() ?: ""

        view.findViewById<Switch>(R.id.swStepStatus).isChecked = status
        view.findViewById<EditText>(R.id.etStepDate).setText(date)
        view.findViewById<EditText>(R.id.etStepRemark).setText(remark)
    }

    private fun getStepData(view: View): Map<String, Any> {
        return mapOf(
            "status" to view.findViewById<Switch>(R.id.swStepStatus).isChecked,
            "date" to view.findViewById<EditText>(R.id.etStepDate).text.toString(),
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