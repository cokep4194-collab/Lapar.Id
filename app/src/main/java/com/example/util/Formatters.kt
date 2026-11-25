package com.example.util

import com.example.R
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale = Locale("id", "ID")

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(idLocale)
    return "Rp ${formatter.format(amount)}"
}

fun formatTimestamp(ts: Timestamp?): String {
    val date = ts?.toDate() ?: return "Baru saja"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
    return sdf.format(date)
}

fun todayDateIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun drawableForKey(key: String): Int {
    return when (key) {
        "img_food_nasi" -> R.drawable.img_food_nasi
        "img_food_mie" -> R.drawable.img_food_mie
        "img_food_minuman" -> R.drawable.img_food_minuman
        "img_hero_campus" -> R.drawable.img_hero_campus
        else -> R.drawable.img_food_nasi
    }
}
