package com.primetv.app.ui.live

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.databinding.ItemLiveCategoryBinding

class CategoryAdapter(
    private val items: List<Pair<String, String>>, // name, id
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(val binding: ItemLiveCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<String, String>, selected: Boolean) {
            binding.tvCategory.text = item.first
            binding.tvCategory.alpha = if (selected) 1f else 0.6f
            binding.root.setOnClickListener {
                val prev = selectedPos
                selectedPos = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onClick(item.second)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLiveCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position], position == selectedPos)

    override fun getItemCount() = items.size
}
