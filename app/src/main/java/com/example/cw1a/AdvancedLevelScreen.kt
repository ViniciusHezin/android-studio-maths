package com.example.cw1a

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first

/**
 * Renders the UI for the Advanced Level game mode.
 * It connects to the AdvancedGameViewModel to display the 20x20 puzzle grid,
 * handles the number input dialogs, tracks the session ID to survive screen rotations,
 * and automatically saves the player's highest score using DataStore.
 */
@Composable
fun AdvancedLevelScreen(
    gameId: Int, // THE FIX: Catch the Session ID
    onBack: () -> Unit,
    viewModel: AdvancedGameViewModel = viewModel()
) {
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    var highScore by remember { mutableIntStateOf(0) }

    val showInputDialog = remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableIntStateOf(-1) }
    val currentInputText = remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val availableWidth = configuration.screenWidthDp.dp - 16.dp
    val cellSize = availableWidth / viewModel.cols
    val gridHeight = cellSize * viewModel.rows


    LaunchedEffect(gameId) {
        highScore = scoreManager.advancedHighScoreFlow.first()
        viewModel.startNewGame(gameId)
    }

    LaunchedEffect(viewModel.score.intValue) {
        if (viewModel.score.intValue > highScore) {
            scoreManager.saveAdvancedHighScore(viewModel.score.intValue)
            highScore = viewModel.score.intValue
        }
    }

    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFEBEB)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                Text("ADVANCED MODE", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text("Starting in ${viewModel.loadingCountdown.intValue}...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Flee", fontSize = 10.sp) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME LEFT: ${viewModel.timeLeft.intValue}", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Best: $highScore", fontSize = 10.sp, color = Color.LightGray)
                }

                Text("Score: ${viewModel.score.intValue}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(viewModel.cols),
                    modifier = Modifier.width(availableWidth).height(gridHeight).border(2.dp, Color.Red).background(Color.Black),
                    userScrollEnabled = false
                ) {
                    items(viewModel.puzzleData.size) { index ->
                        GameCell(cell = viewModel.puzzleData[index]) {
                            if (!viewModel.isGameOver.value) {
                                selectedIndex.intValue = index
                                currentInputText.value = viewModel.puzzleData[index].userInput
                                showInputDialog.value = true
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (viewModel.isGameOver.value) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("GAME OVER!", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("Final score: ${viewModel.score.intValue}\nYour High Score: $highScore") },
            confirmButton = { Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Back to Safety") } }
        )
    }

    if (showInputDialog.value) {
        AlertDialog(
            onDismissRequest = { showInputDialog.value = false },
            title = { Text("Enter Number") },
            text = {
                TextField(
                    value = currentInputText.value,
                    onValueChange = { if (it.all { char -> char.isDigit() }) currentInputText.value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedIndex.intValue in viewModel.puzzleData.indices) {
                        viewModel.onCellInput(selectedIndex.intValue, currentInputText.value)
                    }
                    showInputDialog.value = false
                }) { Text("OK") }
            }
        )
    }
}