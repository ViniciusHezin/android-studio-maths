package com.example.cw1a

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cw1a.ui.theme.Cw1aTheme

/**
 * The main entry point of the application.
 * It sets up the Jetpack Compose UI and ensures the current screen state
 * is saved and restored if the Android system temporarily kills the process.
 */

class MainActivity : ComponentActivity() {
    private var savedScreen: String by mutableStateOf("MENU")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            savedScreen = savedInstanceState.getString("current_screen", "MENU") ?: "MENU"
        }
        setContent {
            Cw1aTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Cw1App(
                        initialScreen = savedScreen,
                        onScreenChange = { newScreen: String -> savedScreen = newScreen }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_screen", savedScreen)
    }
}

/**
 * The root composable that handles navigation between the Menu, Game, and Advanced screens.
 * It generates and tracks the 'gameSessionId' to ensure that device orientation changes
 * do not accidentally reset the active puzzle boards.
 */
@Composable
fun Cw1App(initialScreen: String, onScreenChange: (String) -> Unit) {
    var currentScreen: String by rememberSaveable { mutableStateOf(initialScreen) }
    var chosenEquationCount by rememberSaveable { mutableIntStateOf(100) }

    // THE FIX: Tracks the specific game session so rotation doesn't reset it
    var gameSessionId by rememberSaveable { mutableIntStateOf(0) }

    when (currentScreen) {
        "MENU" -> MainMenu(
            onNewGameClick = { equationCount ->
                chosenEquationCount = equationCount
                gameSessionId++ // Generates a new game ID
                currentScreen = "GAME"
                onScreenChange("GAME")
            },
            onAdvancedLevelClick = {
                gameSessionId++ // Generates a new game ID
                currentScreen = "ADVANCED"
                onScreenChange("ADVANCED")
            }
        )
        "GAME" -> GameScreen(
            equationCount = chosenEquationCount,
            gameId = gameSessionId, // Pass the ID to the screen
            onBack = {
                currentScreen = "MENU"
                onScreenChange("MENU")
            }
        )
        "ADVANCED" -> AdvancedLevelScreen(
            gameId = gameSessionId, // Pass the ID to the screen
            onBack = {
                currentScreen = "MENU"
                onScreenChange("MENU")
            }
        )
    }
}

@Composable
fun MainMenu(
    onNewGameClick: (Int) -> Unit,
    onAdvancedLevelClick: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEquationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { showEquationDialog = true }) { Text("New Game") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAdvancedLevelClick) { Text("Advanced Level") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAboutDialog = true }) { Text("About") }
    }

    if (showEquationDialog) {
        EquationCountDialog(
            onDismiss = { showEquationDialog = false },
            onStart = { count ->
                showEquationDialog = false
                onNewGameClick(count)
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun EquationCountDialog(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(150f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Settings") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Equation Density:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("${sliderValue.toInt()}", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 50f..500f,
                    steps = 45 // Provides increments of 10
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(sliderValue.toInt()) }) {
                Text("Start Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "About Author") },
        text = {
            Column {
                Text(text = "Name: Vinicius Carvalho Hezin", fontWeight = FontWeight.Bold)
                Text(text = "Student ID: w2078163")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "I confirm that I understand what plagiarism is and have read and " +
                            "understood the section on Assessment Offences in the Essential " +
                            "Information for Students. The work that I have submitted is " +
                            "entirely my own. Any work from other authors is duly referenced " +
                            "and acknowledged."
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}