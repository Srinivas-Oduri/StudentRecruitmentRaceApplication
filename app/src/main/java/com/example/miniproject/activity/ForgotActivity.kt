package com.example.miniproject.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.miniproject.R
import com.example.miniproject.databinding.ActivityForgotBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ForgotActivity : BaseActivity() {
    private lateinit var binding: ActivityForgotBinding
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityForgotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth=Firebase.auth

        binding.btnForgotPasswordSubmit.setOnClickListener{ resetPassword() }
    }

    private fun resetPassword(){
        val email=binding.etForgotPasswordEmail.text.toString()
        if(ValidateForm(email)){
            showProgressBar()
           auth.sendPasswordResetEmail(email).addOnCompleteListener {task->
               if(task.isSuccessful){
                   hideProgressBar()
                   binding.tilEmailForgetPassword.visibility=View.GONE
                   binding.tvSubmitMsg.visibility=View.VISIBLE
                   binding.btnForgotPasswordSubmit.visibility=View.GONE
               }
               else{
                   hideProgressBar()
                   showToast(this,"cant reset your password.Try again")
               }

           }
        }
    }

    private fun ValidateForm(email: String): Boolean {
        return when {
            TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmailForgetPassword.error = "Enter vaid email address"
                false
            }
            else -> true
        }

    }
}
