package com.primetv.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ItemPosterBinding

class PosterAdapter(
    private val items: List<VodStream>,
    private val onClick: (VodStream) -> Unit
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    inner class VH(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VodStream) {
            Glide.with(binding.poster)
                .load(item.streamIcon)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.poster)

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
