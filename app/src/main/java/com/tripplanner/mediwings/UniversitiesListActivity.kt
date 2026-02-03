package com.tripplanner.mediwings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class UniversitiesListActivity : AppCompatActivity() {

    private lateinit var rvUniversities: RecyclerView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_universities_list)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Top Universities"

        rvUniversities = findViewById(R.id.rvUniversities)
        rvUniversities.layoutManager = LinearLayoutManager(this)

        database = FirebaseDatabase.getInstance().reference.child("Universities")

        val uniList = mutableListOf<University>()
        val adapter = UniversityAdapter(uniList)
        rvUniversities.adapter = adapter

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                uniList.clear()
                for (data in snapshot.children) {
                    val university = data.getValue(University::class.java)
                    if (university != null) {
                        uniList.add(university)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    data class University(
        val id: String = "",
        val name: String = "",
        val details: String = "",
        val imageUrl: String = ""
    )

    private class UniversityAdapter(private val list: List<University>) : RecyclerView.Adapter<UniversityAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivUniItemPhoto)
            val tvName: TextView = view.findViewById(R.id.tvUniItemName)
            val tvDetails: TextView = view.findViewById(R.id.tvUniItemDetails)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_university, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uni = list[position]
            holder.tvName.text = uni.name
            holder.tvDetails.text = uni.details
            if (uni.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(uni.imageUrl).into(holder.ivPhoto)
            }
        }

        override fun getItemCount() = list.size
    }
}