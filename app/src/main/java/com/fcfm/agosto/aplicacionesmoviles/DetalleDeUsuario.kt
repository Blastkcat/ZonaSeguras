package com.fcfm.agosto.aplicacionesmoviles

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.google.android.material.appbar.MaterialToolbar
import coil.load
import coil.transform.CircleCropTransformation
import com.fcfm.agosto.aplicacionesmoviles.MainActivity
import com.fcfm.agosto.aplicacionesmoviles.R
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


public class DetalleDeUsuario : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_de_usuario)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<TextView>(R.id.username).text = auth.currentUser?.email

        findViewById<ImageView>(R.id.profilePicture).load(auth.currentUser?.photoUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        findViewById<Button>(R.id.signOut).setOnClickListener {
            signOut()
        }
        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar);
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this@DetalleDeUsuario, MainActivity::class.java))
            finish()
        }
    }

    fun signOut() {
        lifecycleScope.launch {
            auth.signOut()

            try {
                val credentialManager = CredentialManager.create(this@DetalleDeUsuario)
                val clearRequest = ClearCredentialStateRequest(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                credentialManager.clearCredentialState(clearRequest)
            } catch (e: Exception) {
                Log.e("AUTH", "Error clearing credential state (Check SHA-1/Setup)", e)
            }

            startActivity(Intent(this@DetalleDeUsuario, MainActivity::class.java))

            finish()
        }
    }
}