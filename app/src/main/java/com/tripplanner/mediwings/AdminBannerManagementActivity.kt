package com.tripplanner.mediwings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

data class BannerItem(
    val id: String = "",
    val url: String = "",
    val uploadProgress: Int = 0,
    val isUploading: Boolean = false
)

class AdminBannerManagementActivity : AppCompatActivity() {

    private lateinit var rvBanners: RecyclerView
    private lateinit var btnAddBanner: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    
    private val bannersList = mutableListOf<BannerItem>()
    private lateinit var adapter: BannerAdapter
    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("Banners")

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadNewBanner(it) }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Permission denied. Cannot upload banners.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_banner_management)

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Banner Management"
        toolbar.setNavigationOnClickListener { finish() }

        rvBanners = findViewById(R.id.rvBanners)
        btnAddBanner = findViewById(R.id.btnAddBanner)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        adapter = BannerAdapter(bannersList, 
            onRemove = { position -> removeBanner(position) },
            onView = { position -> viewBanner(position) }
        )
        
        rvBanners.layoutManager = LinearLayoutManager(this)
        rvBanners.adapter = adapter

        btnAddBanner.setOnClickListener {
            checkPermissionAndPickImage()
        }

        loadBanners()
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                pickImageLauncher.launch("image/*")
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Permission needed to upload banners", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadBanners() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Loading banners..."
        tvStatus.visibility = View.VISIBLE
        
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bannersList.clear()
                
                for (data in snapshot.children) {
                    val id = data.key ?: continue
                    val url = data.value?.toString() ?: continue
                    bannersList.add(BannerItem(id, url, 100, false))
                }
                
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                tvStatus.visibility = View.GONE
                
                if (bannersList.isEmpty()) {
                    tvStatus.text = "No banners yet. Click + to add."
                    tvStatus.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                tvStatus.text = "Failed to load banners"
                tvStatus.visibility = View.VISIBLE
                Toast.makeText(this@AdminBannerManagementActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadNewBanner(fileUri: Uri) {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Uploading banner..."
        tvStatus.visibility = View.VISIBLE
        btnAddBanner.isEnabled = false
        
        val timestamp = System.currentTimeMillis()
        val bannerId = database.push().key ?: return
        val storageRef = storage.reference
            .child("banners")
            .child("banner_${bannerId}_${timestamp}.jpg")

        val uploadTask = storageRef.putFile(fileUri)
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressBar.progress = progress
            tvStatus.text = "Uploading: $progress%"
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                database.child(bannerId).setValue(downloadUri.toString())
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.GONE
                        btnAddBanner.isEnabled = true
                        Toast.makeText(this, "Banner uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        progressBar.visibility = View.GONE
                        tvStatus.text = "Failed to save banner URL"
                        btnAddBanner.isEnabled = true
                        Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                tvStatus.text = "Failed to get download URL"
                btnAddBanner.isEnabled = true
                Toast.makeText(this, "Failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { exception ->
            progressBar.visibility = View.GONE
            tvStatus.text = "Upload failed"
            btnAddBanner.isEnabled = true
            Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeBanner(position: Int) {
        if (position < 0 || position >= bannersList.size) return
        
        val banner = bannersList[position]
        
        AlertDialog.Builder(this)
            .setTitle("Remove Banner")
            .setMessage("Are you sure you want to remove this banner?")
            .setPositiveButton("Remove") { dialog, _ ->
                // Delete from Firebase Storage
                val storageRef = storage.getReferenceFromUrl(banner.url)
                storageRef.delete().addOnSuccessListener {
                    // Delete from database
                    database.child(banner.id).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Banner removed", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to remove from database: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    // Even if storage deletion fails, remove from database
                    database.child(banner.id).removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Banner removed (storage cleanup may have failed)", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun viewBanner(position: Int) {
        if (position < 0 || position >= bannersList.size) return
        val banner = bannersList[position]
        
        // Show full image in dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_view_banner, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivBannerPreview)
        
        Glide.with(this)
            .load(banner.url)
            .into(imageView)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private class BannerAdapter(
        private val list: List<BannerItem>,
        private val onRemove: (Int) -> Unit,
        private val onView: (Int) -> Unit
    ) : RecyclerView.Adapter<BannerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivBanner: ImageView = view.findViewById(R.id.ivBanner)
            val btnRemove: Button = view.findViewById(R.id.btnRemoveBanner)
            val btnView: Button = view.findViewById(R.id.btnViewBanner)
            val tvBannerId: TextView = view.findViewById(R.id.tvBannerId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_banner, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val banner = list[position]
            
            holder.tvBannerId.text = "Banner ${position + 1}"
            
            Glide.with(holder.itemView.context)
                .load(banner.url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivBanner)
            
            holder.btnRemove.setOnClickListener {
                val adapterPos = holder.adapterPosition
                if (adapterPos != RecyclerView.NO_POSITION) {
                    onRemove(adapterPos)
                }
            }
            
            holder.btnView.setOnClickListener {
                val adapterPos = holder.adapterPosition
                if (adapterPos != RecyclerView.NO_POSITION) {
                    onView(adapterPos)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
