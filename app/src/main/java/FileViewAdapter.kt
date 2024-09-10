package com.example.miniproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.miniproject.R

class FileViewAdapter(
    private val context: Context,
    private val fileList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<FileViewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView = itemView.findViewById(R.id.file_name)

        init {
            itemView.setOnClickListener {
                val fileName = fileNameTextView.text.toString()
                onItemClick(fileName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_view_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.fileNameTextView.text = fileList[position]
    }

    override fun getItemCount(): Int = fileList.size
}
