package com.tripplanner.mediwings

import android.content.Intent
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

class UserListActivity : AppCompatActivity() {

    private lateinit var rvUserList: RecyclerView
    private lateinit var database: DatabaseReference
    private var mode: String? = null // "chat" or "control"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        mode = intent.getStringExtra("MODE")
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = if (mode == "chat") "Select User to Chat" else "Select Student"

        rvUserList = findViewById(R.id.rvUserList)
        rvUserList.layoutManager = LinearLayoutManager(this)
        
        database = FirebaseDatabase.getInstance().reference.child("users")
        
        val userList = mutableListOf<UserData>()
        val adapter = UserAdapter(userList) { user ->
            if (mode == "chat") {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ID", user.uid)
                intent.putExtra("USER_NAME", user.name)
                startActivity(intent)
            } else {
                val intent = Intent(this, StudentControlActivity::class.java)
                intent.putExtra("USER_ID", user.uid)
                startActivity(intent)
            }
        }
        rvUserList.adapter = adapter

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val uid = data.key ?: continue
                    val name = data.child("name").value?.toString() ?: "Unknown"
                    val profilePic = data.child("profilePic").value?.toString() ?: ""
                    userList.add(UserData(uid, name, profilePic))
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    data class UserData(val uid: String, val name: String, val profilePic: String)

    private class UserAdapter(val list: List<UserData>, val onClick: (UserData) -> Unit) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivUserImage)
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvLastMsg: TextView = view.findViewById(R.id.tvUserLastMsg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = list[position]
            holder.tvName.text = user.name
            holder.tvLastMsg.text = "Click to open"
            if (user.profilePic.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(user.profilePic).into(holder.ivImage)
            }
            holder.itemView.setOnClickListener { onClick(user) }
        }

        override fun getItemCount() = list.size
    }
}