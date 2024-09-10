package com.example.miniproject.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.miniproject.R
import com.example.miniproject.databinding.ActivityLoginBinding
import com.example.miniproject.databinding.DialogGoogleSignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LogInActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var database: DatabaseReference  // Initialize the DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize Firebase Realtime Database
        database = Firebase.database.reference

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize ActivityResultLauncher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            }
        })

        // Set click listeners
        binding.btnSignInWithGoogle.setOnClickListener { signInWithGoogle() }
        binding.Register.setOnClickListener {
            startActivity(Intent(this@LogInActivity, RegisterActivity::class.java))
        }
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this@LogInActivity, ForgotActivity::class.java))
        }
        binding.btnSignIn.setOnClickListener { signInUser() }
    }

    private fun signInUser() {
        val email = binding.etSinInEmail.text.toString()
        val password = binding.etSinInPassword.text.toString()
        if (validateForm(email, password)) {
            showProgressBar()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    hideProgressBar()
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            database.child("users").child(userId).get().addOnCompleteListener { dataTask ->
                                if (dataTask.isSuccessful) {
                                    val dataSnapshot = dataTask.result
                                    if (dataSnapshot.exists()) {
                                        // User exists, check for specific email and password
                                        if (email == "vasaviplacements@gmail.com" && password == "Vasavi@123") {
                                            startActivity(Intent(this, PRaceActivity::class.java))
                                        } else {
                                            startActivity(Intent(this, RaceActivity::class.java))
                                        }
                                        finish()
                                    } else {
                                        showToast(this, "User not registered. Please sign up.")
                                    }
                                } else {
                                    showToast(this, "Failed to fetch user data. Please try again.")
                                }
                            }
                        } else {
                            showToast(this, "User ID not found. Please try again.")
                        }
                    } else {
                        showToast(this, "Can't sign in. Try again.")
                    }
                }
        }
    }



    private fun validateForm(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                binding.tilEmail.error = "Enter a valid email address"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter a valid email address"
                false
            }
            TextUtils.isEmpty(password) -> {
                binding.tilPassword.error = "Enter a password"
                false
            }
            else -> true
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(Exception::class.java)
            if (account != null) {
                showNameRollNumberDialog(account)
            }
        } catch (e: Exception) {
            showToast(this, "Sign-In failed: ${e.message}")
        }
    }

    private fun showNameRollNumberDialog(account: GoogleSignInAccount) {
        val dialogBinding = DialogGoogleSignupBinding.inflate(LayoutInflater.from(this))
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("Complete Sign-Up")

        dialogBuilder.setPositiveButton("Submit") { _, _ ->
            val name = dialogBinding.etName.text.toString()
            val roll = dialogBinding.etRollNumber.text.toString()
            if (validateNameRollNumber(name, roll)) {
                updateUI(account, name, roll)
            } else {
                showToast(this, "Please enter valid name and roll number")
            }
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.create().show()
    }

    private fun validateNameRollNumber(name: String, roll: String): Boolean {
        return name.isNotEmpty() && roll.isNotEmpty()
    }

    private fun updateUI(account: GoogleSignInAccount, name: String, roll: String) {
        showProgressBar()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            hideProgressBar()
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                database.child("users").child(userId).get().addOnCompleteListener { dataTask ->
                    if (dataTask.isSuccessful) {
                        val dataSnapshot = dataTask.result
                        if (dataSnapshot.exists()) {
                            // User already exists, so don't overwrite the existing data
                            startActivity(Intent(this, RaceActivity::class.java))
                            finish()
                        } else {
                            // User does not exist, proceed to save data
                            saveUserToDatabase(name, roll, account.email ?: "")
                            startActivity(Intent(this, RaceActivity::class.java))
                            finish()
                        }
                    } else {
                        showToast(this, "Failed to fetch user data. Please try again.")
                    }
                }
            } else {
                showToast(this, "Can't sign in. Try again.")
            }
        }
    }


    private fun saveUserToDatabase(name: String, roll: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val userInfo = mapOf(
            "name" to name,
            "rollNumber" to roll,
            "email" to email
        )
        database.child("users").child(userId).setValue(userInfo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(this, "User data saved successfully")
                } else {
                    showToast(this, "Failed to save user data")
                }
            }
    }
}
