package com.smter.adv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smter.adv.ui.DemoScreen
import com.smter.adv.ui.LaunchState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Debug-only: a launcher extra may pin panel height / variant for screenshots.
        val launch = LaunchState.bind(this)
        setContent {
            DemoScreen(launch = launch)
        }
    }
}
