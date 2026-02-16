package com.tripplanner.mediwings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

data class Message(val id: String = "", val senderId: String = "", val senderName: String = "", val message: String = "", val timestamp: Long = 0, val status: String = "sent", val mediaUrl: String = "", val mediaType: String = "")
sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class DateHeader(val date: String) : ChatItem()
}

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var tvUserName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var llEmptyState: LinearLayout
    
    private var chatId: String? = null
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false
    private var userRole: String = "student"
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        userRole = intent.getStringExtra("USER_ROLE") ?: "student"
        
        val userId = if (isAdmin) intent.getStringExtra("USER_ID") else auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            finish()
            return
        }
        
        currentUserId = auth.currentUser?.uid ?: "admin"
        chatId = "${userId}_$userRole"
        
        fetchCurrentUserName()
        
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvUserName = findViewById(R.id.tvUserName)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        llEmptyState = findViewById(R.id.llEmptyState)
        
        tvUserName.text = intent.getStringExtra("USER_NAME") ?: "Support"
        
        val messagesList = mutableListOf<ChatItem>()
        val adapter = MessageAdapter(messagesList, currentUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        val messagesRef = database.child("Chats").child(chatId!!).child("messages")
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                snapshot.children.mapNotNullTo(messages) { it.getValue(Message::class.java) }
                
                messagesList.clear()
                messages.groupBy { SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(it.timestamp)) }.forEach { (date, msgs) ->
                    messagesList.add(ChatItem.DateHeader(date))
                    msgs.forEach { messagesList.add(ChatItem.MessageItem(it)) }
                }
                
                adapter.notifyDataSetChanged()
                llEmptyState.visibility = if (messagesList.isEmpty()) View.VISIBLE else View.GONE
                if (messagesList.isNotEmpty()) rvMessages.scrollToPosition(messagesList.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) sendTextMessage(text, messagesRef)
        }
    }
    
    private fun sendTextMessage(text: String, messagesRef: DatabaseReference) {
        val messageId = messagesRef.push().key ?: return
        val msg = Message(messageId, currentUserId, currentUserName, text, System.currentTimeMillis())
        messagesRef.child(messageId).setValue(msg).addOnSuccessListener { etMessage.setText("") }
    }
    
    private fun fetchCurrentUserName() {
        val dbNode = if (userRole == "worker") "workers" else "users"
        database.child(dbNode).child(currentUserId).child("name").get().addOnSuccessListener {
            currentUserName = it.value?.toString() ?: "User"
        }
    }

    private class MessageAdapter(val list: List<ChatItem>, val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_IN = 1
        private val VIEW_TYPE_OUT = 2
        private val VIEW_TYPE_DATE = 0

        override fun getItemViewType(position: Int): Int {
            return when (val item = list[position]) {
                is ChatItem.DateHeader -> VIEW_TYPE_DATE
                is ChatItem.MessageItem -> if (item.message.senderId == currentUserId) VIEW_TYPE_OUT else VIEW_TYPE_IN
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_OUT -> MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_out, parent, false))
                VIEW_TYPE_IN -> MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_in, parent, false))
                else -> DateHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = list[position]) {
                is ChatItem.MessageItem -> {
                    val vh = holder as MessageViewHolder
                    vh.textMsg.text = item.message.message
                    vh.textTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.message.timestamp))
                }
                is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).textDate.text = item.date
            }
        }

        override fun getItemCount() = list.size
        class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textMsg: TextView = view.findViewById(R.id.tvMessage)
            val textTime: TextView = view.findViewById(R.id.tvTime)
        }
        class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textDate: TextView = view.findViewById(R.id.tvDateHeader)
        }
    }
}