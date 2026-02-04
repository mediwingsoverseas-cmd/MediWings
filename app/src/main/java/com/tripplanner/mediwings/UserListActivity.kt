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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class UserListActivity : AppCompatActivity() {

    private lateinit var rvUserList: RecyclerView
    private lateinit var database: DatabaseReference
    private var mode: String? = null // "chat" or "control"
    private var userRole: String = "student" // "student" or "worker"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        auth = FirebaseAuth.getInstance()
        mode = intent.getStringExtra("MODE")
        userRole = intent.getStringExtra("ROLE") ?: "student"
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val roleLabel = if (userRole == "worker") "Worker" else "Student"
        supportActionBar?.title = if (mode == "chat") "Select $roleLabel to Chat" else "Select $roleLabel"
        toolbar.setNavigationOnClickListener { finish() }

        rvUserList = findViewById(R.id.rvUserList)
        rvUserList.layoutManager = LinearLayoutManager(this)
        
        database = FirebaseDatabase.getInstance().reference
        
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
                startActivity(intent)
            }
        }
        rvUserList.adapter = adapter

        // Load users
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                val usersToLoad = mutableListOf<UserData>()
                
                for (data in snapshot.children) {
                    val uid = data.key ?: continue
                    val role = data.child("role").value?.toString() ?: ""
                    
                    // Filter out admin users
                    if (role.equals("admin", ignoreCase = true)) continue
                    
                    val name = data.child("name").value?.toString() ?: "Unknown"
                    val profilePic = data.child("profilePic").value?.toString() ?: ""
                    val isOnline = data.child("online").getValue(Boolean::class.java) ?: false
                    usersToLoad.add(UserData(uid, name, profilePic, isOnline))
                }
                
                // Load chat metadata for each user
                if (usersToLoad.isNotEmpty()) {
                    loadChatMetadata(usersToLoad, userList, adapter)
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadChatMetadata(
        users: List<UserData>,
        userList: MutableList<UserDataWithChat>,
        adapter: UserAdapter
    ) {
        val chatsRef = database.child("Chats")
        var loadedCount = 0
        
        users.forEach { user ->
            chatsRef.child(user.uid).child("meta").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastMessage = snapshot.child("lastMessage").value?.toString() ?: "Click to chat"
                    val lastMessageTime = snapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                    val unreadCount = snapshot.child("adminUnreadCount").getValue(Int::class.java) ?: 0
                    
                    val chatMeta = ChatMetadata(lastMessage, lastMessageTime, unreadCount)
                    userList.add(UserDataWithChat(user, chatMeta))
                    
                    loadedCount++
                    if (loadedCount == users.size) {
                        // Sort by last message time (most recent first)
                        userList.sortByDescending { it.chatMetadata.lastMessageTime }
                        adapter.notifyDataSetChanged()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Still add user even if chat metadata fails
                    val chatMeta = ChatMetadata("Click to chat", 0, 0)
                    userList.add(UserDataWithChat(user, chatMeta))
                    
                    loadedCount++
                    if (loadedCount == users.size) {
                        userList.sortByDescending { it.chatMetadata.lastMessageTime }
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        }
    }

    data class UserData(
        val uid: String,
        val name: String,
        val profilePic: String,
        val isOnline: Boolean = false
    )

    data class ChatMetadata(
        val lastMessage: String,
        val lastMessageTime: Long,
        val unreadCount: Int
    )

    data class UserDataWithChat(
        val userData: UserData,
        val chatMetadata: ChatMetadata
    )

    private class UserAdapter(
        val list: List<UserDataWithChat>,
        val onClick: (UserDataWithChat) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivUserImage)
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvLastMsg: TextView = view.findViewById(R.id.tvUserLastMsg)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvUnreadCount: TextView = view.findViewById(R.id.tvUnreadCount)
            val vOnlineIndicator: View = view.findViewById(R.id.vOnlineIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val user = item.userData
            val meta = item.chatMetadata
            
            holder.tvName.text = user.name
            holder.tvLastMsg.text = meta.lastMessage
            
            // Show online indicator
            holder.vOnlineIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            
            // Format and display time
            if (meta.lastMessageTime > 0) {
                val time = formatTime(meta.lastMessageTime)
                holder.tvTime.text = time
                holder.tvTime.visibility = View.VISIBLE
            } else {
                holder.tvTime.visibility = View.GONE
            }
            
            // Show unread count badge
            if (meta.unreadCount > 0) {
                holder.tvUnreadCount.text = meta.unreadCount.toString()
                holder.tvUnreadCount.visibility = View.VISIBLE
            } else {
                holder.tvUnreadCount.visibility = View.GONE
            }
            
            // Load profile picture
            if (user.profilePic.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(user.profilePic).into(holder.ivImage)
            }
            
            holder.itemView.setOnClickListener { onClick(item) }
        }

        private fun formatTime(timestamp: Long): String {
            val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            return when {
                isSameDay(messageDate, today) -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
                isSameDay(messageDate, yesterday) -> "Yesterday"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        override fun getItemCount() = list.size
    }
}