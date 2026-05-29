package com.primetv.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.R
import com.primetv.app.databinding.ItemLiveCategoryBinding

class SearchCategoryAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SearchCategoryAdapter.VH>() {

    inner class VH(val binding: ItemLiveCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String) {
            binding.tvCategory.text = name
            binding.root.setOnClickListener { onClick(name) }
            binding.root.setOnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus)
                    ContextCompat.getDrawable(v.context, R.drawable.selector_detail_btn_secondary)
                else null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLiveCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
