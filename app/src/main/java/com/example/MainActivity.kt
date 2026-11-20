package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.QuoteVoiceApp
import com.example.ui.QuoteVoiceViewModel
import com.example.ui.theme.QuoteVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuoteVoiceTheme {
                val vm: QuoteVoiceViewModel = viewModel(
                    factory = QuoteVoiceViewModel.Factory(applicationContext)
                )
                QuoteVoiceApp(viewModel = vm)
            }
        }
    }
}
