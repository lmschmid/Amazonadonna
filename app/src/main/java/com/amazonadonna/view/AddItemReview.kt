package com.amazonadonna.view

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.amazonadonna.model.Artisan
import com.amazonadonna.model.Product
import kotlinx.android.synthetic.main.activity_add_item_review.*
import okhttp3.*
import java.io.IOException
import android.annotation.TargetApi
import android.content.ContentUris
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.amazonadonna.sync.ProductSync
import java.io.File
import java.util.*


class AddItemReview : AppCompatActivity() {

    //private var photoFile: File? = null
    private val addItemURL = "https://99956e2a.ngrok.io/item/add"
    private val addItemImageURL = "https://99956e2a.ngrok.io/item/updateImage"
    private val editItemURL = "https://99956e2a.ngrok.io/item/editItem"
    var editMode : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item_review)

//        val bitmap = this.intent.getParcelableExtra<Parcelable>("image0") as Bitmap
//        addItemReview_Image.setImageBitmap(bitmap)

        val artisan = intent.extras?.getSerializable("selectedArtisan") as Artisan
        val product = intent.extras?.getSerializable("product") as Product
        var photoFilesArr = intent.extras?.getSerializable("photoFiles") as ArrayList<File?>
        editMode = intent.extras?.get("editMode") as Boolean


        var categoryString = ""
        if (product.specificCategory == "-- Not Applicable --") {
            categoryString = product.category + " > " + product.subCategory
        } else {
            categoryString = product.category + " > " + product.subCategory + " > " + product.specificCategory
        }
        val priceString = "$ " + product.price.toString()
        val productionTimeString = this.resources.getString(R.string.item_review_usually_ships) + product.productionTime
        val productQuantityString = product.itemQuantity.toString() + this.resources.getString(R.string.number_in_stock)
        addItemReview_categories.text = categoryString
        addIemReview_ProductNameTF.text = product.itemName
        addItemReview_itemPrice.text = priceString
        addItemReview_itemDescription.text = product.description
        addItemReview_shippingOption.text = product.shippingOption
        addItemReview_ItemQuantity.text = productQuantityString
        addItemReview_itemTime.text = productionTimeString

        addItemReview_continueButton.setOnClickListener {
            reviewDone(artisan, product, photoFilesArr)
        }
    }

    //TODO user horizontal scroll bar to make a nicer item pic gallery
    private fun reviewDone (artisan: Artisan, product: Product, photos: ArrayList<File?>) {
        //submitToDB(product, artisan, photos)
        product.generateTempID()

        if (editMode) {
            //ProductSync.editProduct(applicationContext, product, artisan, photos)
            submitToDB(product, artisan, photos)
        }
        else {
            ProductSync.addProduct(applicationContext, product, artisan, photos)
            runOnUiThread {
                showResponseDialog(artisan, true)
            }
        }
    }

    private fun submitDismiss(artisan: Artisan) {
        val intent = Intent(this, ArtisanProfile::class.java)
        Log.i("AddItemReview", "review done adding item to db")
        intent.putExtra("artisan", artisan)
        startActivity(intent)
        finish()
    }

    private fun submitToDB(product: Product, artisan: Artisan, photos: ArrayList<File?>) {
        var status = false
        var url = ""
        if (editMode) {
            Log.i("editmode", editMode.toString())
            url = editItemURL
        } else {
            url = addItemURL
        }
        //TODO add process bar to show submitting process
        val requestBody = FormBody.Builder().add("itemName", product.itemName)
                .add("price", product.price.toString())
                .add("description", product.description)
                .add("artisanId", product.artisanId)
                .add("category", product.category)
                .add("subCategory", product.subCategory)
                .add("specificCategory", product.specificCategory)
                .add("shippingOption", product.shippingOption)
                .add("itemQuantity", product.itemQuantity.toString())
                .add("productionTime", product.productionTime.toString())
        if (editMode) {
            requestBody.add("itemId", product.itemId)
        }
        Log.d("AddItemReview", product.toString())
        val client = OkHttpClient()

        val request = Request.Builder()
                .url(url)
                .post(requestBody.build())
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.i("AddItemReview", "prodcut id: $body")
                if (!editMode) {
                    product.itemId = body!!
                }
                runOnUiThread {
                    showResponseDialog(artisan, true)
                }
                var i = 0
                for (photo in photos) {
                    if (photo != null) {
                        submitPictureToDB(product, photo, i)
                    }
                    i++
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("AddItemReview", "failed to do POST request to database: " + url)
                runOnUiThread {
                    showResponseDialog(artisan, false)
                }
            }
        })

        //showResponseDialog(artisan, status)
    }

    private fun showResponseDialog(artisan: Artisan, status: Boolean) {
        val builder = AlertDialog.Builder(this@AddItemReview)
        if (status) {
            builder.setTitle(this.resources.getString(R.string.item_review_response_dialog_title))
            builder.setMessage(this.resources.getString(R.string.item_review_listing_success))
            builder.setOnDismissListener {
                submitDismiss(artisan)
            }
        } else
        {
            builder.setTitle(this.resources.getString(R.string.item_review_response_dialog_title))
            builder.setMessage(this.resources.getString(R.string.item_review_listing_failed))
        }

        val dialog : AlertDialog = builder.create()
        dialog.show()
    }

    //TODO change product pic to an array of url as it can have more than one pic or have multiple fields for the images
    fun submitPictureToDB(product: Product, photoFile: File, index: Int) {
        Log.d("hitFunction", "we here")
        val sourceFile = photoFile
        Log.d("AddItemReview", "File...::::" + sourceFile + " : " + sourceFile.exists())

        val MEDIA_TYPE = MediaType.parse("image/png")

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("itemId", product.itemId)
                .addFormDataPart("picIndex", index.toString())
                .addFormDataPart("image", "itemImage.png", RequestBody.create(MEDIA_TYPE, sourceFile))
                .build()

        val request = Request.Builder()
                .url(addItemImageURL)
                .post(requestBody)
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.i("AddItemImage", body)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("ERROR", "failed to do POST request to database")
            }
        })
    }

    @TargetApi(19)
    private fun createImageFile(data: Intent?) {
        var imagePath: String? = null
        val uri = data!!.data
        if (DocumentsContract.isDocumentUri(this, uri)){
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri.authority){
                val id = docId.split(":")[1]
                val selsetion = MediaStore.Images.Media._ID + "=" + id
                imagePath = imagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selsetion)
            }
            else if ("com.android.providers.downloads.documents" == uri.authority){
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(docId))
                imagePath = imagePath(contentUri, null)
            }
        }
        else if ("content".equals(uri.scheme, ignoreCase = true)){
            imagePath = imagePath(uri, null)
        }
        else if ("file".equals(uri.scheme, ignoreCase = true)){
            imagePath = uri.path
        }

        //photoFile = File(imagePath)
    }

    private fun imagePath(uri: Uri?, selection: String?): String {
        var path: String? = null
        val cursor = contentResolver.query(uri, null, selection, null, null )
        if (cursor != null){
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }
}
