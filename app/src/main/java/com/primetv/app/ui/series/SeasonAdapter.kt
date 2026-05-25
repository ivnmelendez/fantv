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

    inner class VH(val binding: ItemSeasonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(season: Season, selected: Boolean) {
            binding.tvSeasonName.text = season.name ?: "Temporada ${season.seasonNumber}"
            applyStyle(selected)
            binding.root.setOnClickListener { select(season) }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) select(season)
            }
        }

        private fun applyStyle(selected: Boolean) {
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

        private fun select(season: Season) {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            val prev = selectedPos
            selectedPos = pos
            notifyItemChanged(prev)
            notifyItemChanged(selectedPos)
            onClick(season)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSeasonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(seasons[position], position == selectedPos)

    override fun getItemCount() = seasons.size
}
