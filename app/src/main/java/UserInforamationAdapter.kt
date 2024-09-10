package com.example.miniproject

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserInformationAdapter(
    private val items: List<InformationItem>
) : RecyclerView.Adapter<UserInformationAdapter.UserInformationViewHolder>() {

    inner class UserInformationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileImageView: ImageView = itemView.findViewById(R.id.fileImageView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val linkTextView: TextView = itemView.findViewById(R.id.linkTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserInformationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_view_information, parent, false)
        return UserInformationViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserInformationViewHolder, position: Int) {
        val item = items[position]

        // Set description and link
        holder.descriptionTextView.text = item.description
        holder.linkTextView.text = item.link

        // Make the link clickable and open in a browser
        holder.linkTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
            holder.itemView.context.startActivity(intent)
        }

        // Load file preview (PDF or image)
        if (item.fileUrl.endsWith(".pdf")) {
            holder.fileImageView.setImageResource(R.drawable.ic_pdf) // Assuming you have a PDF icon
        } else {
            Glide.with(holder.itemView.context)
                .load(item.fileUrl)
                .placeholder(R.drawable.ic_file) // Placeholder image
                .into(holder.fileImageView)
        }

        // Handle file click to open it
        holder.fileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.fileUrl))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
