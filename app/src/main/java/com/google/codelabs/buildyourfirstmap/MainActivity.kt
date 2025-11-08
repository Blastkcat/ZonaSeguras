package com.google.codelabs.buildyourfirstmap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fcfm.agosto.aplicacionesmoviles.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        val monterrey = LatLng(25.6866, -100.3161)

        mMap.addMarker(MarkerOptions().position(monterrey).title("Marcador en Monterrey"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(monterrey, 10f))

        val fcfm = LatLng( 25.7234,  -100.3129)
        mMap.addMarker(
            MarkerOptions()
                .position(fcfm)
                .title("FCFM UANL")
        )
    }
}
