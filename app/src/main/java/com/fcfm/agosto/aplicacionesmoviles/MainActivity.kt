package com.fcfm.agosto.aplicacionesmoviles

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.GetCredentialRequest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
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
import android.widget.NumberPicker
import android.widget.TextView
import com.fcfm.agosto.aplicacionesmoviles.scores.Score
import com.fcfm.agosto.aplicacionesmoviles.scores.ScoresReader
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone




class MainActivity : AppCompatActivity() {

    private val places: MutableList<Place> = mutableListOf()
    private lateinit var auth: FirebaseAuth

    private lateinit var credentialManager: CredentialManager
    private var placesReader = PlacesReader(this@MainActivity);

    private var user: FirebaseUser? = null

    private lateinit var map: GoogleMap
    private var geoJsonLayer: GeoJsonLayer? = null
    private val db = Firebase.firestore


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()


        credentialManager = CredentialManager.create(this)

        findViewById<Button>(R.id.signIn).setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, DetalleDeUsuario::class.java))
            } else {
                signIn()
            }
            loadPlacesAndMap()
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
                withContext(Dispatchers.Main) {
                    withContext(Dispatchers.Main) {
                        setupMap()
                    }
                }

            }
            catch (e: Exception){
                Log.e("Firestore", "Error loading places", e)
            }
        }
    }

    // 🔹 Configurar el mapa
    @SuppressLint("PotentialBehaviorOverride")
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

        mapFragment?.getMapAsync { map ->
            map.setOnMapLongClickListener { latLng ->
                val view = LayoutInflater.from(this).inflate(R.layout.new_place_form, null)
                val newTitle = view.findViewById<EditText>(R.id.new_title)
                val newAddress = view.findViewById<EditText>(R.id.new_address)
                val newRatingSelector = view.findViewById<NumberPicker>(R.id.new_rating)
                newRatingSelector.maxValue = 5
                newRatingSelector.minValue = 0

                AlertDialog.Builder(this)
                    .setTitle("New Place")
                    .setView(view)
                    .setPositiveButton("Agregar") { _, _, ->
                        val title = newTitle.text.toString().ifBlank { "Default Title" }
                        val address = newAddress.text.toString().ifBlank { "Default Address" }
                        val rating = newRatingSelector.value.toFloat();

                        placesReader.addPlace(title, latLng, address, rating);

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val placesList = placesReader.read()
                                places.clear()
                                places.addAll(placesList)
                                withContext(Dispatchers.Main) {
                                    addMarkers(map)
                                }
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error loading places", e)
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            }

            map.setOnMarkerClickListener { marker ->
                val place = marker.tag as? Place ?: return@setOnMarkerClickListener false
                val view = LayoutInflater.from(this).inflate(R.layout.marker_popup, null)
                val title = view.findViewById<TextView>(R.id.marker_popup_title)
                title.text = place.name
                val address = view.findViewById<TextView>(R.id.marker_popup_address)
                address.text = place.address
                val newRatingSelector = view.findViewById<NumberPicker>(R.id.marker_popup_rating)
                newRatingSelector.maxValue = 5
                newRatingSelector.minValue = 0
                newRatingSelector.value = place.rating.toInt()

                AlertDialog.Builder(this)
                    .setTitle("Place Information")
                    .setView(view)
                    .setPositiveButton("Editar") { _, _, ->
                        val rating = newRatingSelector.value.toFloat();

                        if (auth.currentUser != null) {
                            val currentTime =
                                SimpleDateFormat("yyyy-mm-dd hh:mm:ss", Locale.getDefault())
                            currentTime.timeZone = TimeZone.getTimeZone("UTC")
                            val currentTimeInUTC = currentTime.format(Date())
                            val score =
                                Score(place.id, auth.currentUser?.email!!, rating, currentTimeInUTC)

                            lifecycleScope.launch(Dispatchers.IO) {
                                val scoresReader = ScoresReader(this@MainActivity)
                                val newScore = scoresReader.doPost(score)
                                withContext(Dispatchers.Main) {
                                    place.rating = newScore
                                }
                            }

                        }
                        newRatingSelector.value = place.rating.toInt()

                        db.collection(getString(R.string.placesFirestore)).document(place.id).set(place)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

                true
            }

            addMarkers(map)

            //map.setInfoWindowAdapter(MarkerPopupAdapter(this))
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
                    .setServerClientId(getString(R.string.default_web_client_id))
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
