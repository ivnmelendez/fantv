package com.primetv.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.data.model.MenuItem
import com.primetv.app.databinding.ItemMenuBinding

class MenuAdapter(
    private val items: List<MenuItem>,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuVH>() {

    private var selectedIndex = 0

    inner class MenuVH(val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem, isSelected: Boolean) {
            binding.menuIcon.setImageResource(item.iconResId)
            binding.tvTitle.text = item.title
            binding.menuIcon.alpha = if (isSelected) 1.0f else 0.5f
            binding.menuIconRlaySelected.visibility =
                if (isSelected) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener {
                val prev = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedIndex)
                onSelect(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuVH {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuVH(binding)
    }

    override fun onBindViewHolder(holder: MenuVH, position: Int) =
        holder.bind(items[position], position == selectedIndex)

    override fun getItemCount() = items.size

    fun selectIndex(index: Int) {
        val prev = selectedIndex
        selectedIndex = index
        notifyItemChanged(prev)
        notifyItemChanged(selectedIndex)
    }
}
