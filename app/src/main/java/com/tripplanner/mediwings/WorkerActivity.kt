package com.tripplanner.mediwings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WorkerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect to WorkerHomeActivity
        startActivity(Intent(this, WorkerHomeActivity::class.java))
        finish()
    }
}