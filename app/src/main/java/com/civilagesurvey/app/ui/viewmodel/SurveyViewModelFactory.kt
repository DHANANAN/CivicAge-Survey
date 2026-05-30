package com.civilagesurvey.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.civilagesurvey.app.data.repository.SurveyRepository
import com.civilagesurvey.app.data.repository.GoogleFormsRepository

class SurveyViewModelFactory(
    private val application: Application,
    private val repository: SurveyRepository,
    private val googleFormsRepository: GoogleFormsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SurveyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SurveyViewModel(application, repository, googleFormsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
