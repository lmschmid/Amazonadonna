package com.amazonadonna.database

import android.arch.persistence.room.*
import com.amazonadonna.model.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    fun getAll(): List<Product>

    @Query("SELECT pictureURL FROM product")
    fun getAllImages(): List<String>

    @Query("SELECT * FROM product WHERE synced = (:syncState)")
    fun getAllBySyncState(syncState: Int): List<Product>

    @Query("SELECT * FROM product WHERE itemId IN (:productIds)")
    fun loadAllByIds(productIds: IntArray): List<Product>

    @Query("UPDATE product SET synced = (:syncState) WHERE itemId = (:productId)")
    fun setSyncedState(productId: String, syncState: Int)

    @Query("SELECT * FROM product WHERE itemName LIKE :productName " +
            "LIMIT 1")
    fun findByName(productName: String): Product

    @Query("SELECT * FROM product WHERE itemId LIKE :id " +
            "LIMIT 1")
    fun findByID(id: String): Product

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(product: Product)

    @Delete
    fun delete(product: Product)

    @Query("DELETE FROM product")
    fun deleteAll()
}