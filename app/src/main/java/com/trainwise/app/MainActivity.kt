package com.trainwise.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.trainwise.app.ui.TrainwiseApp
import com.trainwise.app.ui.TrainwiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrainwiseTheme { TrainwiseApp() } }
    }
}
