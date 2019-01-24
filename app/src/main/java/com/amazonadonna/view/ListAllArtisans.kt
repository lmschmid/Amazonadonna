package com.amazonadonna.view

import android.arch.persistence.room.Room
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.list_all_artisans.*
import com.amazonadonna.model.Artisan
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException
import com.google.gson.reflect.TypeToken
import android.support.v7.widget.DividerItemDecoration
import com.amazonadonna.database.AppDatabase
import com.amazonadonna.database.ArtisanDao


class ListAllArtisans : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_all_artisans)

        fetchJSON()
        //TODO add search bar

        recyclerView_listAllartisans.layoutManager = LinearLayoutManager(this)

        //load an empty list as placeholder before GET request completes
        val emptyArtisanList : List<Artisan> = emptyList()
        recyclerView_listAllartisans.adapter = ListArtisanAdapter(emptyArtisanList)

        recyclerView_listAllartisans.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    private fun fetchJSON() {
        val url = "https://4585da82.ngrok.io/artisans"
        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()

                println(body)
                val gson = GsonBuilder().create()
                //val artisans : List<com.amazonadonna.model.Artisan> =  gson.fromJson(body, mutableListOf<com.amazonadonna.model.Artisan>().javaClass)
                //System.out.print(artisans.get(0))
                val artisans : List<Artisan> = gson.fromJson(body,  object : TypeToken<List<Artisan>>() {}.type)

                runOnUiThread {
                    recyclerView_listAllartisans.adapter = ListArtisanAdapter(artisans)
                }

                //!--------------------------------!
                //SQLite Prototype Code
                val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java, "amazonadonna-main"
                ).fallbackToDestructiveMigration().build()
                val artisanDao = db.artisanDao();

                val tempArtisan = artisans.get(0)
                val tempId = tempArtisan.artisanId

                artisanDao.insertAll(artisans)
                val testArtisan = artisanDao.findByID(tempId)
                Log.d("ASSERT", "Retrieved Artisan '" + testArtisan.name + "' from the database!")
                //!--------------------------------!
            }

            override fun onFailure(call: Call?, e: IOException?) {
                println("Failed to execute request")
                Log.d("ERROR", "Failed to execute GET request to " + url)
            }
        })
    }
}