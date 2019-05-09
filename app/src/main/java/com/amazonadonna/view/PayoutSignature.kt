package com.amazonadonna.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.gcacace.signaturepad.views.SignaturePad
import android.content.pm.ActivityInfo
import kotlinx.android.synthetic.main.activity_payout_signature.*
import android.util.Log
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import com.amazonadonna.model.App
import com.amazonadonna.model.Artisan
import com.amazonadonna.model.Payout
import com.amazonadonna.sync.PayoutSync
import com.amazonadonna.sync.Synchronizer.Companion.SYNC_NEW
import okhttp3.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class PayoutSignature : AppCompatActivity() {

    private val REQUEST_EXTERNAL_STORAGE = 3
    private val updateURL = App.BACKEND_BASE_URL + "/artisan/edit"
    private val payoutHistory = App.BACKEND_BASE_URL + "/payout/add"
    private val payoutSignatureURL = App.BACKEND_BASE_URL + "/payout/updateImage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payout_signature)
        val artisan = intent.extras?.getSerializable("artisan") as Artisan
        val amount = intent.getDoubleExtra("payoutAmount", 0.0)
        Log.i("PayoutSignature", "Artisan original balance: ${artisan.balance}")
        Log.i("PayoutSignature", "Processing payout amount of : $amount")

        clearSignature_Button.isEnabled = false
        doneSignature_button.isEnabled = false
        //change screen orientation to landscape mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        signature_pad.setOnSignedListener(object : SignaturePad.OnSignedListener {

            override fun onStartSigning() {
                //Event triggered when the pad is touched
            }

            override fun onSigned() {
                //Event triggered when the pad is signed
                clearSignature_Button.isEnabled = true
                doneSignature_button.isEnabled = true
            }

            override fun onClear() {
                //Event triggered when the pad is cleared
                clearSignature_Button.isEnabled = false
                doneSignature_button.isEnabled = false
            }
        })

        clearSignature_Button.setOnClickListener {
            clearSignature()
        }

        doneSignature_button.setOnClickListener {
            saveSignature(artisan, amount)
        }
    }

    private fun clearSignature() {
        signature_pad.clear()
    }

    private fun saveSignature(artisan: Artisan, amount : Double) {
        val signatureFilePath = saveSignatureToCache()
        var payout = Payout("", amount, System.currentTimeMillis(), artisan.artisanId, SYNC_NEW, "Not set", artisan.cgaId)
        PayoutSync.addPayout(applicationContext, payout, artisan, File(signatureFilePath))
        runOnUiThread{
            showResponseDialog(artisan, true, amount)
        }
        //updateArtisanBalance(artisan, amount, signatureFilePath)
        //showResponseDialog(artisan, true)
    }

    private fun saveSignatureToCache() : String {
        //file/storage/emulated/0/DCIM/Camera/IMG_20190206_201443.jpg
        val fileName = getCurrentDate()
        val file = File(externalCacheDir, fileName)
        file.createNewFile()
        //val fileProvider = FileProvider.getUriForFile(this@PayoutSignature, "com.amazonadonna.amazonhandmade.fileprovider", file)
        //Log.d("PayoutSignature", "file created: " + file.absolutePath + " : " + file.exists())

        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            signature_pad.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            Log.d("PayoutSignature", "file created: " + fileName + " : " + file.exists())
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fileOutputStream?.close()
        }

        return file.path
    }

    private fun getCurrentDate() : String {
        val dateFormat = SimpleDateFormat("yyy_mm_dd_HHmmss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun updateArtisanBalance(artisan: Artisan, amount: Double, signatureFilePath : String) {
        Log.i("PayoutSignature", "updating Artisan with new balance of: " + (artisan.balance - amount).toString())
        val requestBody = FormBody.Builder().add("artisanId", artisan.artisanId)
                .add("balance", (artisan.balance - amount).toString())

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(updateURL)
                .post(requestBody.build())
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.i("PayoutSignature", "artisan update $body")
                //balance updated now send signature
                submitPayoutToDB(artisan, amount, signatureFilePath)
                //submitSignatureToDB(artisan, signatureFilePath)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("PayoutSignature", "failed to do POST request to database $updateURL")
            }
        })

        //showResponseDialog(artisan, true)
    }

    private fun submitPayoutToDB(artisan: Artisan, amount: Double, signatureFilePath: String) {
        val requestBody = FormBody.Builder().add("artisanId", artisan.artisanId)
                .add("cgaId", artisan.cgaId)
                .add("amount", amount.toString())
                .add("date", System.currentTimeMillis().toString())

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(payoutHistory)
                .post(requestBody.build())
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()!!.string()
                Log.i("PayoutSignature", "payoutid $body")
                submitSignatureToDB(artisan, body, signatureFilePath, amount)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("PayoutSignature", "failed to do POST request to database $payoutHistory")
            }
        })

        //showResponseDialog(artisan, true)
    }
    private fun showResponseDialog(artisan: Artisan, status: Boolean, amount: Double) {
        val builder = AlertDialog.Builder(this@PayoutSignature)
        if (status) {
            builder.setTitle("Payout Approved!")
            builder.setMessage("Current Artisan Balance: $ ${artisan.balance}")
            builder.setOnDismissListener {
                submitDismiss(artisan)
//                val intent = Intent(this, ArtisanProfileCGA::class.java)
//                intent.putExtra("artisan", artisan)
//                //finishAffinity()
//                startActivity(intent)
//               // finishAffinity()
//               finish()
            }
        } else {
            builder.setTitle("Payout Failed!")
            builder.setMessage("Current Artisan Balance: $ ${artisan.balance}")
            builder.setOnDismissListener {
                //do nothing
            }
        }

        val dialog : AlertDialog = builder.create()
        dialog.show()
    }

    private fun submitSignatureToDB(artisan: Artisan, payoutId : String , signatureFilePath: String, amount: Double) {
        val signatureFile = File(signatureFilePath)
        Log.d("PayoutSignature", "submitSignatureToDB file " + signatureFile + " : " + signatureFile.exists())

        val MEDIA_TYPE = MediaType.parse("image/png")

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payoutId", payoutId)
                .addFormDataPart("image", "payout.png", RequestBody.create(MEDIA_TYPE, signatureFile))
                .build()

        val request = Request.Builder()
                .url(payoutSignatureURL)
                .post(requestBody)
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                Log.d("PayoutSignature", "signature pic success $body")
                runOnUiThread{
                    showResponseDialog(artisan, true, amount)
                }
                signatureFile.delete()
                Log.d("PayoutSignature", "signature file clean up " + signatureFile + " : " + signatureFile.exists())

            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("PayoutSignature", "failed to do POST request to database $payoutSignatureURL")
                runOnUiThread{
                    showResponseDialog(artisan, false, amount)
                }
                signatureFile.delete()
                Log.d("PayoutSignature", "signature file clean up " + signatureFile + " : " + signatureFile.exists())
            }
        })
    }

    private fun submitDismiss(artisan: Artisan) {
        val intent = Intent(this, ArtisanProfileCGA::class.java)
        intent.putExtra("artisan", artisan)
        //finishAffinity()
        startActivity(intent)
        finish()
    }
}
