package com.fcfm.agosto.aplicacionesmoviles

import android.content.Intent
import android.credentials.CredentialManager
import android.credentials.CredentialOption

import android.credentials.GetCredentialRequest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fcfm.agosto.aplicacionesmoviles.place.Place
import com.fcfm.agosto.aplicacionesmoviles.place.PlacesReader

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPolygon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext





class MainActivity : AppCompatActivity() {

    private val places: MutableList<Place> = mutableListOf()
    private lateinit var auth: FirebaseAuth

    private lateinit var credentialManager: CredentialManager
    private var placesReader = PlacesReader(this@MainActivity);

    private var user: FirebaseUser? = null

    private lateinit var map: GoogleMap
    private var geoJsonLayer: GeoJsonLayer? = null


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()


        credentialManager= CredentialManager.create(this);

        findViewById<Button>(R.id.signIn).setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, DetalleDeUsuario::class.java))
            } else {
                signIn()
            }
        }


        val buscador = findViewById<EditText>(R.id.editTextText)

        buscador.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {

                val textoBuscado = buscador.text.toString().trim()
                if (textoBuscado.isNotEmpty()) {
                    buscarColonia(textoBuscado)
                }
                true
            } else {
                false
            }
        }


    }


    // 🔹 Cargar los lugares y el mapa
    private fun loadPlacesAndMap(){
        lifecycleScope.launch(Dispatchers.IO){
            try{
                val placesList = placesReader.read()
                places.clear()
                places.addAll(placesList)
                setupMap()

            }
            catch (e: Exception){
                Log.e("Firestore", "Error loading places", e)
            }
        }
    }

    // 🔹 Configurar el mapa
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            addMarkers(map)
            map.setInfoWindowAdapter(MarkerPopupAdapter(this))

            loadGeoJson(map)


            setupSearch()
        }

    }

    // 🔹 Añadir marcadores
    private fun addMarkers(map: GoogleMap) {
        map.clear()
        places.forEach { place ->
            val marker = map.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(place.latLng)
            )
            marker?.tag = place
        }
    }

    // 🔹 Cargar y mostrar el GeoJSON
    private fun loadGeoJson(map: GoogleMap) {
        try {
            assets.open("NuevoLeon.geojson").use { inputStream ->
                val jsonStringBuilder = StringBuilder()
                val buffer = ByteArray(8192) // 8 KB buffer
                var bytesRead: Int

                // Leer en fragmentos (sin cargar todo a la vez)
                val reader = inputStream.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    jsonStringBuilder.append(line)
                }

                val json = JSONObject(jsonStringBuilder.toString())

                val layer = GeoJsonLayer(map, json)

                // 🔹 Estilos
                val polygonStyle = layer.defaultPolygonStyle
                polygonStyle.fillColor = Color.argb(70, 0, 150, 255)
                polygonStyle.strokeColor = Color.BLUE
                polygonStyle.strokeWidth = 2f

                layer.addLayerToMap()

                Log.i("MAPA", "GeoJSON cargado correctamente")
            }
        } catch (e: OutOfMemoryError) {
            Log.e("MAPA", "El archivo GeoJSON es demasiado grande. Filtra o reduce su tamaño.", e)
        } catch (e: Exception) {
            Log.e("MAPA", "Error al cargar GeoJSON", e)
        }
    }



    private fun buscarColonia(nombre: String) {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        mapFragment?.getMapAsync { map ->
            try {
                val inputStream = assets.open("NuevoLeon.geojson")
                val json = inputStream.bufferedReader().use { it.readText() }
                val layer = GeoJsonLayer(map, JSONObject(json))


                map.clear()
                var coloniaEncontrada = false
                var boundsBuilder = LatLngBounds.Builder()

                for (feature in layer.features) {
                    val nombreColonia = feature.getProperty("COLONIA") ?: ""
                    // Comparar ignorando acentos y mayúsculas
                    if (normalizar(nombreColonia) == normalizar(nombre)) {
                        coloniaEncontrada = true

                        // Dibujar el polígono manualmente
                        val geometry = feature.geometry
                        val polygonOptions = PolygonOptions()
                            .fillColor(Color.argb(80, 0, 150, 255))
                            .strokeColor(Color.BLUE)
                            .strokeWidth(3f)

                        when (geometry) {
                            is com.google.maps.android.data.geojson.GeoJsonPolygon -> {
                                for (ring in geometry.coordinates) {
                                    for (coord in ring) {
                                        polygonOptions.add(coord)
                                        boundsBuilder.include(coord)
                                    }
                                }
                            }
                            is com.google.maps.android.data.geojson.GeoJsonMultiPolygon -> {
                                for (polygon in geometry.polygons) {
                                    for (ring in polygon.coordinates) {
                                        for (coord in ring) {
                                            polygonOptions.add(coord)
                                            boundsBuilder.include(coord)
                                        }
                                    }
                                }
                            }
                        }

                        map.addPolygon(polygonOptions)
                        break


                    }
                }

                if (coloniaEncontrada) {
                    val bounds = boundsBuilder.build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } else {
                    Toast.makeText(this, "Colonia no encontrada", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("MAPA", "Error buscando colonia", e)
            }
        }
    }

    // 🔹 Quita acentos y pasa a minúsculas
    private fun normalizar(texto: String): String {
        val normalized = java.text.Normalizer.normalize(texto.lowercase(), java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalized, "")
    }



    private fun setupSearch() {
        val searchField = findViewById<android.widget.EditText>(R.id.editTextText)

        searchField.setOnEditorActionListener { v, actionId, event ->
            val coloniaBuscada = v.text.toString().trim()
            if (coloniaBuscada.isNotEmpty()) {
                mostrarColonia(coloniaBuscada)
            }
            true
        }
    }

    private fun mostrarColonia(nombre: String) {
        try {

            map.clear()
            geoJsonLayer?.removeLayerFromMap()

            val inputStream: InputStream = assets.open("NuevoLeon.geojson")
            val json = inputStream.bufferedReader().use { it.readText() }
            val fullLayer = GeoJsonLayer(map, JSONObject(json))

            var encontrada = false
            val boundsBuilder = LatLngBounds.Builder()


            for (feature in fullLayer.features) {
                val nombreColonia = feature.getProperty("COLONIA") ?: continue

                if (nombreColonia.equals(nombre, ignoreCase = true)) {
                    encontrada = true

                    // Resaltar la feature encontrada
                    val polyStyle = feature.polygonStyle
                    polyStyle.fillColor = Color.argb(140, 76, 175, 80) // verde translúcido
                    polyStyle.strokeColor = Color.GREEN
                    polyStyle.strokeWidth = 3f

                    // Calcular bounds según la geometría
                    val geometry = feature.geometry
                    if (geometry is com.google.maps.android.data.geojson.GeoJsonPolygon) {
                        // 🔹 Polígono normal
                        for (ring in geometry.coordinates) {
                            for (coord in ring) {
                                boundsBuilder.include(coord)
                            }
                        }
                    } else if (geometry is com.google.maps.android.data.geojson.GeoJsonMultiPolygon) {
                        // 🔹 MultiPolígono (varios polígonos dentro)
                        for (poly in geometry.polygons) {
                            for (ring in poly.coordinates) {
                                for (coord in ring) {
                                    boundsBuilder.include(coord)
                                }
                            }
                        }
                    } else if (geometry is com.google.maps.android.data.geojson.GeoJsonPoint) {

                        boundsBuilder.include(geometry.coordinates)
                    }

                } else {

                    val otherStyle = feature.polygonStyle
                    otherStyle.fillColor = Color.TRANSPARENT
                    otherStyle.strokeColor = Color.TRANSPARENT
                    otherStyle.strokeWidth = 0f
                }
            }

            fullLayer.addLayerToMap()
            geoJsonLayer = fullLayer // guardar referencia

            if (encontrada) {

                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } else {
                Log.w("BUSCADOR", "No se encontró la colonia '$nombre'")
            }
        } catch (e: Exception) {
            Log.e("BUSCADOR", "Error mostrando colonia", e)
        }
    }






    private fun signInWithGoogle(tokenId: String) {
        val credential = GoogleAuthProvider.getCredential(tokenId, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    user = auth.currentUser
                    Log.i("AUTH", "Google Sign-In successful: ${user?.email}")
                    startActivity(Intent(this, DetalleDeUsuario::class.java))
                } else {
                    Log.w("AUTH", "Google Sign-In failed", task.exception)
                }
            }
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun signIn() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(R.string.default_web_client_id.toString())
                    .build();

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

                val result = credentialManager.getCredential(this@MainActivity, request);

                val credential = result.credential;
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data);
                signInWithGoogle(googleIdToken.idToken);
            } catch (e: GetCredentialException) {
                Log.w("AUTH", "Credential error", e)
            }
        }
    }





}
