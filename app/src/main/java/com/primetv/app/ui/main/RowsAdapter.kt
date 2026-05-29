package com.primetv.app.ui.main

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.BaseGridView
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.R
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ItemCategoryRowBinding
import com.primetv.app.databinding.ItemRefreshCardBinding

class RowsAdapter(
    private val rows: List<ContentRow>,
    private val onItemClick: (VodStream) -> Unit,
    private val onItemFocus: (VodStream) -> Unit = {},
    private val onRefreshRow: ((String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Item {
        data class Row(val row: ContentRow, val originalIndex: Int) : Item()
        data class Refresh(val categoryId: String) : Item()
    }

    private val mutableRows = rows.toMutableList()
    private var flatItems = buildFlatItems()

    private fun buildFlatItems() = buildList {
        mutableRows.forEachIndexed { index, row ->
            add(Item.Row(row, index))
            if (row.isRefreshable && onRefreshRow != null) add(Item.Refresh(row.categoryId))
        }
    }

    fun updateRow(categoryId: String, newItems: List<VodStream>) {
        val rowIndex = mutableRows.indexOfFirst { it.categoryId == categoryId }
        if (rowIndex < 0) return
        mutableRows[rowIndex] = mutableRows[rowIndex].copy(items = newItems)
        flatItems = buildFlatItems()
        val adapterPos = flatItems.indexOfFirst { it is Item.Row && (it as Item.Row).row.categoryId == categoryId }
        if (adapterPos >= 0) notifyItemChanged(adapterPos)
    }

    fun insertOrUpdateWatchHistoryRow(items: List<VodStream>) {
        val idx = mutableRows.indexOfFirst { it.categoryId == "watch_history" }
        if (items.isEmpty()) {
            if (idx >= 0) { mutableRows.removeAt(idx); flatItems = buildFlatItems(); notifyDataSetChanged() }
            return
        }
        val row = ContentRow("watch_history", "Seguir viendo", items)
        if (idx >= 0) {
            mutableRows[idx] = row
            flatItems = buildFlatItems()
            val pos = flatItems.indexOfFirst { it is Item.Row && (it as Item.Row).row.categoryId == "watch_history" }
            if (pos >= 0) notifyItemChanged(pos)
        } else {
            mutableRows.add(0, row)
            flatItems = buildFlatItems()
            notifyItemInserted(0)
        }
    }

    private val scrollStates = HashMap<String, Parcelable?>()
    private val focusedPositions = HashMap<Int, Int>()

    fun getFocusedPosition(rowIndex: Int): Int = focusedPositions[rowIndex] ?: 0
    fun setFocusedPosition(rowIndex: Int, itemPos: Int) { focusedPositions[rowIndex] = itemPos }

    inner class RowVH(val binding: ItemCategoryRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ContentRow, position: Int) {
            binding.root.alpha = 0f
            binding.root.animate()
                .alpha(1f)
                .setDuration(320)
                .setStartDelay((position * 50L).coerceAtMost(300L))
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()

            binding.tvRowTitle.text = row.categoryName
            binding.rvRow.adapter = PosterAdapter(row.items, onItemClick, onItemFocus)

            val saved = scrollStates[row.categoryId]
            if (saved != null) {
                binding.rvRow.layoutManager?.onRestoreInstanceState(saved)
            } else {
                binding.rvRow.setSelectedPosition(0)
            }
            binding.rvRow.tag = row.categoryId
        }
    }

    inner class RefreshVH(val binding: ItemCategoryRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryId: String, position: Int) {
            binding.root.alpha = 0f
            binding.root.animate()
                .alpha(1f)
                .setDuration(320)
                .setStartDelay((position * 50L).coerceAtMost(300L))
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
            binding.tvRowTitle.text = "Actualizar eventos deportivos del día"
            binding.rvRow.adapter = RefreshCardAdapter { onRefreshRow?.invoke(categoryId) }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is RowVH) {
            val id = holder.binding.rvRow.tag as? String ?: return
            scrollStates[id] = holder.binding.rvRow.layoutManager?.onSaveInstanceState()
        }
    }

    override fun getItemViewType(position: Int) = when (flatItems[position]) {
        is Item.Row -> 0
        is Item.Refresh -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val binding = ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val gapPx = binding.rvRow.context.resources.run {
                getDimensionPixelSize(R.dimen.main_menu_view_width_closed) +
                getDimensionPixelSize(R.dimen.poster_width)
            }
            val titleStartPx = binding.rvRow.context.resources.getDimensionPixelSize(R.dimen.row_title_start)
            binding.tvRowTitle.setPaddingRelative(titleStartPx, 0, 0, 0)
            binding.rvRow.apply {
                itemAnimator = null
                windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
                windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
                windowAlignmentOffset = gapPx
                setPaddingRelative(gapPx, 0, 0, 0)
                clipToPadding = false
            }
            RowVH(binding)
        } else {
            val binding = ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val gapPx = binding.rvRow.context.resources.run {
                getDimensionPixelSize(R.dimen.main_menu_view_width_closed) +
                getDimensionPixelSize(R.dimen.poster_width)
            }
            val titleStartPx = binding.rvRow.context.resources.getDimensionPixelSize(R.dimen.row_title_start)
            binding.tvRowTitle.setPaddingRelative(titleStartPx, 0, 0, 0)
            binding.rvRow.apply {
                itemAnimator = null
                windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
                windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
                windowAlignmentOffset = gapPx
                setPaddingRelative(gapPx, 0, 0, 0)
                clipToPadding = false
            }
            RefreshVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatItems[position]) {
            is Item.Row -> (holder as RowVH).bind(item.row, position)
            is Item.Refresh -> (holder as RefreshVH).bind(item.categoryId, position)
        }
    }

    override fun getItemCount() = flatItems.size
}

class RefreshCardAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<RefreshCardAdapter.VH>() {

    inner class VH(val binding: ItemRefreshCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClick() } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRefreshCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {}
    override fun getItemCount() = 1
}
