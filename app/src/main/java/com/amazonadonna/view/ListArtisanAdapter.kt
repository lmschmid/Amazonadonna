package com.amazonadonna.view

import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazonadonna.model.Artisan
import kotlinx.android.synthetic.main.list_artisan_cell.view.*
import android.util.Log
import com.amazonadonna.database.ImageStorageProvider
import com.amazonadonna.sync.Synchronizer


class ListArtisanAdapter (private val context: Context, private val artisans :MutableList<Artisan>) : RecyclerView.Adapter<ArtisanViewHolder> () {
    private var removedPostion = 0
    private var removedArtisan = Artisan("", "", "", "", "", "", "", 0.0,0.0,"", Synchronizer.SYNCED,0.0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtisanViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.list_artisan_cell, parent, false)
        return ArtisanViewHolder(cellForRow)
    }

    fun removeItem(viewHolder: RecyclerView.ViewHolder) {
        removedPostion = viewHolder.adapterPosition
        removedArtisan = artisans[viewHolder.adapterPosition]

        //remove functionality
        artisans.removeAt(viewHolder.adapterPosition)
        notifyItemRemoved(viewHolder.adapterPosition)
        //undo functionality
        Snackbar.make(viewHolder.itemView, "${removedArtisan.artisanName} deleted.", Snackbar.LENGTH_INDEFINITE).setAction("UNDO") {
            artisans.add(removedPostion, removedArtisan)
            notifyItemInserted(removedPostion)
        }.show()
    }

    override fun getItemCount(): Int {
        return artisans.count()
    }

    override fun onBindViewHolder(holder: ArtisanViewHolder, position: Int) {
        val artisan = artisans.get(position)
        holder.bindArtisian(artisan, context)

        holder.view.setOnClickListener{
            val intent = Intent(context, ArtisanProfile::class.java)
            intent.putExtra("artisan", artisan)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

}


class ArtisanViewHolder (val view : View) : RecyclerView.ViewHolder(view) {

    fun bindArtisian(artisan: Artisan, context: Context) {
        Log.d("URL:::::", artisan.picURL)

        var isp = ImageStorageProvider(context)
        isp.loadImageIntoUI(artisan.picURL, view.imageView_artisanProfilePic, ImageStorageProvider.ARTISAN_IMAGE_PREFIX, view.context)

        view.textView_artisanName.text = artisan.artisanName
        //view.textView_bio.text = artisan.bio
        view.textView_artisanLoc.text = (artisan.city + "," + artisan.country)
    }

}   
