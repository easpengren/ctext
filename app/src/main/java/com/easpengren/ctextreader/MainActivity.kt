package com.easpengren.ctextreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.easpengren.ctextreader.ui.CtextScreen
import com.easpengren.ctextreader.ui.CtextViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CtextViewModel = hiltViewModel()
            CtextScreen(viewModel = viewModel)
        }
    }
}
