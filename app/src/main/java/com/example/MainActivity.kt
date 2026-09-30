package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SmaliMergerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SmaliMergerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SmaliMergerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(fontTheme = uiState.fontTheme) {
                SmaliMergerApp(viewModel = viewModel)
            }
        }
    }
}
