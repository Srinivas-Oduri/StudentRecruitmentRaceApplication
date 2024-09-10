package com.example.miniproject.activity


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.miniproject.InformationAdapter
import com.example.miniproject.InformationItem
import com.example.miniproject.R
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class InformationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InformationAdapter
    private lateinit var informationItems: MutableList<InformationItem>
    private lateinit var databaseReference: DatabaseReference
    private var selectedFileUri: Uri? = null

    private val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedFileUri = result.data?.data
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)

        recyclerView = findViewById(R.id.recyclerView)
        val addButton: ImageButton = findViewById(R.id.addButton)

        databaseReference = FirebaseDatabase.getInstance().getReference("informationItems")
        informationItems = mutableListOf()

        adapter = InformationAdapter(informationItems) { item ->
            deleteItem(item)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Call fetchItems() to load data from Firebase
        fetchItems()

        addButton.setOnClickListener {
            showAddDialog()
        }

        // Optionally, you can load sample data to test without Firebase (already in your code)
        // loadSampleData()
    }



    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_information, null)
        val fileInputButton = dialogView.findViewById<Button>(R.id.fileInputButton)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val linkInput = dialogView.findViewById<EditText>(R.id.linkInput)

        fileInputButton.setOnClickListener {
            selectFile()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Information")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val description = descriptionInput.text.toString()
                val link = linkInput.text.toString()
                if (selectedFileUri != null) {
                    uploadFile(selectedFileUri!!) { fileUrl ->
                        val newItem = InformationItem("", fileUrl, description, link)
                        addItem(newItem)
                    }
                } else {
                    // Handle no file selected case
                    Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult.launch(intent)
    }

    private fun uploadFile(fileUri: Uri, callback: (String) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("uploads/${System.currentTimeMillis()}")
        storageReference.putFile(fileUri).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }
        }.addOnFailureListener {
            // Handle upload failure
            Log.e("InformationActivity", "Failed to upload file: ${it.message}")
        }
    }

    private fun fetchItems() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                informationItems.clear()
                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(InformationItem::class.java)
                    if (item != null) {
                        informationItems.add(item)
                        Log.d("InformationActivity", "Fetched item: $item")
                    } else {
                        Log.d("InformationActivity", "Null item fetched from database")
                    }
                }
                adapter.notifyDataSetChanged()
                Log.d("InformationActivity", "Total items fetched: ${informationItems.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("InformationActivity", "Error fetching data", error.toException())
            }
        })
    }
    private fun loadSampleData() {
        informationItems.clear()
        informationItems.add(InformationItem("1", "https://example.com/file1.jpg", "Sample Description 1", "https://example.com"))
        informationItems.add(InformationItem("2", "https://example.com/file2.jpg", "Sample Description 2", "https://example.com"))
        adapter.notifyDataSetChanged()
    }



    private fun addItem(item: InformationItem) {
        val newItemRef = databaseReference.push()
        val newItem = InformationItem(
            id = newItemRef.key ?: "",
            fileUrl = item.fileUrl,
            description = item.description,
            link = item.link
        )

        newItemRef.setValue(newItem).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("InformationActivity", "Item added successfully: $newItem")
            } else {
                Log.e("InformationActivity", "Failed to add item: ${task.exception?.message}")
                task.exception?.printStackTrace()  // Print full stack trace if there's an exception
            }
        }.addOnFailureListener { exception ->
            Log.e("InformationActivity", "Error adding item: ${exception.message}")
            exception.printStackTrace()
        }
    }

    private fun deleteItem(item: InformationItem) {
        databaseReference.child(item.id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(item.fileUrl)
                storageReference.delete().addOnSuccessListener {
                    Log.d("InformationActivity", "File deleted from storage")
                }.addOnFailureListener {
                    Log.e("InformationActivity", "Failed to delete file from storage: ${it.message}")
                }
            } else {
                Log.e("InformationActivity", "Failed to delete item from database: ${task.exception?.message}")
            }
        }
    }
}
