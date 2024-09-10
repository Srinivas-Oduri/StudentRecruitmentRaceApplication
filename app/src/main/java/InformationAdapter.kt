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

class InformationAdapter(
    private val items: List<InformationItem>,
    private val onDeleteClick: (InformationItem) -> Unit
) : RecyclerView.Adapter<InformationAdapter.InformationViewHolder>() {

    inner class InformationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileImageView: ImageView = itemView.findViewById(R.id.fileImageView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val linkTextView: TextView = itemView.findViewById(R.id.linkTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InformationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_information, parent, false)
        return InformationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InformationViewHolder, position: Int) {
        val item = items[position]

        // Set description and link
        holder.descriptionTextView.text = item.description
        holder.linkTextView.text = item.link

        // Make the link clickable
        holder.linkTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(item.link)
            }
            holder.itemView.context.startActivity(intent)
        }

        // Load file preview
        if (item.fileUrl.endsWith(".pdf")) {
            // If it's a PDF, show a PDF icon
            holder.fileImageView.setImageResource(R.drawable.ic_pdf) // Assume you have a PDF icon in your drawable folder
        } else {
            // Otherwise, try to load the image using Glide
            Glide.with(holder.itemView.context)
                .load(item.fileUrl)
                .placeholder(R.drawable.ic_file) // Placeholder image
                .into(holder.fileImageView)
        }

        // Handle file click to open it
        holder.fileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(item.fileUrl)
            }
            holder.itemView.context.startActivity(intent)
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
