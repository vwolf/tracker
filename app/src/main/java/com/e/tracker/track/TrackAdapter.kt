package com.e.tracker.track

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.e.tracker.R
import com.e.tracker.database.TrackModel
import com.e.tracker.databinding.TrackListItemViewBinding



class TrackAdapter(
    private val clickListener: TrackListener,
    private val editIconClickListener: (TrackModel, Int) -> Unit) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {


    var data = listOf<TrackModel>()
    set(value) {
        field = value
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    override fun getItemCount() = data.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position], clickListener, editIconClickListener)
    }


    class ViewHolder(val binding: TrackListItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(item: TrackModel, clickListener: TrackListener, editIconClickListener: (TrackModel, Int) -> Unit) {
            //this.layoutPosition

            binding.track = item
            itemView.findViewById<TextView>(R.id.track_list_name).text = item.trackName
            itemView.findViewById<TextView>(R.id.track_list_description).text = item.trackDescription
            itemView.findViewById<ImageView>(R.id.type_image).setImageResource( when (item.type) {
                "walking" -> R.drawable.ic_directions_walk_black_24dp
                "biking" -> R.drawable.ic_directions_bike_black_24dp
                else -> R.drawable.ic_directions_walk_black_24dp
            })
            if (item.id < 1) {
                itemView.setBackgroundColor(  ContextCompat.getColor(viewHolderContext, R.color.schema_one_blue_light) )
            }

            binding.clickListener = clickListener
            binding.editImage.setOnClickListener { editIconClickListener(item, this.layoutPosition)}
            binding.executePendingBindings()
        }

        companion object {
            private lateinit var viewHolderContext: Context
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                viewHolderContext = parent.context
                val binding = TrackListItemViewBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }


}



class TrackListener(val clickListener: (tn: TrackModel) -> Unit) {
    fun onClick(track: TrackModel) = clickListener(track)
}