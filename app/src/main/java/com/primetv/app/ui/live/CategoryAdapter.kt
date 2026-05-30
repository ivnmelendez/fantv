package com.primetv.app.ui.live

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.databinding.ItemLiveCategoryBinding

class CategoryAdapter(
    private val items: List<Pair<String, String>>, // name, id
    private val onSelect: (String) -> Unit,  // fires on focus — updates channel list
    private val onConfirm: () -> Unit        // fires on click — moves focus to channels
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(val binding: ItemLiveCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<String, String>, selected: Boolean) {
            binding.tvCategory.text = item.first
            binding.tvCategory.alpha = if (selected) 1f else 0.55f
            binding.tvCategory.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)

            binding.root.setOnFocusChangeListener { v, hasFocus ->
                v.setBackgroundColor(if (hasFocus) 0x22FFFFFF else 0x00000000)
                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_ID.toInt()) return@setOnFocusChangeListener
                    val prev = selectedPos
                    selectedPos = pos
                    if (prev != pos) {
                        notifyItemChanged(prev)
                        notifyItemChanged(pos)
                    }
                    onSelect(item.second)
                }
            }

            binding.root.setOnClickListener {
                onConfirm()
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
