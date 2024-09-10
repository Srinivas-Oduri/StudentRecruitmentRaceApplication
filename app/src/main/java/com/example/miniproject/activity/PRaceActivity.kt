package com.example.miniproject.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.miniproject.R
import com.example.miniproject.adapter.UserAdapter
import com.example.miniproject.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class PRaceActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val filteredUserList = mutableListOf<User>()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Ensure this ID is correct
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        usersRecyclerView = findViewById(R.id.users_recycler_view)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(filteredUserList) { selectedUser ->
            handleUserItemClick(selectedUser)
        }
        usersRecyclerView.adapter = userAdapter
        fetchUsers()

        val searchView: SearchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.prace_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.information -> {
                // Handle information action
                startActivity(Intent(this, InformationActivity::class.java))
            }
            R.id.logout -> {
                logoutUser()
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

    private fun fetchUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        if (it.rank > 0) {  // Exclude users with rank 0
                            userList.add(it.copy(id = userSnapshot.key ?: ""))
                        }
                    }
                }
                userList.sortBy { it.rank }
                filteredUserList.clear()
                filteredUserList.addAll(userList)
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun filterUsers(query: String?) {
        val lowerCaseQuery = query?.toLowerCase(Locale.getDefault())
        filteredUserList.clear()
        if (lowerCaseQuery.isNullOrEmpty()) {
            filteredUserList.addAll(userList)
        } else {
            for (user in userList) {
                if (user.rollNumber.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredUserList.add(user)
                }
            }
        }
        userAdapter.notifyDataSetChanged()
    }

    private fun handleUserItemClick(selectedUser: User) {
        val currentUserId = auth.currentUser?.uid

        if (selectedUser.id == currentUserId) {
            startActivity(Intent(this, ProfileActivity::class.java))
        } else {
            val intent = Intent(this, CompetitorsProfileActivity::class.java)
            intent.putExtra("USER_ID", selectedUser.id)
            startActivity(intent)
        }
    }
}
