package com.iatualcance.alfred.notify

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Tiempo del día con Open-Meteo (gratis, sin clave). La ciudad se escribe en Ajustes y se guarda su posición. */
object Weather {
    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return try { JSONObject(c.inputStream.bufferedReader().readText()) } finally { c.disconnect() }
    }

    /** Frase del tiempo de hoy, o null si no hay ciudad o no hay conexión. */
    fun today(city: String, ctx: Context): String? {
        if (city.isBlank()) return null
        val prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
        var lat = prefs.getString("lat:$city", null); var lon = prefs.getString("lon:$city", null)
        if (lat == null || lon == null) {
            val r = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=es&name=" + URLEncoder.encode(city, "UTF-8")).optJSONArray("results")?.optJSONObject(0) ?: return null
            lat = r.getDouble("latitude").toString(); lon = r.getDouble("longitude").toString()
            prefs.edit().putString("lat:$city", lat).putString("lon:$city", lon).apply()
        }
        val d = get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&timezone=auto&forecast_days=1&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max").getJSONObject("daily")
        return Plan.weatherLine(
            d.getJSONArray("weather_code").getInt(0), d.getJSONArray("precipitation_probability_max").optInt(0, 0),
            Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)).toInt(), Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
        )
    }
}
