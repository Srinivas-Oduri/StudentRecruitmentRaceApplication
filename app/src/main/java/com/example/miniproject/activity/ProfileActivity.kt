package com.example.miniproject.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.miniproject.R
import com.example.miniproject.adapter.FileViewAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var nameTextView: TextView
    private lateinit var rollNumberTextView: TextView
    private lateinit var cgpaTextView: TextView
    private lateinit var profilePhotoImageView: ImageView
    private lateinit var cameraIcon: ImageView
    private lateinit var rankTextView: TextView

    private lateinit var normalCertificationsRecyclerView: RecyclerView
    private lateinit var globalCertificationsRecyclerView: RecyclerView
    private lateinit var projectFilesRecyclerView: RecyclerView

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Add your web client ID here
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val toolbar: Toolbar = findViewById(R.id.toolbar1)
        setSupportActionBar(toolbar)

        nameTextView = findViewById(R.id.name)
        rollNumberTextView = findViewById(R.id.roll_number)
        cgpaTextView = findViewById(R.id.cgpa)
        rankTextView = findViewById(R.id.rank)
        profilePhotoImageView = findViewById(R.id.profile_photo)
        cameraIcon = findViewById(R.id.camera_icon)

        normalCertificationsRecyclerView = findViewById(R.id.normal_certifications_recycler_view)
        globalCertificationsRecyclerView = findViewById(R.id.global_certifications_recycler_view)
        projectFilesRecyclerView = findViewById(R.id.project_files_recycler_view)

        normalCertificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        globalCertificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        projectFilesRecyclerView.layoutManager = LinearLayoutManager(this)

        cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val rollNumber = snapshot.child("rollNumber").getValue(String::class.java)

                    val cgpaValue = snapshot.child("cgpa").value
                    val cgpa = when (cgpaValue) {
                        is Double -> cgpaValue.toString()
                        is String -> cgpaValue
                        is Number -> cgpaValue.toString() + ".0"
                        else -> "0.0"
                    }

                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    val rank = snapshot.child("rank").getValue(Int::class.java) ?: 0

                    nameTextView.text = name ?: "N/A"
                    rollNumberTextView.text = rollNumber ?: "N/A"
                    cgpaTextView.text = cgpa
                    rankTextView.text = rank.toString()

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profileImageUrl)
                            .into(profilePhotoImageView)
                    } else {
                        profilePhotoImageView.setImageResource(R.drawable.logo6)
                    }

                    setFiles("normal_certifications", normalCertificationsRecyclerView)
                    setFiles("global_certifications", globalCertificationsRecyclerView)
                    setFiles("project_files", projectFilesRecyclerView)
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast(this@ProfileActivity, "Failed to load user data")
                }
            })
    }

    private fun setFiles(fileType: String, recyclerView: RecyclerView) {
        val userId = auth.currentUser?.uid ?: return
        val filesRef = database.child("users").child(userId).child(fileType)

        filesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fileList = mutableListOf<String>()
                for (fileSnapshot in snapshot.children) {
                    val fileName = fileSnapshot.child("name").getValue(String::class.java)
                    fileName?.let { fileList.add(it) }
                }
                val adapter = FileViewAdapter(this@ProfileActivity, fileList) { fileName ->
                    fetchFileFromDatabase(fileType, fileName)
                }
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@ProfileActivity, "Failed to load $fileType files")
            }
        })
    }

    private fun fetchFileFromDatabase(fileType: String, fileName: String) {
        val userId = auth.currentUser?.uid ?: return
        val fileRef = database.child("users").child(userId).child(fileType).orderByChild("name").equalTo(fileName)

        fileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (fileSnapshot in snapshot.children) {
                        val fileUrl = fileSnapshot.child("url").getValue(String::class.java)
                        if (fileUrl != null) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(fileUrl)
                                setDataAndType(Uri.parse(fileUrl), "application/pdf") // Adjust MIME type as needed
                                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                            }
                            startActivity(intent)
                        }
                    }
                } else {
                    showToast(this@ProfileActivity, "File not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@ProfileActivity, "Failed to fetch file")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            if (selectedImageUri != null) {
                uploadImageToFirebase(selectedImageUri)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    database.child("users").child(userId).child("profileImageUrl").setValue(uri.toString())
                        .addOnSuccessListener {
                            Glide.with(this)
                                .load(uri)
                                .into(profilePhotoImageView)

                            showToast(this, "Profile picture updated successfully")
                        }
                        .addOnFailureListener {
                            showToast(this, "Failed to update profile picture in the database")
                        }
                }.addOnFailureListener {
                    showToast(this, "Failed to retrieve image URL")
                }
            }
            .addOnFailureListener {
                showToast(this, "Failed to upload profile picture")
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_profile -> {
                startActivity(Intent(this@ProfileActivity, EditProfileActivity::class.java))
            }
            R.id.about -> {
                startActivity(Intent(this@ProfileActivity, AboutActivity::class.java))
            }
            R.id.logout -> {
                logoutUser()
            }
            R.id.inf -> {
                startActivity(Intent(this@ProfileActivity, UserInformationActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
