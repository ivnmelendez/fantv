package com.primetv.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ItemCategoryRowBinding

class RowsAdapter(
    private val rows: List<ContentRow>,
    private val onItemClick: (VodStream) -> Unit,
    private val onItemFocus: (VodStream) -> Unit = {}
) : RecyclerView.Adapter<RowsAdapter.RowVH>() {

    inner class RowVH(val binding: ItemCategoryRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ContentRow) {
            binding.tvRowTitle.text = row.categoryName
            binding.rvRow.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = PosterAdapter(row.items, onItemClick, onItemFocus)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val binding = ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowVH(binding)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) = holder.bind(rows[position])
    override fun getItemCount() = rows.size
}
