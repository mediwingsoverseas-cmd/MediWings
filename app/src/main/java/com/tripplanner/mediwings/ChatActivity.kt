package com.tripplanner.mediwings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Data class for Chat Messages
data class Message(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0
)

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid ?: run {
            finish()
            return
        }
        
        // If Admin opens this, they pass the student's ID. 
        // If Student opens this, we use their own ID.
        chatId = intent.getStringExtra("USER_ID") ?: currentUserId

        database = FirebaseDatabase.getInstance().reference.child("Chats").child(chatId!!)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        val messagesList = mutableListOf<Message>()
        val adapter = MessageAdapter(messagesList, currentUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        // Listen for messages in real-time
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (data in snapshot.children) {
                    val msg = data.getValue(Message::class.java)
                    if (msg != null) messagesList.add(msg)
                }
                adapter.notifyDataSetChanged()
                if (messagesList.isNotEmpty()) {
                    rvMessages.smoothScrollToPosition(messagesList.size - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = Message(currentUserId, text, System.currentTimeMillis())
                database.push().setValue(msg).addOnSuccessListener {
                    etMessage.setText("")
                }
            }
        }
    }

    private class MessageAdapter(val list: List<Message>, val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val VIEW_TYPE_IN = 1
        private val VIEW_TYPE_OUT = 2

        class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textMsg: TextView = view.findViewById(R.id.tvMessage)
        }

        override fun getItemViewType(position: Int): Int {
            return if (list[position].senderId == currentUserId) VIEW_TYPE_OUT else VIEW_TYPE_IN
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = if (viewType == VIEW_TYPE_OUT) R.layout.item_message_out else R.layout.item_message_in
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = list[position]
            (holder as MessageViewHolder).textMsg.text = msg.message
        }

        override fun getItemCount() = list.size
    }
}