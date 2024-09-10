package com.example.miniproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.miniproject.R
import com.example.miniproject.model.User
import com.google.android.material.imageview.ShapeableImageView

class UserAdapter(
    private val userList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.userName.text = user.name
        holder.userRollNumber.text = user.rollNumber

        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profileImageUrl)
                .into(holder.userProfilePicture)
        } else {
            holder.userProfilePicture.setImageResource(R.drawable.logo6)  // Default image
        }

        // Update rank display
        holder.userRankNumber.text = when (user.rank) {
            1 -> "🎖️"
            2 -> "🥈"
            3 -> "🥉"
            else -> user.rank.toString()
        }

        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userProfilePicture: ShapeableImageView = itemView.findViewById(R.id.user_profile_picture)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userRollNumber: TextView = itemView.findViewById(R.id.user_roll_number)
        val userRankNumber: TextView = itemView.findViewById(R.id.user_rank_number)
    }
}
