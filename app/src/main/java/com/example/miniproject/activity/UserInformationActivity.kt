package com.example.miniproject.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.miniproject.InformationAdapter
import com.example.miniproject.InformationItem
import com.example.miniproject.R
import com.example.miniproject.UserInformationAdapter
import com.google.firebase.database.*

class UserInformationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserInformationAdapter
    private lateinit var informationItems: MutableList<InformationItem>
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_information)

        recyclerView = findViewById(R.id.recyclerView)

        databaseReference = FirebaseDatabase.getInstance().getReference("informationItems")
        informationItems = mutableListOf()

        adapter = UserInformationAdapter(informationItems)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchItems()
    }

    private fun fetchItems() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                informationItems.clear()
                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(InformationItem::class.java)
                    if (item != null) {
                        informationItems.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }
}
