package com.tripplanner.mediwings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class ContactEntry(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

class AdminEditContactActivity : AppCompatActivity() {

    private lateinit var rvContacts: RecyclerView
    private lateinit var btnAddContact: Button
    private lateinit var btnSaveAll: Button
    private val contactsList = mutableListOf<ContactEntry>()
    private lateinit var adapter: ContactAdapter
    private val database = FirebaseDatabase.getInstance().reference.child("CMS").child("contacts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_contact)

        // Add toolbar with back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Contact Info"
        toolbar.setNavigationOnClickListener { finish() }

        rvContacts = findViewById(R.id.rvContacts)
        btnAddContact = findViewById(R.id.btnAddContact)
        btnSaveAll = findViewById(R.id.btnSaveContact)

        adapter = ContactAdapter(contactsList) { position ->
            // Remove contact
            contactsList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, contactsList.size)
        }
        
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = adapter

        // Load existing contacts
        loadContacts()

        btnAddContact.setOnClickListener {
            val newId = database.push().key ?: return@setOnClickListener
            contactsList.add(ContactEntry(id = newId, name = "", email = "", phone = ""))
            adapter.notifyItemInserted(contactsList.size - 1)
            rvContacts.smoothScrollToPosition(contactsList.size - 1)
        }

        btnSaveAll.setOnClickListener {
            saveAllContacts()
        }
    }

    private fun loadContacts() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactsList.clear()
                
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        val id = data.key ?: continue
                        val name = data.child("name").value?.toString() ?: ""
                        val email = data.child("email").value?.toString() ?: ""
                        val phone = data.child("phone").value?.toString() ?: ""
                        contactsList.add(ContactEntry(id, name, email, phone))
                    }
                } else {
                    // Add default empty contact if none exist
                    val newId = database.push().key ?: return
                    contactsList.add(ContactEntry(id = newId, name = "", email = "", phone = ""))
                }
                
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminEditContactActivity, "Failed to load contacts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAllContacts() {
        // Build contacts data from the in-memory list by forcing adapter to sync
        val contactsToSave = mutableListOf<ContactEntry>()
        
        // Iterate through the adapter's data and get EditText values from ViewHolders
        for (i in 0 until adapter.itemCount) {
            val viewHolder = rvContacts.findViewHolderForAdapterPosition(i) as? ContactAdapter.ViewHolder
            if (viewHolder != null) {
                val name = viewHolder.etName.text.toString().trim()
                val email = viewHolder.etEmail.text.toString().trim()
                val phone = viewHolder.etPhone.text.toString().trim()
                
                // Skip completely empty entries
                if (name.isEmpty() && email.isEmpty() && phone.isEmpty()) {
                    continue
                }
                
                // Validate that non-empty entries have required fields
                if (name.isEmpty() || (email.isEmpty() && phone.isEmpty())) {
                    Toast.makeText(this, "Each contact must have a name and at least email or phone", Toast.LENGTH_LONG).show()
                    return
                }
                
                if (i < contactsList.size) {
                    contactsToSave.add(ContactEntry(contactsList[i].id, name, email, phone))
                }
            }
        }
        
        // Clear and save
        database.removeValue().addOnSuccessListener {
            val updates = mutableMapOf<String, Any>()
            
            for (contact in contactsToSave) {
                updates["${contact.id}/name"] = contact.name
                updates["${contact.id}/email"] = contact.email
                updates["${contact.id}/phone"] = contact.phone
            }
            
            if (updates.isNotEmpty()) {
                database.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Contacts Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "No contacts to save", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class ContactAdapter(
        private val list: MutableList<ContactEntry>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val etName: EditText = view.findViewById(R.id.etContactName)
            val etEmail: EditText = view.findViewById(R.id.etContactEmail)
            val etPhone: EditText = view.findViewById(R.id.etContactPhone)
            val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveContact)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = list[position]
            
            holder.etName.setText(contact.name)
            holder.etEmail.setText(contact.email)
            holder.etPhone.setText(contact.phone)
            
            holder.btnRemove.setOnClickListener {
                val adapterPos = holder.bindingAdapterPosition
                if (adapterPos != RecyclerView.NO_POSITION) {
                    onRemove(adapterPos)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}