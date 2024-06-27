package com.layoutmanager.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView

class IconAdapter : RecyclerView.Adapter<IconAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvIndex.text = "$position"
    }

    override fun getItemCount(): Int {
        return 60
    }

    class ViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvIndex: AppCompatTextView = itemView.findViewById(R.id.tv_index)

    }

}