package com.civilagesurvey.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.civilagesurvey.app.data.database.AppDatabase
import com.civilagesurvey.app.data.repository.SurveyRepository
import com.civilagesurvey.app.data.repository.GoogleFormsRepository
import com.civilagesurvey.app.data.api.GoogleFormsApiService
import com.civilagesurvey.app.ui.screens.MainSurveyApp
import com.civilagesurvey.app.ui.theme.DCATheme
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModelFactory
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            FirebaseApp.initializeApp(this)
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
        } catch (e: Throwable) {
            Log.e(TAG, "Critical failure during startup", e)
            try {
                setContent {
                    DCATheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "DCA App Crash Guard",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "A critical error occurred during startup:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = Log.getStackTraceString(e),
                                        modifier = Modifier.padding(16.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Toast.makeText(this, "Fatal Startup Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
