package com.devayu.calcpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File

class VaultAdapter(
    private val files: List<VaultManager.VaultFile>, // Changed to use our data class
    private val onClick: (VaultManager.VaultFile) -> Unit
) : RecyclerView.Adapter<VaultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vault_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = files[position]

        // Load the THUMBNAIL file, not the encrypted one
        // Since it's a standard JPG/PNG, Coil loads it instantly
        holder.img.load(item.thumbFile) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = files.size
}