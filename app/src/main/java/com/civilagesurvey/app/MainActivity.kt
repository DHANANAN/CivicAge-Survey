package com.civilagesurvey.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.civilagesurvey.app.data.database.AppDatabase
import com.civilagesurvey.app.data.repository.SurveyRepository
import com.civilagesurvey.app.data.repository.GoogleFormsRepository
import com.civilagesurvey.app.data.api.GoogleFormsApiService
import com.civilagesurvey.app.ui.screens.MainSurveyApp
import com.civilagesurvey.app.ui.theme.DCATheme
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val surveyDao = database.surveyDao()
        val repository = SurveyRepository(surveyDao)
        val googleFormsApiService = GoogleFormsApiService.create()
        val googleFormsRepository = GoogleFormsRepository(surveyDao, googleFormsApiService)
        
        val viewModelFactory = SurveyViewModelFactory(application, repository, googleFormsRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[SurveyViewModel::class.java]

        setContent {
            val appearanceMode by viewModel.appearanceMode.collectAsState()
            val fontSizeMode by viewModel.fontSizeMode.collectAsState()

            DCATheme(
                appearanceMode = appearanceMode,
                fontSizeMode = fontSizeMode
            ) {
                MainSurveyApp(viewModel = viewModel)
            }
        }
    }
}
