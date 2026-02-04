package com.tripplanner.mediwings

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// Data class for Chat Messages
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "sent" 
)

sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class DateHeader(val date: String) : ChatItem()
}

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var tvUserName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvTypingIndicator: TextView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var ivBack: ImageView
    
    private var chatId: String? = null
    private var otherUserName: String? = null
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false
    
    private var messagesListener: ValueEventListener? = null
    private var metaListener: ValueEventListener? = null
    private var onlineListener: ValueEventListener? = null
    private var typingTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        
        // Validate admin access - admin must have both IS_ADMIN flag and valid USER_ID to chat
        if (isAdmin) {
            val userId = intent.getStringExtra("USER_ID")
            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "Cannot start chat: No user selected", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            // For admin, use "admin" as sender ID to identify admin messages
            currentUserId = "admin"
            chatId = userId
        } else {
            // For students, require Firebase authentication
            currentUserId = auth.currentUser?.uid ?: run {
                finish()
                return
            }
            chatId = intent.getStringExtra("USER_ID") ?: currentUserId
        }
        
        otherUserName = intent.getStringExtra("USER_NAME") ?: "Support"

        database = FirebaseDatabase.getInstance().reference

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvUserName = findViewById(R.id.tvUserName)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator)
        llEmptyState = findViewById(R.id.llEmptyState)
        ivBack = findViewById(R.id.ivBack)
        
        tvUserName.text = otherUserName
        
        ivBack.setOnClickListener {
            finish()
        }

        val messagesList = mutableListOf<ChatItem>()
        val adapter = MessageAdapter(messagesList, currentUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        val messagesRef = database.child("Chats").child(chatId!!).child("messages")
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (data in snapshot.children) {
                    val msg = data.getValue(Message::class.java)
                    if (msg != null) messages.add(msg)
                }
                
                messagesList.clear()
                val groupedByDate = messages.groupBy { getDateLabel(it.timestamp) }
                groupedByDate.forEach { (dateLabel, msgs) ->
                    messagesList.add(ChatItem.DateHeader(dateLabel))
                    msgs.forEach { messagesList.add(ChatItem.MessageItem(it)) }
                }
                
                adapter.notifyDataSetChanged()
                if (messagesList.isNotEmpty()) {
                    llEmptyState.visibility = View.GONE
                    rvMessages.scrollToPosition(messagesList.size - 1)
                } else {
                    llEmptyState.visibility = View.VISIBLE
                }
                markMessagesAsRead()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        messagesRef.addValueEventListener(messagesListener!!)

        val metaRef = database.child("Chats").child(chatId!!).child("meta")
        metaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingPath = if (isAdmin) "studentTyping" else "adminTyping"
                val isTyping = snapshot.child(typingPath).getValue(Boolean::class.java) ?: false
                tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        metaRef.addValueEventListener(metaListener!!)

        val onlineRef = database.child("users").child(if (isAdmin) chatId!! else "admin").child("online")
        onlineListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                tvOnlineStatus.text = if (isOnline) "Online" else "Offline"
                tvOnlineStatus.setTextColor(ContextCompat.getColor(this@ChatActivity, if (isOnline) R.color.green else R.color.gray))
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        onlineRef.addValueEventListener(onlineListener!!)

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTypingStatus(true)
            }
            override fun afterTextChanged(s: Editable?) {
                typingTimer?.cancel()
                typingTimer = Timer()
                typingTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        updateTypingStatus(false)
                    }
                }, 2000)
            }
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val messageId = messagesRef.push().key ?: return@setOnClickListener
                val msg = Message(
                    id = messageId,
                    senderId = currentUserId,
                    senderName = if (isAdmin) "Admin" else "Student",
                    message = text,
                    timestamp = System.currentTimeMillis()
                )
                
                messagesRef.child(messageId).setValue(msg).addOnSuccessListener {
                    etMessage.setText("")
                    updateTypingStatus(false)
                    
                    val metaUpdates = hashMapOf<String, Any>(
                        "lastMessage" to text,
                        "lastMessageTime" to msg.timestamp,
                        "lastSenderId" to currentUserId
                    )
                    metaRef.updateChildren(metaUpdates)
                }
            }
        }
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        val typingPath = if (isAdmin) "adminTyping" else "studentTyping"
        database.child("Chats").child(chatId!!).child("meta").child(typingPath).setValue(isTyping)
    }

    private fun markMessagesAsRead() {
        val unreadField = if (isAdmin) "adminUnreadCount" else "studentUnreadCount"
        database.child("Chats").child(chatId!!).child("meta").child(unreadField).setValue(0)
    }

    private fun getDateLabel(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(messageDate, today) -> "Today"
            isSameDay(messageDate, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.let { database.child("Chats").child(chatId!!).child("messages").removeEventListener(it) }
        metaListener?.let { database.child("Chats").child(chatId!!).child("meta").removeEventListener(it) }
        onlineListener?.let { database.child("users").child(if (isAdmin) chatId!! else "admin").child("online").removeEventListener(it) }
        typingTimer?.cancel()
        updateTypingStatus(false)
    }

    private class MessageAdapter(val list: List<ChatItem>, val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val VIEW_TYPE_DATE = 0
        private val VIEW_TYPE_IN = 1
        private val VIEW_TYPE_OUT = 2

        class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textMsg: TextView = view.findViewById(R.id.tvMessage)
            val textTime: TextView = view.findViewById(R.id.tvTime)
            val textStatus: TextView? = view.findViewById(R.id.tvStatus)
        }

        class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textDate: TextView = view.findViewById(R.id.tvDateHeader)
        }

        override fun getItemViewType(position: Int): Int {
            return when (val item = list[position]) {
                is ChatItem.DateHeader -> VIEW_TYPE_DATE
                is ChatItem.MessageItem -> if (item.message.senderId == currentUserId) VIEW_TYPE_OUT else VIEW_TYPE_IN
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_DATE -> DateHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false))
                VIEW_TYPE_OUT -> MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_out, parent, false))
                else -> MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message_in, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = list[position]) {
                is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).textDate.text = item.date
                is ChatItem.MessageItem -> {
                    val msg = item.message
                    val vh = holder as MessageViewHolder
                    vh.textMsg.text = msg.message
                    vh.textTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                    vh.textStatus?.text = when (msg.status) {
                        "read" -> "✓✓"
                        "delivered" -> "✓✓"
                        else -> "✓"
                    }
                }
            }
        }

        override fun getItemCount() = list.size
    }
}