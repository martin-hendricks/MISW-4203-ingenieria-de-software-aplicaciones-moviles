package com.miso.vinilos.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miso.vinilos.model.data.UserRole
import com.miso.vinilos.model.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el perfil del usuario
 * Maneja la selección y persistencia del rol de usuario
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = UserPreferencesRepository.getInstance(application)
    
    /**
     * Estado actual del rol del usuario
     */
    val userRole: StateFlow<UserRole> = repository.userRoleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserRole.VISITOR
        )
    
    /**
     * Actualiza el rol del usuario
     */
    fun selectRole(role: UserRole) {
        viewModelScope.launch {
            repository.saveUserRole(role)
        }
    }
}

