package com.example.data.repository

import android.content.Context
import com.example.config.Config
import com.example.data.database.UserProfileEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val context: Context
) {
    val currentUid: String?
        get() = authOrNull()?.currentUser?.uid

    fun currentEmail(): String? = authOrNull()?.currentUser?.email

    fun isConfigured(): Boolean = FirebaseRuntime.isConfigured(context)

    fun statusMessage(): String = FirebaseRuntime.statusMessage(context)

    suspend fun signInOrCreate(
        email: String,
        password: String,
        fullName: String,
        surveyorId: String,
        mobileNumber: String,
        createAccount: Boolean,
        appearanceSettings: Map<String, Any?>
    ): Result<UserProfileEntity> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException(statusMessage()))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        return try {
            val auth = authOrNull()
                ?: return Result.failure(IllegalStateException("Firebase is not initialized. Add app/google-services.json."))
            val firestore = firestoreOrNull()
                ?: return Result.failure(IllegalStateException("Firestore is not initialized. Add app/google-services.json."))

            if (createAccount) {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
            } else {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
            }

            val firebaseUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("Firebase did not return a signed-in user."))

            val normalizedEmail = (firebaseUser.email ?: email).trim().lowercase()
            val now = System.currentTimeMillis()
            val role = Config.roleForEmail(normalizedEmail)
            val profile = UserProfileEntity(
                uid = firebaseUser.uid,
                fullName = fullName.trim(),
                email = normalizedEmail,
                surveyorId = surveyorId.trim(),
                mobileNumber = mobileNumber.trim(),
                role = role,
                active = true,
                createdAt = now,
                lastLoginAt = now,
                appearanceSettingsJson = appearanceSettings.toString()
            )

            val firestoreProfile = mapOf(
                "uid" to profile.uid,
                "fullName" to profile.fullName,
                "email" to profile.email,
                "surveyorId" to profile.surveyorId,
                "mobileNumber" to profile.mobileNumber,
                "role" to profile.role,
                "active" to profile.active,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "appearanceSettings" to appearanceSettings
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(firestoreProfile, SetOptions.merge())
                .await()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        authOrNull()?.signOut()
    }

    private fun authOrNull(): FirebaseAuth? {
        return if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        return if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }
}
