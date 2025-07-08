package com.rjnr.thaiwrter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rjnr.thaiwrter.ui.navigation.NavDestinations
import com.rjnr.thaiwrter.ui.screens.CharacterPracticeScreen
import com.rjnr.thaiwrter.ui.screens.MainScreen
import com.rjnr.thaiwrter.ui.screens.ProgressScreen
import com.rjnr.thaiwrter.ui.theme.ThaiWrterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThaiWrterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavDestinations.HOME
                    ) {
                        composable(NavDestinations.HOME) {
                            MainScreen(navController = navController)
                        }
                        composable(NavDestinations.CHARACTER_PRACTICE) {
                            CharacterPracticeScreen(navController = navController)
                        }
                        composable(NavDestinations.PROGRESS) {
                            ProgressScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ThaiWrterTheme {
        Greeting("Android")
    }
}