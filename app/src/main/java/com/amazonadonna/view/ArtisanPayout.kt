package com.amazonadonna.view

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.amazonadonna.model.Artisan
import kotlinx.android.synthetic.main.activity_artisan_payout.*
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.support.v7.app.AlertDialog
import android.util.Log


class ArtisanPayout : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artisan_payout)

        val artisan = intent.extras?.getSerializable("artisan") as Artisan

        artisanPayout_amount.setText(artisan.balance.toString())
        artisanPayout_dateTV.text = "Date: " + getCurrDate()
        artisanPayout_continue.setOnClickListener {
            continueToSignature(artisan)
        }

        artisanPayout_datePicker.setOnClickListener {
            pickDate()
        }
    }

    private fun pickDate() {
        val date: Calendar = Calendar.getInstance()
        var initYear = date.get(Calendar.YEAR)
        var initMonth = date.get(Calendar.MONTH)
        var initDay = date.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            initMonth = monthOfYear + 1
            initDay = dayOfMonth
            initYear = year

            Log.i("ArtisanPayout", "new picked Date: " + initMonth + "/" + initDay + "/" + initYear)
            artisanPayout_dateTV.setText("Date: " + initMonth + "/" + initDay + "/" + initYear)
            //lblDate.setText("" + dayOfMonth + " " + MONTHS[monthOfYear] + ", " + year)
        }, initYear, initMonth, initDay)
        dpd.show()
    }

    private fun getCurrDate() : String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun continueToSignature(artisan: Artisan) {
        if (validateAmount(artisan)) {
            Log.w("ArtisanPayout", "Payout amount exceeded balance")
            val builder = AlertDialog.Builder(this@ArtisanPayout)
            builder.setTitle("Payout Error")
            builder.setMessage("Payout amount exceeds Artisan's Payout balance!")
            builder.setOnDismissListener {
                //Do nothing
            }
            val dialog : AlertDialog = builder.create()
            dialog.show()
        }
        val intent = Intent(this, PayoutSignature::class.java)
        intent.putExtra("artisan", artisan)
        intent.putExtra("payoutAmount", artisanPayout_amount.text.toString().toDouble())
        startActivity(intent)
    }

    private fun validateAmount(artisan : Artisan) : Boolean {
        return (artisanPayout_amount.text.toString().toDouble() > artisan.balance)
    }
}
