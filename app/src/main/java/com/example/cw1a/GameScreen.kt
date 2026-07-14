package com.example.cw1a

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
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
 * Renders the primary UI for the standard cross-math game mode.
 * It connects to the GameViewModel to display the dynamic puzzle grid,
 * passes the user-selected equation density and session ID down to the logic,
 * handles number input dialogs, and manages DataStore to persistently save high scores.
 */
@Composable
fun GameScreen(
    equationCount: Int,
    gameId: Int, // The Session ID to prevent rotation reset
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel()
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

    // Re-runs only when the gameId changes (i.e. clicking "New Game", NOT rotating)
    LaunchedEffect(gameId) {
        highScore = scoreManager.highScoreFlow.first()
        viewModel.startNewGame(equationCount, gameId)
    }

    // Listens to the ViewModel's score and updates DataStore if you beat it
    LaunchedEffect(viewModel.score.intValue) {
        if (viewModel.score.intValue > highScore) {
            scoreManager.saveHighScore(viewModel.score.intValue)
            highScore = viewModel.score.intValue
        }
    }

    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.Blue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Starting in ${viewModel.loadingCountdown.intValue}...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF0F0F0)).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) { Text("Back", fontSize = 10.sp) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Timer ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = viewModel.timerEnabled.value,
                            onCheckedChange = { viewModel.timerEnabled.value = it },
                            modifier = Modifier.scale(0.7f)
                        )
                        if (viewModel.timerEnabled.value) {
                            Text(" : ${viewModel.timeLeft.intValue}", color = if (viewModel.timeLeft.intValue < 10) Color.Red else Color.Black, fontSize = 12.sp)
                        }
                    }
                    Text("Best: $highScore", fontSize = 10.sp, color = Color.Gray)
                }

                Text("Score: ${viewModel.score.intValue}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(viewModel.cols),
                    modifier = Modifier.width(availableWidth).height(gridHeight).border(2.dp, Color.Black).background(Color.Black),
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
            title = { Text("GAME OVER!") },
            text = { Text("Final score: ${viewModel.score.intValue}\nYour High Score: $highScore") },
            confirmButton = { Button(onClick = onBack) { Text("Menu") } }
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
                        // Pass the input back to the ViewModel logic
                        viewModel.onCellInput(selectedIndex.intValue, currentInputText.value)
                    }
                    showInputDialog.value = false
                }) { Text("OK") }
            }
        )
    }
}

/**
 * Represents a single interactive square on the puzzle grid.
 */
@Composable
fun GameCell(cell: VisualCell, onClick: () -> Unit) {
    val isBlock = cell.value.isEmpty() && !cell.isEditable
    Box(
        modifier = Modifier.aspectRatio(1f).border(0.5.dp, Color.DarkGray)
            .background(
                when {
                    isBlock -> Color.Black
                    cell.statusColor != Color.Transparent -> cell.statusColor
                    cell.isEditable -> Color.White
                    else -> Color.LightGray
                }
            )
            .clickable(enabled = cell.isEditable) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!isBlock) {
            Text(
                text = if (cell.isEditable) cell.userInput else cell.value,
                color = Color.Black, fontSize = 8.sp, maxLines = 1
            )
        }
    }
}