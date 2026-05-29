package com.nlud.civilagesurvey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nlud.civilagesurvey.data.database.AppDatabase
import com.nlud.civilagesurvey.data.repository.SurveyRepository
import com.nlud.civilagesurvey.ui.screens.MainSurveyApp
import com.nlud.civilagesurvey.ui.theme.MyApplicationTheme
import com.nlud.civilagesurvey.ui.viewmodel.SurveyViewModel
import com.nlud.civilagesurvey.ui.viewmodel.SurveyViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable premium modern Edge-to-Edge draw limits
        enableEdgeToEdge()

        // Initialize local database components
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SurveyRepository(database.surveyDao())
        
        // Instantiate state manager
        val viewModelFactory = SurveyViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[SurveyViewModel::class.java]

        setContent {
            val themeMode by viewModel.appearanceMode.collectAsState()
            val fontSizeMode by viewModel.fontSizeMode.collectAsState()

            MyApplicationTheme(
                appearanceMode = themeMode,
                fontSizeMode = fontSizeMode
            ) {
                MainSurveyApp(viewModel = viewModel)
            }
        }
    }
}

