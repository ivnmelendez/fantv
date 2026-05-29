package com.primetv.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.primetv.app.R
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ItemSearchResultBinding

class SearchResultsAdapter(
    private var items: List<VodStream>,
    private val onClick: (VodStream) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.VH>() {

    inner class VH(val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VodStream) {
            binding.tvTitle.text = item.name
            Glide.with(binding.poster)
                .load(item.streamIcon)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.poster)
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                binding.rlayMain.foreground = if (hasFocus)
                    ContextCompat.getDrawable(binding.root.context, R.drawable.shape_card_focused)
                else null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    fun update(newItems: List<VodStream>) {
        items = newItems
        notifyDataSetChanged()
    }
}
