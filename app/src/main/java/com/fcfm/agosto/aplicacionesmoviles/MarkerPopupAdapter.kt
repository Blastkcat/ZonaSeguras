package com.fcfm.agosto.aplicacionesmoviles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.fcfm.agosto.aplicacionesmoviles.place.Place
import com.fcfm.agosto.aplicacionesmoviles.scores.Score
import com.fcfm.agosto.aplicacionesmoviles.scores.ScoresReader
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.util.Log

class MarkerPopupAdapter(
    private val context: Context
) : GoogleMap.InfoWindowAdapter {
    private val scoresReader = ScoresReader(context)
    private lateinit var auth: FirebaseAuth
    private var rating: Float? = null

    private val db = Firebase.firestore
    private lateinit var ratingText: TextView

    override fun getInfoContents(marker: Marker): View? {
        auth = FirebaseAuth.getInstance()

        val place = marker.tag as? Place ?: return null
        rating = place.rating;

        val view = LayoutInflater.from(context).inflate(
            R.layout.marker_popup, null
        )


        return view
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    fun addScore(place: Place) {
        val currentTime = SimpleDateFormat("yyyy-mm-dd hh:mm:ss", Locale.getDefault())
        currentTime.timeZone = TimeZone.getTimeZone("UTC")
        val currentTimeInUTC = currentTime.format(Date())
        val score = Score(place.id,auth.currentUser?.email!!, rating!!, currentTimeInUTC)
        val newScore = scoresReader.doPost(score)
        place.rating = newScore

        db.collection(context.getString(R.string.placesFirestore)).document(place.id).set(place)
    }

    fun increaseRating(): Float {
        rating?.let {
            return if (it >= 5) {
                5F
            } else {
                rating!! + 0.1F;
            }
        }

        return 0F;
    }

    fun decreaseRating(): Float {
        rating?.let {
            return if (it <= 0) {
                0F
            } else {
                rating!! - 0.1F;
            }
        }

        return 0F;
    }
}