package com.fcfm.agosto.aplicacionesmoviles.scores

import android.content.Context
import com.fcfm.agosto.aplicacionesmoviles.R
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class ScoresReader(private val context: Context) {

    private val sheetUrl = context.getString(R.string.scoresSheetUrl)
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun doGet(): Float {
        val request = Request.Builder()
            .url(sheetUrl)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body.toString().toFloat()//json.decodeFromString<List<Score>>(response.body.toString())
        }
    }

    fun doPost(score: Score): Float {
        val jsonString = json.encodeToString<Score>(score)
        val body = jsonString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(sheetUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body.toString().toFloat()
        }
    }
}