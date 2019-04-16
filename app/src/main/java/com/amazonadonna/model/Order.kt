package com.amazonadonna.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.ColumnInfo
import com.amazonadonna.sync.Synchronizer
import com.beust.klaxon.Json
import java.io.Serializable

@Entity(tableName = "order")
data class Order (
        @ColumnInfo(name = "numItems") @Json(name = "numItems") var numItems : Int,
        @ColumnInfo(name = "shippingAddress") @Json(name = "shippingAddress") var shippingAddress : String,
        @PrimaryKey @Json(name = "orderId") var orderId : String,
        @ColumnInfo(name = "shippedStatus") @Json(name = "shippedStatus") var shippedStatus : Boolean,
        @ColumnInfo(name = "totalCostDollars") @Json(name = "totalCostDollars") var totalCostDollars : Int,
        @ColumnInfo(name = "totalCostCents") @Json(name = "totalCostCents") var totalCostCents : Int,
        //TODO uncomment when backend route supports amOrderNumber, otherwise causes crash
        //@ColumnInfo(name = "amOrderNumber") @Json(name = "amOrderNumber") var amOrderNumber : String,
        @ColumnInfo(name = "products") @Json(name = "products") var products : List<Product>,
        @ColumnInfo(name = "cgaId") @Json(name = "cgaId") var cgaId : String,
        @ColumnInfo(name = "synced") var synced : Int = Synchronizer.SYNCED)
        //TODO change format of date
        //TODO uncomment when backend route supports orderDate, otherwise causes crash
        //@ColumnInfo(name = "orderDate") @Json(name = "orderDate") var orderDate : String)
        : Serializable {

//    fun generateOrderID() {
//        //TODO fill in logic for generating unique ID for artisan
//        var num = Random().nextInt()
//        orderId = shippingAddress + cgaId + num.toString()
//    }

}