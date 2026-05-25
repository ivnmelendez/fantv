package com.primetv.app.ui.main

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.primetv.app.R
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ItemPosterBinding

class PosterAdapter(
    private val items: List<VodStream>,
    private val onClick: (VodStream) -> Unit,
    private val onFocus: (VodStream) -> Unit = {}
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    inner class VH(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setBackground(null)
            binding.root.setOnKeyListener { v, keyCode, event -> handleRowNav(v, keyCode, event) }
        }

        fun bind(item: VodStream) {
            Glide.with(binding.poster)
                .load(item.streamIcon)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.poster)

            binding.tvQuality.visibility = View.GONE

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                binding.rlayMain.foreground = if (hasFocus)
                    ContextCompat.getDrawable(binding.root.context, R.drawable.shape_card_focused)
                else null
                if (hasFocus) {
                    onFocus(item)
                    recordPosition()
                }
            }
        }

        private fun recordPosition() {
            val rowLayout = binding.root.parent?.parent as? View ?: return
            val outerRv = rowLayout.parent as? RecyclerView ?: return
            val rowIndex = outerRv.getChildAdapterPosition(rowLayout)
            if (rowIndex < 0) return
            (outerRv.adapter as? RowsAdapter)?.setFocusedPosition(rowIndex, bindingAdapterPosition)
        }
    }

    private fun handleRowNav(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_UP) return false
        if (event.action == KeyEvent.ACTION_UP) return true

        val rowLayout = v.parent?.parent as? View ?: return false
        val outerRv = rowLayout.parent as? RecyclerView ?: return false
        val currentRow = outerRv.getChildAdapterPosition(rowLayout)
        if (currentRow < 0) return false

        val targetRow = if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) currentRow + 1 else currentRow - 1
        if (targetRow < 0 || targetRow >= (outerRv.adapter?.itemCount ?: 0)) return false

        val rowsAdapter = outerRv.adapter as? RowsAdapter
        val targetItemPos = rowsAdapter?.getFocusedPosition(targetRow) ?: 0

        (outerRv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
            ?.scrollToPositionWithOffset(targetRow, 0)
        outerRv.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                outerRv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val targetVH = outerRv.findViewHolderForAdapterPosition(targetRow) ?: return
                val targetHgv = targetVH.itemView.findViewById<RecyclerView>(R.id.rv_row)
                if (targetHgv != null) {
                    (targetHgv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
                        ?.scrollToPositionWithOffset(targetItemPos, 0)
                    targetHgv.post {
                        targetHgv.findViewHolderForAdapterPosition(targetItemPos)?.itemView?.requestFocus()
                            ?: targetHgv.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                            ?: targetHgv.requestFocus()
                    }
                } else {
                    targetVH.itemView.requestFocus()
                }
            }
        })
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
