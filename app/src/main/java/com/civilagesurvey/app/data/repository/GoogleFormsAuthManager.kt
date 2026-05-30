package com.civilagesurvey.app.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleFormsAuthManager(private val context: Context) {
    private val TAG = "GoogleFormsAuthManager"
    private val auth = FirebaseAuth.getInstance()

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val accessToken: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val formsScopes = listOf(
        Scope("https://www.googleapis.com/auth/forms.body.readonly"),
        Scope("https://www.googleapis.com/auth/forms.responses.readonly")
    )

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        
        formsScopes.forEach { builder.requestScopes(it) }
        
        return builder.build()
    }

    fun checkPermissions(): Boolean {
        // This requires the play-services-auth dependency to be correctly synced.
        // If it's still showing unresolved, it's likely a sync issue.
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null && GoogleSignIn.hasPermissions(account, *formsScopes.toTypedArray())
        } catch (e: Exception) {
            false
        }
    }

    fun handleSignInResult(accessToken: String?) {
        if (accessToken != null) {
            _authState.value = AuthState.Authenticated(accessToken)
        } else {
            _authState.value = AuthState.Error("Failed to obtain Access Token")
        }
    }
}
