package com.fcfm.agosto.aplicacionesmoviles

import androidx.credentials.exceptions.ClearCredentialException
import android.content.Intent
import android.credentials.ClearCredentialStateRequest
import android.credentials.CredentialManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DetalleDeUsuario : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_de_usuario)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<TextView>(R.id.username).text = auth.currentUser?.email
    }
/*
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun signOut() {

        lifecycleScope.launch() {
            auth.signOut()
            val credentialManager =
                androidx.credentials.CredentialManager.create(this@DetalleDeUsuario);
            var clearRequest = ClearCredentialStateRequest(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            credentialManager.clearCredentialState()
            startActivity(Intent(this@DetalleDeUsuario, MainActivity::class.java))
        }
    }*/
}