package com.jarvis.assistant.security

import android.app.Activity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminGuard {
    private const val PRIMARY_ADMIN_EMAIL = "mdaslamlaskar14@gmail.com"

    /**
     * Checks whether the currently authenticated user is an authorized administrator
     * by verifying their email against the secure Firestore '/admins/{email}' whitelist
     * and checking the primary admin override.
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val email = user.email?.lowercase()?.trim() ?: return false

        // Primary administrator override
        if (email == PRIMARY_ADMIN_EMAIL) {
            return true
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("admins").document(email).get().await()
            doc.exists() && (doc.getBoolean("active") ?: true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Guards an administrative navigation route or action.
     * If the user is authorized, executes [onAuthorized].
     * Otherwise, shows a security denial toast and finishes the activity.
     */
    suspend fun enforceAccess(activity: Activity, onAuthorized: () -> Unit) {
        val authorized = isCurrentUserAdmin()
        if (authorized) {
            onAuthorized()
        } else {
            Toast.makeText(
                activity,
                "Security Alert: Unauthorized access attempt blocked.",
                Toast.LENGTH_LONG
            ).show()
            activity.finish()
        }
    }
}
