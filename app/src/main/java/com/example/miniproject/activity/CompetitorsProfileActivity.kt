package com.example.miniproject.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.miniproject.R
import com.example.miniproject.adapter.FileViewAdapter
import com.google.firebase.database.*

class CompetitorsProfileActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference

    private lateinit var nameTextView: TextView
    private lateinit var rollNumberTextView: TextView
    private lateinit var cgpaTextView: TextView
    private lateinit var profilePhotoImageView: ImageView
    private lateinit var rankTextView: TextView

    private lateinit var normalCertificationsRecyclerView: RecyclerView
    private lateinit var globalCertificationsRecyclerView: RecyclerView
    private lateinit var projectFilesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_competitors_profile)

        database = FirebaseDatabase.getInstance().reference

        nameTextView = findViewById(R.id.name)
        rollNumberTextView = findViewById(R.id.roll_number)
        cgpaTextView = findViewById(R.id.cgpa)
        rankTextView = findViewById(R.id.rank)
        profilePhotoImageView = findViewById(R.id.profile_photo)

        normalCertificationsRecyclerView = findViewById(R.id.normal_certifications_recycler_view)
        globalCertificationsRecyclerView = findViewById(R.id.global_certifications_recycler_view)
        projectFilesRecyclerView = findViewById(R.id.project_files_recycler_view)

        normalCertificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        globalCertificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        projectFilesRecyclerView.layoutManager = LinearLayoutManager(this)

        val userId = intent.getStringExtra("USER_ID")
        if (userId != null) {
            loadUserData(userId)
        } else {
            showToast("Failed to load user data")
            finish()
        }
    }

    private fun loadUserData(userId: String) {
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Fetch and display user profile information
                    val name = snapshot.child("name").getValue(String::class.java) ?: "N/A"
                    val rollNumber = snapshot.child("rollNumber").getValue(String::class.java) ?: "N/A"
                    val cgpa = snapshot.child("cgpa").getValue(Double::class.java)?.toString() ?: "0.0"
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    val rank = snapshot.child("rank").getValue(Int::class.java) ?: 0

                    nameTextView.text = name
                    rollNumberTextView.text = rollNumber
                    cgpaTextView.text = cgpa
                    rankTextView.text = rank.toString()

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@CompetitorsProfileActivity)
                            .load(profileImageUrl)
                            .into(profilePhotoImageView)
                    } else {
                        profilePhotoImageView.setImageResource(R.drawable.logo6) // Default profile image
                    }

                    // Load and display files
                    setFiles("normal_certifications", normalCertificationsRecyclerView)
                    setFiles("global_certifications", globalCertificationsRecyclerView)
                    setFiles("project_files", projectFilesRecyclerView)
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Failed to load user data: ${error.message}")
                }
            })
    }

    private fun setFiles(fileType: String, recyclerView: RecyclerView) {
        val userId = intent.getStringExtra("USER_ID") ?: return
        val filesRef = database.child("users").child(userId).child(fileType)

        filesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fileList = mutableListOf<String>()
                for (fileSnapshot in snapshot.children) {
                    val fileName = fileSnapshot.child("name").getValue(String::class.java)
                    fileName?.let { fileList.add(it) }
                }
                val adapter = FileViewAdapter(this@CompetitorsProfileActivity, fileList) { fileName ->
                    fetchFileFromDatabase(fileType, fileName)
                }
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load $fileType files")
            }
        })
    }

    private fun fetchFileFromDatabase(fileType: String, fileName: String) {
        val userId = intent.getStringExtra("USER_ID") ?: return
        val fileRef = database.child("users").child(userId).child(fileType).orderByChild("name").equalTo(fileName)

        fileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (fileSnapshot in snapshot.children) {
                        val fileUrl = fileSnapshot.child("url").getValue(String::class.java)
                        if (fileUrl != null) {
                            // Open the file using an Intent or any other method you prefer
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(fileUrl)
                                setDataAndType(Uri.parse(fileUrl), "application/pdf") // Adjust MIME type as needed
                                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                            }
                            startActivity(intent)
                        }
                    }
                } else {
                    showToast("File not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to fetch file")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
