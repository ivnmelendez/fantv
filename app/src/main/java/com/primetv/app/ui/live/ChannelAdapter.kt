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

    inner class VH(val binding: ItemLiveChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LiveStream) {
            binding.tvChannelName.text = item.name
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

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
