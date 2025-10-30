package com.miso.vinilos.model.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miso.vinilos.model.data.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio para gestionar las preferencias del usuario
 * Usa DataStore para persistencia de datos
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
    
    companion object {
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        
        @Volatile
        private var instance: UserPreferencesRepository? = null
        
        fun getInstance(context: Context): UserPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: UserPreferencesRepository(context).also { instance = it }
            }
        }
    }
    
    /**
     * Flow que emite el rol actual del usuario
     */
    val userRoleFlow: Flow<UserRole> = context.dataStore.data.map { preferences ->
        val roleName = preferences[USER_ROLE_KEY] ?: UserRole.VISITOR.name
        try {
            UserRole.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            UserRole.VISITOR
        }
    }
    
    /**
     * Guarda el rol seleccionado por el usuario
     */
    suspend fun saveUserRole(role: UserRole) {
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE_KEY] = role.name
        }
    }
}

