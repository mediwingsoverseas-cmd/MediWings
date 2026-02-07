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

// Data class for Chat Messages
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "sent",
    val mediaUrl: String = "",
    val mediaType: String = "" // "image", "file", or ""
)

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
    private lateinit var btnAttach: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvTypingIndicator: TextView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var ivBack: ImageView
    
    private var chatId: String? = null
    private var otherUserName: String? = null
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false
    private var userRole: String = "student" // "student" or "worker"
    private var currentUserName: String = ""
    
    private var messagesListener: ValueEventListener? = null
    private var metaListener: ValueEventListener? = null
    private var onlineListener: ValueEventListener? = null
    private var typingTimer: Timer? = null
    
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadMedia(it) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickMediaLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        try {
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()
            database = FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize Firebase. Please check your connection.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        userRole = intent.getStringExtra("USER_ROLE") ?: "student" // Get user role from intent
        
        // Validate role is either "student" or "worker"
        if (userRole != "student" && userRole != "worker") {
            Toast.makeText(this, "Invalid user role", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
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
            currentUserName = "Admin"
            // Include role in chatId for role-based separation: userId_role
            chatId = "${userId}_${userRole}"
        } else {
            // For students/workers, require Firebase authentication
            currentUserId = auth.currentUser?.uid ?: run {
                Toast.makeText(this, "Authentication required. Please log in.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val userId = intent.getStringExtra("USER_ID") ?: currentUserId
            // Include role in chatId for role-based separation: userId_role
            chatId = "${userId}_${userRole}"
            
            // Fetch current user's name from database
            fetchCurrentUserName()
        }
        
        // Final validation: ensure chatId is valid
        val currentChatId = chatId
        if (currentChatId.isNullOrEmpty() || currentChatId.contains("null")) {
            Toast.makeText(this, "Invalid chat session. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Use currentChatId instead of chatId!! to avoid smart cast issues and unnecessary !!
        val safeChatId = currentChatId

        otherUserName = intent.getStringExtra("USER_NAME") ?: "Support"

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

        val messagesRef = database.child("Chats").child(safeChatId).child("messages")
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
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
                } catch (e: Exception) {
                    Toast.makeText(this@ChatActivity, "Error loading messages. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        messagesRef.addValueEventListener(messagesListener!!)

        val metaRef = database.child("Chats").child(safeChatId).child("meta")
        metaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val typingPath = if (isAdmin) "studentTyping" else "adminTyping"
                    val isTyping = snapshot.child(typingPath).getValue(Boolean::class.java) ?: false
                    tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    // Silently ignore typing indicator errors
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Typing indicator is not critical, silently ignore errors
            }
        }
        metaRef.addValueEventListener(metaListener!!)

        val onlineRef = database.child("users").child(if (isAdmin) safeChatId else "admin").child("online")
        onlineListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                    tvOnlineStatus.text = if (isOnline) "Online" else "Offline"
                    tvOnlineStatus.setTextColor(ContextCompat.getColor(this@ChatActivity, if (isOnline) R.color.green else R.color.gray))
                } catch (e: Exception) {
                    // Silently ignore online status errors
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Online status is not critical, silently ignore errors
            }
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
                sendTextMessage(text, messagesRef)
            }
        }
        
        // Try to find attach button (if it doesn't exist, we'll handle gracefully)
        try {
            btnAttach = findViewById(R.id.btnAttach)
            btnAttach.setOnClickListener {
                checkPermissionAndPickMedia()
            }
        } catch (e: Exception) {
            // Attach button doesn't exist in layout, skip
        }
    }
    
    private fun sendTextMessage(text: String, messagesRef: DatabaseReference) {
        val safeChatId = chatId ?: return
        val messageId = messagesRef.push().key
        if (messageId == null) {
            Toast.makeText(this, "Failed to generate message ID. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val senderName = when {
            currentUserName.isNotEmpty() -> currentUserName
            isAdmin -> "Admin"
            else -> "Unknown User"
        }
        val msg = Message(
            id = messageId,
            senderId = currentUserId,
            senderName = senderName,
            message = text,
            timestamp = System.currentTimeMillis()
        )
        
        messagesRef.child(messageId).setValue(msg)
            .addOnSuccessListener {
                etMessage.setText("")
                updateTypingStatus(false)
                
                val metaRef = database.child("Chats").child(safeChatId).child("meta")
                val metaUpdates = hashMapOf<String, Any>(
                    "lastMessage" to text,
                    "lastMessageTime" to msg.timestamp,
                    "lastSenderId" to currentUserId
                )
                metaRef.updateChildren(metaUpdates)
                
                // Trigger notification for the other user
                triggerNotification(text, "text")
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to send message: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun sendMediaMessage(mediaUrl: String, mediaType: String, messagesRef: DatabaseReference) {
        val safeChatId = chatId ?: return
        val messageId = messagesRef.push().key
        if (messageId == null) {
            Toast.makeText(this, "Failed to generate message ID. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val displayText = if (mediaType == "image") "📷 Photo" else "📎 File"
        val senderName = when {
            currentUserName.isNotEmpty() -> currentUserName
            isAdmin -> "Admin"
            else -> "Unknown User"
        }
        val msg = Message(
            id = messageId,
            senderId = currentUserId,
            senderName = senderName,
            message = displayText,
            timestamp = System.currentTimeMillis(),
            mediaUrl = mediaUrl,
            mediaType = mediaType
        )
        
        messagesRef.child(messageId).setValue(msg)
            .addOnSuccessListener {
                val metaRef = database.child("Chats").child(safeChatId).child("meta")
                val metaUpdates = hashMapOf<String, Any>(
                    "lastMessage" to displayText,
                    "lastMessageTime" to msg.timestamp,
                    "lastSenderId" to currentUserId
                )
                metaRef.updateChildren(metaUpdates)
                
                // Trigger notification for the other user
                triggerNotification(displayText, mediaType)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to send media message: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun checkPermissionAndPickMedia() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                pickMediaLauncher.launch("image/*")
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Permission needed to send images", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun uploadMedia(uri: Uri) {
        val safeChatId = chatId ?: return
        Toast.makeText(this, "Uploading media...", Toast.LENGTH_SHORT).show()
        
        try {
            // Check file size
            val inputStream = contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()
            
            if (fileSize > 1024 * 1024) { // 1MB limit
                val fileSizeKB = fileSize / 1024
                Toast.makeText(this, "File too large! ${fileSizeKB}KB selected, max 1MB (1024KB)", Toast.LENGTH_LONG).show()
                return
            }
            
            if (fileSize == 0) {
                Toast.makeText(this, "Invalid file selected. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val timestamp = System.currentTimeMillis()
            val filename = "chat_media_${timestamp}.jpg"
            
            val storageRef = storage.reference
                .child("chat_media")
                .child(safeChatId)
                .child(filename)
            
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val messagesRef = database.child("Chats").child(safeChatId).child("messages")
                        sendMediaMessage(downloadUri.toString(), "image", messagesRef)
                        Toast.makeText(this, "Media sent!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to get download URL: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Upload failed: ${exception.message}. Please check your connection.", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun triggerNotification(messageText: String, messageType: String) {
        val safeChatId = chatId ?: return
        // Get the recipient's FCM token
        val recipientId = if (isAdmin) safeChatId else "admin"
        
        database.child("users").child(recipientId).child("fcmToken")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val token = snapshot.value?.toString()
                    if (!token.isNullOrEmpty()) {
                        // In a production app, you'd send this to your backend server
                        // which would then use the Firebase Admin SDK to send the notification
                        // For now, we'll update a notification trigger in the database
                        
                        val notificationData = hashMapOf<String, Any>(
                            "to" to recipientId,
                            "title" to "${if (isAdmin) "Admin" else otherUserName}",
                            "body" to messageText,
                            "timestamp" to System.currentTimeMillis(),
                            "chatId" to safeChatId,
                            "senderId" to currentUserId
                        )
                        
                        database.child("NotificationQueue").push().setValue(notificationData)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        try {
            chatId?.let { id ->
                val typingPath = if (isAdmin) "adminTyping" else "studentTyping"
                database.child("Chats").child(id).child("meta").child(typingPath).setValue(isTyping)
            }
        } catch (e: Exception) {
            // Silently ignore typing status update errors
        }
    }

    private fun markMessagesAsRead() {
        try {
            chatId?.let { id ->
                val unreadField = if (isAdmin) "adminUnreadCount" else "studentUnreadCount"
                database.child("Chats").child(id).child("meta").child(unreadField).setValue(0)
            }
        } catch (e: Exception) {
            // Silently ignore mark as read errors
        }
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
    
    private fun fetchCurrentUserName() {
        // Determine the correct database path based on role
        val userPath = if (userRole == "worker") "workers" else "users"
        
        database.child(userPath).child(currentUserId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserName = snapshot.value?.toString() ?: ""
                    if (currentUserName.isEmpty()) {
                        // Fallback: try alternate path if name not found
                        val alternatePath = if (userRole == "worker") "users" else "workers"
                        database.child(alternatePath).child(currentUserId).child("name")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    currentUserName = snapshot.value?.toString() ?: "User"
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    currentUserName = "User"
                                }
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    currentUserName = "User"
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            chatId?.let { id ->
                messagesListener?.let { database.child("Chats").child(id).child("messages").removeEventListener(it) }
                metaListener?.let { database.child("Chats").child(id).child("meta").removeEventListener(it) }
                onlineListener?.let { 
                    val onlineUserId = if (isAdmin) id else "admin"
                    database.child("users").child(onlineUserId).child("online").removeEventListener(it) 
                }
                updateTypingStatus(false)
            }
            typingTimer?.cancel()
        } catch (e: Exception) {
            // Silently handle cleanup errors
        }
    }

    private class MessageAdapter(val list: List<ChatItem>, val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val VIEW_TYPE_DATE = 0
        private val VIEW_TYPE_IN = 1
        private val VIEW_TYPE_OUT = 2

        class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textMsg: TextView = view.findViewById(R.id.tvMessage)
            val textTime: TextView = view.findViewById(R.id.tvTime)
            val textStatus: TextView? = view.findViewById(R.id.tvStatus)
            val imageMedia: ImageView? = try { view.findViewById(R.id.ivMessageMedia) } catch (e: Exception) { null }
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
                    
                    // Handle media messages
                    if (msg.mediaType == "image" && msg.mediaUrl.isNotEmpty() && vh.imageMedia != null) {
                        vh.imageMedia.visibility = View.VISIBLE
                        Glide.with(vh.imageMedia.context)
                            .load(msg.mediaUrl)
                            .centerCrop()
                            .into(vh.imageMedia)
                    } else {
                        vh.imageMedia?.visibility = View.GONE
                    }
                }
            }
        }

        override fun getItemCount() = list.size
    }
}