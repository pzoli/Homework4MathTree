package com.example.homework4mathtree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.homework4mathtree.ui.MathTreeApp
import com.example.homework4mathtree.ui.theme.Homework4MathTreeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Homework4MathTreeTheme {
                MathTreeApp()
            }
        }
    }
}
