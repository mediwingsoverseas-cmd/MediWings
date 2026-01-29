package com.tripplanner.mediwings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class Message(val senderId: String = "", val message: String = "", val timestamp: Long = 0)

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return
        
        // If Admin opens this, they pass the student's ID. 
        // If Student opens this, we use their own ID.
        chatId = intent.getStringExtra("USER_ID") ?: currentUserId

        database = FirebaseDatabase.getInstance().reference.child("Chats").child(chatId!!)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        val messagesList = mutableListOf<Message>()
        val adapter = MessageAdapter(messagesList, currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (data in snapshot.children) {
                    val msg = data.getValue(Message::class.java)
                    if (msg != null) messagesList.add(msg)
                }
                adapter.notifyDataSetChanged()
                rvMessages.scrollToPosition(messagesList.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = Message(currentUserId, text, System.currentTimeMillis())
                database.push().setValue(msg)
                etMessage.setText("")
            }
        }
    }

    // Simple Adapter for Chat
    private class MessageAdapter(val list: List<Message>, val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = list[position]
            val textView = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            textView.text = if (msg.senderId == currentUserId) "Me: ${msg.message}" else "Other: ${msg.message}"
        }
        override fun getItemCount() = list.size
    }
}