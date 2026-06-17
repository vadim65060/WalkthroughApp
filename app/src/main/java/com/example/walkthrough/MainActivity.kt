package com.example.walkthrough

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.walkthrough.di.RepositoryHolder
import com.example.walkthrough.presentation.navigation.NavGraph
import com.example.walkthrough.ui.theme.WalkthroughTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Инициализация репозиториев
            RepositoryHolder.init(this)
            Log.d("MainActivity", "RepositoryHolder initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing RepositoryHolder", e)
        }

        try {
            setContent {
                WalkthroughTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph()
                    }
                }
            }
            Log.d("MainActivity", "Content set successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting content", e)
        }
    }
}