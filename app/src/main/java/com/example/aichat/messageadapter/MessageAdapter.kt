package com.example.aichat.messageadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R

class MessagesAdapter(private val items: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_LEFT = 0
        private const val VIEW_RIGHT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isSentByUser) VIEW_RIGHT else VIEW_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_RIGHT) {
            val v = inflater.inflate(R.layout.item_message_right, parent, false)
            RightHolder(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_left, parent, false)
            LeftHolder(v)
        }
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        if (holder is RightHolder) holder.tv.text = msg.text
        if (holder is LeftHolder) holder.tv.text = msg.text
    }

    fun addMessage(msg: Message) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    class LeftHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvLeftMessage)
    }

    class RightHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvRightMessage)
    }
}
