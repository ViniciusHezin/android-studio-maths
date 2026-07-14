package com.example.cw1a

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


/**
 * Represents the pure data state of a single cell within the puzzle grid.
 * It tracks the underlying correct value, whether the player is allowed to edit it,
 * the player's current input, and the color state for real-time Red/Green validation.
 */

data class VisualCell(
    val value: String = "",
    val isEditable: Boolean = false,
    val userInput: String = "",
    val statusColor: Color = Color.Transparent
)

class GameViewModel : ViewModel() {
    val rows = Random.nextInt(11, 21)
    val cols = Random.nextInt(11, 21)

    val puzzleData = mutableStateListOf<VisualCell>()
    val score = mutableIntStateOf(0)

    var timerEnabled = mutableStateOf(false)
    var timeLeft = mutableIntStateOf(60)
    var isGameOver = mutableStateOf(false)

    var loadingCountdown = mutableIntStateOf(3)
    var isLoading = mutableStateOf(true)

    private var isTimerRunning = false

    private var currentGameId = -1

    fun startNewGame(equationCount: Int, gameId: Int) {
        // THE FIX: If the ID is the same, we just rotated the phone. Do not reset!
        if (currentGameId == gameId) return
        currentGameId = gameId

        puzzleData.clear()
        score.intValue = 0
        timeLeft.intValue = 60
        isGameOver.value = false
        loadingCountdown.intValue = 3
        isLoading.value = true
        isTimerRunning = false

        viewModelScope.launch {
            delay(50L)
            puzzleData.addAll(generateInitialPuzzle(rows, cols, equationCount))

            while (loadingCountdown.intValue > 0) {
                delay(1000L)
                loadingCountdown.intValue--
            }
            isLoading.value = false
            startTimer()
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true

        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (timerEnabled.value && timeLeft.intValue > 0 && !isGameOver.value && !isLoading.value) {
                    timeLeft.intValue--
                    if (timeLeft.intValue == 0) isGameOver.value = true
                }
            }
        }
    }

    fun onCellInput(index: Int, input: String) {
        puzzleData[index] = puzzleData[index].copy(userInput = input)
        score.intValue = validateAndHighlight(puzzleData, rows, cols)

        val editableCells = puzzleData.filter { it.isEditable }
        val isPuzzleComplete = editableCells.isNotEmpty() && editableCells.all { it.statusColor == Color.Green }

        if (isPuzzleComplete) {
            isGameOver.value = true
        }
    }

    private fun generateInitialPuzzle(rows: Int, cols: Int, equationCount: Int): List<VisualCell> {
        val grid = MutableList(rows * cols) { VisualCell() }
        val ops = listOf("+", "-", "x", "/")
        val seeds = listOf(0 to 0, 0 to cols - 5, rows - 5 to 0, rows - 5 to cols - 5, rows / 2 to cols / 2)
        seeds.forEach { (r, c) ->
            val start = (r * cols + c).coerceIn(grid.indices)
            placeStrictEquation(grid, start, 1, ops, rows, cols)
            placeStrictEquation(grid, start, cols, ops, rows, cols)
        }

        // Loops exactly the amount of times the player chose
        repeat(equationCount) {
            val existing = grid.indices.filter { grid[it].value.toIntOrNull() != null }
            if (existing.isNotEmpty()) {
                val start = existing.random()
                val step = if (Random.nextBoolean()) 1 else cols
                placeStrictEquation(grid, start, step, ops, rows, cols)
            }
        }
        return grid
    }

    private fun placeStrictEquation(grid: MutableList<VisualCell>, start: Int, step: Int, ops: List<String>, rows: Int, cols: Int) {
        if (grid[start].value.isNotEmpty() && grid[start].value.toIntOrNull() == null) return
        val n1 = grid[start].value.toIntOrNull() ?: Random.nextInt(1, 20)
        val op = ops.random()
        val n2 = Random.nextInt(1, 15)
        val res = when (op) {
            "+" -> n1 + n2
            "-" -> if (n1 >= n2) n1 - n2 else return
            "x" -> n1 * n2
            "/" -> if (n2 != 0 && n1 % n2 == 0) n1 / n2 else return
            else -> n1 + n2
        }
        val values = listOf(n1.toString(), op, n2.toString(), "=", res.toString())
        for (i in 0..4) {
            val idx = start + (i * step)
            if (idx !in grid.indices || (step == 1 && (start / cols != idx / cols))) return
            val current = grid[idx].value
            if (current.isNotEmpty() && current != values[i]) return
        }
        val hide = listOf(0, 2, 4).random()
        for (i in 0..4) {
            val idx = start + (i * step)
            grid[idx] = VisualCell(value = values[i], isEditable = (i == hide) || grid[idx].isEditable, userInput = grid[idx].userInput)
        }
    }

    private fun validateAndHighlight(puzzleData: MutableList<VisualCell>, rows: Int, cols: Int): Int {
        var points = 0
        val feedback = MutableList(puzzleData.size) { Color.Transparent }
        fun checkSegment(start: Int, step: Int) {
            val indices = (0..4).map { start + it * step }
            val v = indices.map { val cell = puzzleData[it]; if (cell.isEditable) cell.userInput else cell.value }
            if (v.any { it.isEmpty() }) return
            val n1 = v[0].toIntOrNull() ?: return
            val n2 = v[2].toIntOrNull() ?: return
            val res = v[4].toIntOrNull() ?: return
            val isCorrect = when(v[1]) {
                "+" -> n1 + n2 == res
                "-" -> n1 - n2 == res
                "x" -> n1 * n2 == res
                "/" -> n2 != 0 && n1 / n2 == res
                else -> false
            }
            if (isCorrect) points++
            indices.forEach { idx -> if (feedback[idx] != Color.Green) feedback[idx] = if (isCorrect) Color.Green else Color.Red }
        }
        for (r in 0 until rows) {
            for (c in 0 until cols - 4) {
                val idx = r * cols + c
                if (puzzleData[idx + 3].value == "=") checkSegment(idx, 1)
            }
        }
        for (c in 0 until cols) {
            for (r in 0 until rows - 4) {
                val idx = r * cols + c
                if (idx + 3 * cols < puzzleData.size && puzzleData[idx + 3 * cols].value == "=") checkSegment(idx, cols)
            }
        }
        for (i in puzzleData.indices) {
            puzzleData[i] = puzzleData[i].copy(statusColor = feedback[i])
        }
        return points
    }
}