package com.primetv.app.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.primetv.app.data.model.Episode
import com.primetv.app.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val episodes: List<Episode>,
    private val onClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.VH>() {

    inner class VH(val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ep: Episode) {
            val seasonNum  = ep.season.toString().padStart(2, '0')
            val episodeNum = ep.episodeNum.toString().padStart(2, '0')
            binding.tvEpNum.text       = "T$seasonNum:E$episodeNum"
            binding.tvEpisodeTitle.text   = ep.title ?: ""
            binding.tvEpisodePlot.text    = ep.info?.plot ?: ""
            binding.tvEpisodeDuration.text = ep.info?.duration ?: ""

            Glide.with(binding.ivThumbnail.context)
                .load(ep.info?.movieImage)
                .into(binding.ivThumbnail)

            binding.root.setOnClickListener { onClick(ep) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(episodes[position])
    override fun getItemCount() = episodes.size
}
