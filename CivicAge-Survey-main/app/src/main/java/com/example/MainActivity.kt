package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.database.AppDatabase
import com.example.data.repository.SurveyRepository
import com.example.ui.screens.MainSurveyApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SurveyViewModel
import com.example.ui.viewmodel.SurveyViewModelFactory

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
