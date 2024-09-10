package com.example.miniproject.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.miniproject.MainActivity
import com.example.miniproject.R
import com.example.miniproject.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().reference

        binding.btnSignUp.setOnClickListener { registerUser() }

        binding.Login.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LogInActivity::class.java))
        }
    }

    private fun registerUser() {
        val name = binding.etSinUpName.text.toString()
        val roll = binding.etRollnumber.text.toString()
        val email = binding.etSinUpEmail.text.toString()
        val password = binding.etSinUpPassword.text.toString()

        if (ValiadteForm(name, roll, email, password)) {
            showProgressBar()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            val userId = it.uid
                            val userInfo = mapOf(
                                "name" to name,
                                "rollNumber" to roll,
                                "email" to email
                            )
                            database.child("users").child(userId).setValue(userInfo)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        showToast(this, "User ID created successfully")
                                        startActivity(Intent(this, RaceActivity::class.java))
                                        finish()
                                    } else {
                                        showToast(this, "User data not saved. Try again")
                                    }
                                    hideProgressBar()
                                }
                        }
                    } else {
                        showToast(this, "User ID not created. Try again")
                        hideProgressBar()
                    }
                }
        }
    }

    private fun ValiadteForm(name: String, roll: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                binding.tilName.error = "Enter name"
                false
            }
            TextUtils.isEmpty(roll) -> {
                binding.tilRollnumber.error = "Enter roll number"
                false
            }
            TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter valid email address"
                false
            }
            TextUtils.isEmpty(password) -> {
                binding.tilPassword.error = "Enter password"
                false
            }
            else -> {
                true
            }
        }
    }
}
