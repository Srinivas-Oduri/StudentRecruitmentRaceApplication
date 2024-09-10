
package com.example.miniproject.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.miniproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference

    private lateinit var cgpaEditText: EditText
    private lateinit var normalCertificationsLayout: LinearLayout
    private lateinit var globalCertificationsLayout: LinearLayout
    private lateinit var projectFilesLayout: LinearLayout
    private lateinit var saveButton: TextView
    private lateinit var deleteButton: TextView
    private lateinit var saveupButton: TextView

    private val PICK_FILE_REQUEST = 1
    private var fileType = ""
    private var enteredFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance().reference

        val toolbar: Toolbar = findViewById(R.id.toolbar_edit_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cgpaEditText = findViewById(R.id.cgpa_edit_text)
        normalCertificationsLayout = findViewById(R.id.normal_certifications_layout)
        globalCertificationsLayout = findViewById(R.id.global_certifications_layout)
        projectFilesLayout = findViewById(R.id.project_files_layout)
        saveButton = findViewById(R.id.save_button)
        saveupButton=findViewById(R.id.saveup_button)
        deleteButton = findViewById(R.id.delete_button)

        normalCertificationsLayout.findViewById<ImageButton>(R.id.upload_normal_cert_button)
            .setOnClickListener { uploadFile("normal_certifications") }
        globalCertificationsLayout.findViewById<ImageButton>(R.id.upload_global_cert_button)
            .setOnClickListener { uploadFile("global_certifications") }
        projectFilesLayout.findViewById<ImageButton>(R.id.upload_project_files_button)
            .setOnClickListener { uploadFile("project_files") }

        saveButton.setOnClickListener {
            saveProfileData()
        }
        saveupButton.setOnClickListener {
            startActivity(Intent(this,RaceActivity::class.java))
            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
        }
        deleteButton.setOnClickListener { promptDeleteFile() }
    }

    private fun uploadFile(type: String) {
        fileType = type

        val fileNameInput = EditText(this)
        fileNameInput.hint = "Enter File Name"

        AlertDialog.Builder(this)
            .setTitle("Enter File Name")
            .setView(fileNameInput)
            .setPositiveButton("Next") { _, _ ->
                enteredFileName = fileNameInput.text.toString().trim()
                if (enteredFileName.isNotEmpty()) {
                    promptFileSelection()
                } else {
                    Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptFileSelection() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = data?.data
            if (fileUri != null) {
                uploadFileToFirebase(fileUri)
            } else {
                Toast.makeText(this, "File selection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadFileToFirebase(fileUri: Uri) {
        val fileRef = storage.child("uploads/$fileType/$enteredFileName")
        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    val fileUrl = uri.toString()
                    saveFileUrlToDatabase(fileUrl)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "File upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFileUrlToDatabase(fileUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        val fileData = mapOf(
            "name" to enteredFileName,
            "url" to fileUrl
        )

        userRef.child(fileType).child(enteredFileName).setValue(fileData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                incrementFileCount(userRef)
                Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save file data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun incrementFileCount(userRef: DatabaseReference) {
        val countRef = userRef.child("${fileType}_count")
        countRef.get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            countRef.setValue(currentCount + 1).addOnCompleteListener {
                calculateAndSaveUserScore()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to increment file count: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptDeleteFile() {
        val fileTypeInput = EditText(this)
        fileTypeInput.hint = "Enter File Type (normal_certifications, global_certifications, project_files)"
        val fileNameInput = EditText(this)
        fileNameInput.hint = "Enter File Name to Delete"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(fileTypeInput)
            addView(fileNameInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setView(layout)
            .setPositiveButton("Delete") { _, _ ->
                fileType = fileTypeInput.text.toString().trim()
                val fileNameToDelete = fileNameInput.text.toString().trim()
                if (fileType.isNotEmpty() && fileNameToDelete.isNotEmpty()) {
                    deleteFileFromDatabase(fileNameToDelete)
                } else {
                    Toast.makeText(this, "File type and name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFileFromDatabase(fileName: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child(fileType)

        userRef.child(fileName).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fileRef = storage.child("uploads/$fileType/$fileName")
                fileRef.delete().addOnSuccessListener {
                    decrementFileCount(userRef.parent!!)
                    Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete file from storage: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to delete file data from database", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to delete file data from database: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decrementFileCount(userRef: DatabaseReference) {
        val countRef = userRef.child("${fileType}_count")
        countRef.get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            if (currentCount > 0) {
                countRef.setValue(currentCount - 1).addOnCompleteListener {
                    calculateAndSaveUserScore()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to decrement file count: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileData() {
        val cgpaString = cgpaEditText.text.toString().trim()
        val cgpa: Double = cgpaString.toDoubleOrNull() ?: 0.0  // Convert to Double

        if (cgpa == 0.0) {
            Toast.makeText(this, "Please enter a valid CGPA", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val userUpdates = mapOf(
            "cgpa" to cgpa
        )

        database.child("users").child(userId).updateChildren(userUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                calculateAndSaveUserScore()
                //profile data updated toast
                Toast.makeText(this, "CGPA updated successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,RaceActivity::class.java))
            } else {
                Toast.makeText(this, "Failed to save profile data", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to save profile data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAndSaveUserScore() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            val cgpa = snapshot.child("cgpa").getValue(Double::class.java) ?: 0.0
            val globalCertificationsCount = snapshot.child("global_certifications_count").getValue(Int::class.java) ?: 0
            val normalCertificationsCount = snapshot.child("normal_certifications_count").getValue(Int::class.java) ?: 0
            val projectFilesCount = snapshot.child("project_files_count").getValue(Int::class.java) ?: 0

            val cmin = 7.0
            val cmax = 10.0
            val gmin = 0
            val gmax = 3
            val ccmin = 2
            val ccmax = 5
            val pmin = 0
            val pmax = 5

// Prevent division by zero
            val CN = if (cgpa > cmin) (cgpa - cmin) / (cmax - cmin) else 0.0
            val GN = if (globalCertificationsCount > gmin) (globalCertificationsCount - gmin) / (gmax - gmin).toDouble() else 0.0
            val CCN = if (normalCertificationsCount > ccmin) (normalCertificationsCount - ccmin) / (ccmax - ccmin).toDouble() else 0.0
            val PN = if (projectFilesCount > pmin) (projectFilesCount - pmin) / (pmax - pmin).toDouble() else 0.0

            val score = ((CN * 0.40) + (GN * 0.20) + (CCN * 0.15) + (PN * 0.25))
            userRef.child("score").setValue(score).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    assignUserRanks()
                //user score toast
                } else {
                    Toast.makeText(this, "Failed to save user score", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to calculate user score: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun assignUserRanks() {
        val usersRef = database.child("users")

        usersRef.get().addOnSuccessListener { snapshot ->
            val usersList = mutableListOf<Pair<String, Double>>()

            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                val score = userSnapshot.child("score").getValue(Double::class.java) ?: 0.0
                val cgpa = userSnapshot.child("cgpa").getValue(Double::class.java) ?: 0.0
                val globalCertificationsCount = userSnapshot.child("global_certifications_count").getValue(Int::class.java) ?: 0
                val normalCertificationsCount = userSnapshot.child("normal_certifications_count").getValue(Int::class.java) ?: 0
                val projectFilesCount = userSnapshot.child("project_files_count").getValue(Int::class.java) ?: 0

                // Add to the users list only if cgpa and all counts are greater than 0
                if (cgpa > 0.0 || globalCertificationsCount > 0 || normalCertificationsCount > 0 || projectFilesCount > 0) {
                    usersList.add(Pair(userId, score))
                } else {
                    // Set rank to 0 for users with no CGPA and no certifications/project files
                    usersRef.child(userId).child("rank").setValue(0)
                }
            }

            // Sort and assign ranks only for users who meet the condition
            usersList.sortByDescending { it.second }
            usersList.forEachIndexed { index, pair ->
                val userId = pair.first
                val rank = index + 1
                usersRef.child(userId).child("rank").setValue(rank)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to assign ranks: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}

