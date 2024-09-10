package com.example.miniproject.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.miniproject.databinding.ActivityStartBinding
import com.google.firebase.auth.FirebaseAuth

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val email = currentUser.email
            if (email == "vasaviplacements@gmail.com") {
                startActivity(Intent(this, PRaceActivity::class.java))
            } else {
                startActivity(Intent(this, RaceActivity::class.java))
            }
            finish()
        }

        binding.button.setOnClickListener {
            startActivity(Intent(this@StartActivity, LogInActivity::class.java))
        }
    }
}
