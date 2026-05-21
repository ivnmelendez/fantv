package com.primetv.app.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.data.model.Season
import com.primetv.app.databinding.ItemSeasonBinding

class SeasonAdapter(
    private val seasons: List<Season>,
    private val onClick: (Season) -> Unit
) : RecyclerView.Adapter<SeasonAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(val binding: ItemSeasonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(season: Season, selected: Boolean) {
            binding.tvSeasonName.text = season.name ?: "Season ${season.seasonNumber}"
            binding.tvSeasonName.alpha = if (selected) 1f else 0.6f
            binding.root.setOnClickListener {
                val prev = selectedPos
                selectedPos = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onClick(season)
            }
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
