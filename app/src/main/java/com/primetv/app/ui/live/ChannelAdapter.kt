package com.primetv.app.ui.live

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.primetv.app.data.model.LiveStream
import com.primetv.app.databinding.ItemLiveChannelBinding

class ChannelAdapter(
    private val items: List<LiveStream>,
    private val onClick: (LiveStream) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    var playingIndex: Int = -1
        set(value) {
            val prev = field
            field = value
            if (prev >= 0) notifyItemChanged(prev)
            if (value >= 0) notifyItemChanged(value)
        }

    inner class VH(val binding: ItemLiveChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LiveStream, playing: Boolean) {
            binding.tvChannelName.text = item.name
            binding.tvChannelName.alpha = if (playing) 1f else 0.7f
            binding.tvChannelName.setTypeface(null, if (playing) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            Glide.with(binding.ivChannelLogo)
                .load(item.streamIcon)
                .into(binding.ivChannelLogo)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLiveChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position == playingIndex)
    override fun getItemCount() = items.size
}
