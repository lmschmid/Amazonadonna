package com.amazonadonna.view

import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import com.amazonadonna.database.ImageStorageProvider
import com.amazonadonna.model.Artisan
import com.amazonadonna.sync.ArtisanSync
import com.amazonadonna.view.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_add_artisan.*
import kotlinx.android.synthetic.main.activity_artisan_profile.*
import kotlinx.android.synthetic.main.activity_edit_artisan.*
import okhttp3.*
import java.io.*

class EditArtisan : AppCompatActivity() {
    private var photoFile: File? = null
    private val fileName: String = "editProfilePic.png"
    private val editArtisanURL = "https://99956e2a.ngrok.io/artisan/edit"
    private val updateArtisanURL = "https://99956e2a.ngrok.io/artisan/updateImage"
    private val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034
    private val CHOOSE_PHOTO_ACTIVITY_REQUEST_CODE = 1046
    private lateinit var pic : Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_artisan)
        val IMAGE_UPLOADING_PERMISSION = 3
//        ArtisanSync.sync(this, cgaId)
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), IMAGE_UPLOADING_PERMISSION)

        val oldArtisan = intent.extras?.getSerializable("artisan") as Artisan
        Log.d("HOT FIX 12", oldArtisan.toString())
        //fill in information from old artisan
        editArtisanBio.setText(oldArtisan.bio)
        editArtisan_cc.setText(oldArtisan.city + "," + oldArtisan.country)
        editArtisan_name.setText(oldArtisan.artisanName)
        editArtisan_number.setText(oldArtisan.contactNumber)

        /*if (oldArtisan.picURL != "Not set") {
            Picasso.with(this).load(oldArtisan.picURL).into(this.editArtisan_pic)
        } else {
            this.editArtisan_pic.setImageResource(R.drawable.placeholder)
        }*/
        var isp = ImageStorageProvider(applicationContext)
        isp.loadImageIntoUI(oldArtisan.picURL, this.editArtisan_pic, ImageStorageProvider.ARTISAN_IMAGE_PREFIX, applicationContext)

        pic = editArtisan_pic.drawable

        editArisan_SaveButton.setOnClickListener {
            updateArtisan(oldArtisan)
        }

        editSelectPicture.setOnClickListener {
            selectPicture()
        }

        editTakePicture.setOnClickListener {
            takePicture()
        }
    }

    private fun selectPicture() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CHOOSE_PHOTO_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = File(externalCacheDir, fileName)

        if(photoFile!!.exists()) {
            photoFile!!.delete()
        }
        photoFile!!.createNewFile()

        val fileProvider = FileProvider.getUriForFile(this@EditArtisan, "com.amazonadonna.amazonhandmade.fileprovider", photoFile!!)

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun setImageView() {
        val takenImage = BitmapFactory.decodeFile(photoFile!!.absolutePath)
        // RESIZE BITMAP, see section below
        // Load the taken image into a preview
        val ivPreview = findViewById(R.id.editArtisan_pic) as ImageView
        ivPreview.setImageBitmap(takenImage)
    }

    @TargetApi(19)
    private fun createImageFile(data: Intent?) {
        var imagePath: String? = null
        val uri = data!!.data
        val w = 331
        val h = 273
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

        photoFile = File(imagePath)

        //pre-scaling bits
        val uri_test = FileProvider.getUriForFile(this@EditArtisan, "com.amazonadonna.amazonhandmade.fileprovider", photoFile!!)
        val bm = loadScaledBitmap(uri_test, w, h)
        val stream = ByteArrayOutputStream()
        bm!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        var byteArray = stream.toByteArray()
        //byteArray = ByteArray(photoFile!!.length().toInt())

        try {


            //convert array of bytes into file
            val fileOuputStream = FileOutputStream(photoFile)
            fileOuputStream.write(byteArray)
            fileOuputStream.close()
            Log.d("SKETIT", "lol")

            println("Done")
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK) {
                    val w = 331
                    val h = 273
                    val dataURI = FileProvider.getUriForFile(this@EditArtisan, "com.amazonadonna.amazonhandmade.fileprovider", photoFile!!)
                    try {
                        Log.d("Add Artisan post photo", "Success")
                        Log.d("Add Artisan post photo", "Exists?: " + photoFile!!.exists())
                        val bm = loadScaledBitmap(dataURI, w, h)
                        val ivPreview = findViewById(R.id.editArtisan_pic) as ImageView
                        ivPreview.setImageBitmap(bm)
                        //setImageView()
                    } catch (e: Error) {
                        Log.d("Add Artisan post Photo", "it failed")
                    }
//                    try {
//                        Log.d("EditArtisan post photo", "Success")
//                        Log.d("EditArtisan post photo", "Exists?: " + photoFile!!.exists())
//                        setImageView()
//                    }
//                    catch(e: Error) {
//                        Log.d("EditArtisan post Photo", "it failed")
//                    }
                }
            CHOOSE_PHOTO_ACTIVITY_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        val w = 331
                        val h = 273
                        val dataURI = data.data
                        Log.d("HEIGHT", h.toString())
                        Log.d("WIDTH", w.toString())
                        Log.d("dataURI", dataURI.toString())
                        createImageFile(data)


                        try {
                            Log.d("Add Artisan post photo", "Success")
                            Log.d("Add Artisan post photo", "Exists?: " + photoFile!!.exists())
                            val bm = loadScaledBitmap(dataURI, w, h)
                            val ivPreview = findViewById(R.id.editArtisan_pic) as ImageView
                            ivPreview.setImageBitmap(bm)
                            //setImageView()
                        }
                        catch(e: Error) {
                            Log.d("Add Artisan post Photo", "it failed")
                        }
//                        createImageFile(data)
//                        Log.d("Add Artisan postGallery", "File:  Exists?: " + photoFile!!.exists())
//                        setImageView()
                    }
                    else {
                        Log.d("Add Artisan postGallery", "Data was null")
                    }
//                    if (data != null) {
//                        createImageFile(data)
//                        Log.d("EditArtisan postGallery", "File:  Exists?: " + photoFile!!.exists())
//                        setImageView()
//                    }
//                    else {
//                        Log.d("EditArtisan postGallery", "Data was null")
//                    }
                }
        }
    }


    @Throws(FileNotFoundException::class)
    private fun loadScaledBitmap(src: Uri, req_w: Int, req_h: Int): Bitmap? {

        var bm: Bitmap? = null

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(baseContext.contentResolver.openInputStream(src), null, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, req_w, req_h)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        bm = BitmapFactory.decodeStream(
                baseContext.contentResolver.openInputStream(src), null, options)

        return bm
    }

    public fun calculateInSampleSize(options : BitmapFactory.Options,
                                     reqWidth : Int, reqHeight : Int): Int {
        // Raw height and width of image
        val height = options.outHeight;
        val width = options.outWidth;
        var inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            val heightRatio = Math.round((height.toFloat()) / Math.round(reqHeight.toFloat()))
            val widthRatio = Math.round(width.toFloat() / reqHeight.toFloat())

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            if (heightRatio < widthRatio)
                inSampleSize = heightRatio
            else
                inSampleSize = widthRatio
            // inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio
        }

        return inSampleSize
    }

    private fun parseLoc () : Pair<String, String> {
        val rawLoc = editArtisan_cc.text.toString()
        val ind = rawLoc.indexOf(',')
        return Pair(rawLoc.substring(0, ind), rawLoc.substring(ind+1))
    }

    //TODO need to update pic on back button
    private fun updateArtisan(oldArtisan : Artisan) {
        if (!validateFields()) {
            return
        } else {

            oldArtisan.artisanName = editArtisan_name.text.toString()
            oldArtisan.bio = editArtisanBio.text.toString()
            oldArtisan.city = parseLoc().first
            oldArtisan.country = parseLoc().second
            oldArtisan.contactNumber = editArtisan_number.text.toString()


            var newPhoto: File? = null
            if (editArtisan_pic.drawable != pic) {
                newPhoto = photoFile
            }

            ArtisanSync.updateArtisan(applicationContext, oldArtisan, newPhoto)

            //submitToDB(oldArtisan)
            val intent = Intent(this, ArtisanProfile::class.java)
            intent.putExtra("artisan", oldArtisan)
            startActivity(intent)
            finish()
        }
    }

    private fun submitToDB(oldArtisan: Artisan) {
        var updatePic = false

        val requestBody = FormBody.Builder().add("artisanId", oldArtisan.artisanId)
                .add("cgoId", oldArtisan.cgoId)
                .add("bio", oldArtisan.bio)
                .add("city", oldArtisan.city)
                .add("country", oldArtisan.country)
                .add("artisanName", oldArtisan.artisanName)
                .add("contactNumber", oldArtisan.contactNumber)
                .add("lat", 0.0.toString())
                .add("lon", 0.0.toString())
                .add("balance", oldArtisan.balance.toString())

        if (editArtisan_pic.drawable == pic) {
            requestBody.add("picURL", oldArtisan.picURL)
        } else {
            updatePic = true
        }

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(editArtisanURL)
                .post(requestBody.build())
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.i("EditArtisan", body)

                if (updatePic) {
                    updateArtisanPic(oldArtisan)
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("EditArtisan", "failed to do POST request to database" + editArtisanURL)
            }
        })
    }

    private fun validateFields() : Boolean {
        if (TextUtils.isEmpty(editArtisan_name.text.toString())){
            editArtisan_name.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        if (TextUtils.isEmpty(editArtisan_cc.text.toString())) {
            editArtisan_cc.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        if ((!editArtisan_cc.text.toString().contains(","))) {
            editArtisan_cc.error = this.resources.getString(R.string.loc_missing_comma)
            return false
        }

        if (TextUtils.isEmpty(editArtisan_number.text.toString())){
            editArtisan_number.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        if (TextUtils.isEmpty(editArtisanBio.text.toString())){
            editArtisanBio.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        return true
    }

    fun updateArtisanPic(artisan: Artisan) {
        val sourceFile = photoFile!!
        Log.d("EditArtisan", "submitPictureToDB file" + sourceFile + " : " + sourceFile!!.exists())

        val MEDIA_TYPE = MediaType.parse("image/png")

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("artisanId", artisan.artisanId)
                .addFormDataPart("image", "editProfilePic.png", RequestBody.create(MEDIA_TYPE, sourceFile))
                .build()

        val request = Request.Builder()
                .url(updateArtisanURL)
                .post(requestBody)
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.d("EditArtisan", body)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("EditArtisan", "failed to do POST request to database" + updateArtisanURL)
            }
        })
    }
}
