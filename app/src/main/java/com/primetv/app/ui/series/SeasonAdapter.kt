package com.primetv.app.ui.series

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.R
import com.primetv.app.data.model.Season
import com.primetv.app.databinding.ItemSeasonBinding

class SeasonAdapter(
    private val seasons: List<Season>,
    private val onClick: (Season) -> Unit
) : RecyclerView.Adapter<SeasonAdapter.VH>() {

    private var selectedPos = 0
    private var activeVH: VH? = null

    inner class VH(val binding: ItemSeasonBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(season: Season, selected: Boolean) {
            binding.tvSeasonName.text = season.name ?: "Temporada ${season.seasonNumber}"
            applyStyle(selected)
            if (selected) activeVH = this

            binding.root.setOnClickListener { triggerSelect(this, season) }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnFocusChangeListener
                when {
                    hasFocus && pos == selectedPos -> applyStyle(selected = true)
                    hasFocus -> applyFocused()
                    else -> applyStyle(pos == selectedPos)
                }
            }
        }

        fun applyStyle(selected: Boolean) {
            if (selected) {
                binding.root.setBackgroundResource(R.drawable.bg_season_selected)
                binding.tvSeasonName.setTextColor(Color.parseColor("#1A1A1A"))
                binding.tvSeasonName.alpha = 1f
            } else {
                binding.root.background = null
                binding.tvSeasonName.setTextColor(Color.parseColor("#FFFFFF"))
                binding.tvSeasonName.alpha = 0.7f
            }
        }

        private fun applyFocused() {
            binding.root.setBackgroundColor(Color.parseColor("#55FFFFFF"))
            binding.tvSeasonName.setTextColor(Color.parseColor("#FFFFFF"))
            binding.tvSeasonName.alpha = 1f
        }
    }

    fun getSelectedPosition() = selectedPos

    private fun triggerSelect(vh: VH, season: Season) {
        val pos = vh.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return
        if (pos == selectedPos) {
            activeVH = vh
            return
        }
        activeVH?.applyStyle(false)
        activeVH = vh
        selectedPos = pos
        vh.applyStyle(true)
        onClick(season)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSeasonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(seasons[position], position == selectedPos)

    override fun getItemCount() = seasons.size
}
