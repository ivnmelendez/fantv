package com.primetv.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.primetv.app.R

class SearchKeyAdapter(
    private val keys: List<String>,
    private val onKey: (String) -> Unit
) : RecyclerView.Adapter<SearchKeyAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv) {
        fun bind(key: String) {
            tv.text = key
            tv.setOnClickListener { onKey(key) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_key, parent, false) as TextView
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(keys[position])
    override fun getItemCount() = keys.size
}
