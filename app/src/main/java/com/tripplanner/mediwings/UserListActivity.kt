package com.tripplanner.mediwings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserListActivity : AppCompatActivity() {

    private lateinit var rvUserList: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private var mode: String? = null 
    private var userRole: String = "student" 
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val TAG = "UserListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().reference
        
        mode = intent.getStringExtra("MODE")
        userRole = intent.getStringExtra("ROLE") ?: "student"
        
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val roleLabel = if (userRole == "worker") "Worker" else "Student"
        supportActionBar?.title = if (mode == "chat") "Chat with $roleLabel" else "Select $roleLabel"
        toolbar.setNavigationOnClickListener { finish() }

        rvUserList = findViewById(R.id.rvUserList)
        rvUserList.layoutManager = LinearLayoutManager(this)
        
        val userList = mutableListOf<UserDataWithChat>()
        val adapter = UserAdapter(userList) { user ->
            if (mode == "chat") {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("USER_ID", user.userData.uid)
                intent.putExtra("USER_NAME", user.userData.name)
                intent.putExtra("USER_ROLE", userRole)
                intent.putExtra("IS_ADMIN", true)
                startActivity(intent)
            } else {
                val intent = Intent(this, StudentControlActivity::class.java)
                intent.putExtra("USER_ID", user.userData.uid)
                intent.putExtra("USER_ROLE", userRole)
                startActivity(intent)
            }
        }
        rvUserList.adapter = adapter

        // Always load from Realtime Database as primary for list (more reliable registration source)
        val dbNode = if (userRole == "worker") "workers" else "users"
        database.child(dbNode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val usersToLoad = mutableListOf<UserData>()
                    for (data in snapshot.children) {
                        val uid = data.key ?: continue
                        val role = data.child("role").value?.toString() ?: ""
                        if (role.equals("admin", ignoreCase = true)) continue
                        
                        val name = data.child("name").value?.toString() ?: "Unknown"
                        val profilePic = data.child("profilePic").value?.toString() ?: ""
                        val isOnline = data.child("online").getValue(Boolean::class.java) ?: false
                        usersToLoad.add(UserData(uid, name, profilePic, isOnline))
                    }
                    
                    if (usersToLoad.isNotEmpty()) {
                        loadChatMetadata(usersToLoad, userList, adapter)
                    } else {
                        userList.clear()
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading users", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    private fun loadChatMetadata(users: List<UserData>, userList: MutableList<UserDataWithChat>, adapter: UserAdapter) {
        val chatsRef = database.child("Chats")
        val newList = mutableListOf<UserDataWithChat>()
        var loadedCount = 0
        
        if (users.isEmpty()) {
            userList.clear()
            adapter.notifyDataSetChanged()
            return
        }

        users.forEach { user ->
            val chatId = "${user.uid}_${userRole}"
            chatsRef.child(chatId).child("meta").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastMessage = snapshot.child("lastMessage").value?.toString() ?: "No messages"
                    val lastMessageTime = snapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                    val unreadCount = snapshot.child("adminUnreadCount").getValue(Int::class.java) ?: 0
                    
                    newList.add(UserDataWithChat(user, ChatMetadata(lastMessage, lastMessageTime, unreadCount)))
                    loadedCount++
                    
                    if (loadedCount == users.size) {
                        newList.sortByDescending { it.chatMetadata.lastMessageTime }
                        userList.clear()
                        userList.addAll(newList)
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == users.size) {
                        userList.clear()
                        userList.addAll(newList)
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        }
    }

    data class UserData(val uid: String, val name: String, val profilePic: String, val isOnline: Boolean = false)
    data class ChatMetadata(val lastMessage: String, val lastMessageTime: Long, val unreadCount: Int)
    data class UserDataWithChat(val userData: UserData, val chatMetadata: ChatMetadata)

    private class UserAdapter(val list: List<UserDataWithChat>, val onClick: (UserDataWithChat) -> Unit) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivUserImage)
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvLastMsg: TextView = view.findViewById(R.id.tvUserLastMsg)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvUnreadCount: TextView = view.findViewById(R.id.tvUnreadCount)
            val vOnlineIndicator: View = view.findViewById(R.id.vOnlineIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.userData.name
            holder.tvLastMsg.text = item.chatMetadata.lastMessage
            holder.vOnlineIndicator.visibility = if (item.userData.isOnline) View.VISIBLE else View.GONE
            
            if (item.chatMetadata.lastMessageTime > 0) {
                holder.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.chatMetadata.lastMessageTime))
                holder.tvTime.visibility = View.VISIBLE
            } else holder.tvTime.visibility = View.GONE
            
            if (item.chatMetadata.unreadCount > 0) {
                holder.tvUnreadCount.text = item.chatMetadata.unreadCount.toString()
                holder.tvUnreadCount.visibility = View.VISIBLE
            } else holder.tvUnreadCount.visibility = View.GONE
            
            Glide.with(holder.itemView.context).load(if (item.userData.profilePic.isNotEmpty()) item.userData.profilePic else R.drawable.ic_default_avatar).circleCrop().into(holder.ivImage)
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}