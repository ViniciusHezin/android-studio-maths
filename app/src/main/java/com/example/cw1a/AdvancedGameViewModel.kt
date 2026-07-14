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
 * ==============================================================================
 * ADVANCED LEVEL ALGORITHM DOCUMENTATION
 * ==============================================================================
 * * APPROACH:
 * The Advanced Level utilizes a "Seed and Expand" procedural generation algorithm
 * on a fixed 20x20 grid. First, it plants 5 guaranteed "seed" equations at strategic
 * anchor points (corners and center) to ensure the board spans the entire grid.
 * Then, it runs a high-density loop (550 iterations) that searches for existing numbers
 * on the board and attempts to build new, intersecting equations off of them.
 * Finally, to increase difficulty, the masking logic randomly hides up to two parts
 * of the equation (operands or the result) instead of just one.
 * * JUSTIFICATION & PERFORMANCE:
 * Rather than trying to calculate a perfect, pre-solved 20x20 crossword from scratch
 * (which is mathematically heavy and can cause UI freezing on mobile devices), this
 * algorithm builds the board organically by verifying localized math chunks one step
 * at a time. It performs exceptionally well because checking if a 5-cell line is empty
 * and fits a simple math rule (e.g., n1 + n2 = res) operates in O(1) time per attempt.
 * * ADVANTAGES:
 * 1. Highly Performant: The generation happens almost instantly without blocking the Main Thread.
 * 2. Guaranteed Solvability: Because every new equation is generated using valid math
 * stemming from an existing number, the puzzle is mathematically guaranteed to be solvable.
 * 3. High Intersectivity: Building off existing numbers creates a dense, interconnected crossword
 * feel rather than isolated math problems.
 * * DISADVANTAGES:
 * 1. Blank Space Variations: Because of the random direction plotting, some edges of the
 * 20x20 board might remain empty if the random number generator heavily favors the center.
 * 2. Occasional "Islands": If an equation is generated but its hidden numbers are too
 * isolated, it might require brute-force guessing by the user if it doesn't intersect
 * with a known variable.
 * ==============================================================================
 */


/**
 * ViewModel that manages the logic, state, and strict 60-second timer
 * for the 20x20 Advanced Level cross-math puzzle.
 */
class AdvancedGameViewModel : ViewModel() {
    val rows = 20
    val cols = 20

    val puzzleData = mutableStateListOf<VisualCell>()
    val score = mutableIntStateOf(0)

    var timerEnabled = mutableStateOf(true)
    var timeLeft = mutableIntStateOf(60)
    var isGameOver = mutableStateOf(false)

    var loadingCountdown = mutableIntStateOf(3)
    var isLoading = mutableStateOf(true)

    private var isTimerRunning = false
    private var currentGameId = -1

    /**
* Initializes a fresh game session.
 */
    fun startNewGame(gameId: Int) {
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
            puzzleData.addAll(generateInitialPuzzle(rows, cols))

            while (loadingCountdown.intValue > 0) {
                delay(1000L)
                loadingCountdown.intValue--
            }
            isLoading.value = false
            startTimer()
        }
    }
    /**
     * Launches a background coroutine to count down the clock every second.
     */
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

    /**
     * Handles the player's number input for a specific grid cell.
     */
    fun onCellInput(index: Int, input: String) {
        puzzleData[index] = puzzleData[index].copy(userInput = input)
        score.intValue = validateAndHighlight(puzzleData, rows, cols)

        // REQUIREMENT 7: WIN STATE CHECK
        val editableCells = puzzleData.filter { it.isEditable }
        val isPuzzleComplete = editableCells.isNotEmpty() && editableCells.all { it.statusColor == Color.Green }

        if (isPuzzleComplete) {
            isGameOver.value = true
        }
    }
    /**
     * Procedurally generates the puzzle grid using a "Seed and Expand" algorithm.
     */

    private fun generateInitialPuzzle(rows: Int, cols: Int): List<VisualCell> {
        val grid = MutableList(rows * cols) { VisualCell() }
        val ops = listOf("+", "-", "x", "/")
        val seeds = listOf(0 to 0, 0 to cols - 5, rows - 5 to 0, rows - 5 to cols - 5, rows / 2 to cols / 2)
        seeds.forEach { (r, c) ->
            val start = (r * cols + c).coerceIn(grid.indices)
            placeStrictEquation(grid, start, 1, ops, rows, cols)
            placeStrictEquation(grid, start, cols, ops, rows, cols)
        }
        repeat(550) {
            val existing = grid.indices.filter { grid[it].value.toIntOrNull() != null }
            if (existing.isNotEmpty()) {
                val start = existing.random()
                val step = if (Random.nextBoolean()) 1 else cols
                placeStrictEquation(grid, start, step, ops, rows, cols)
            }
        }
        return grid
    }

    /**
     * Attempts to place a single 5-part mathematical equation on the grid horizontally or vertically.
     */
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

        val hideIndices = mutableListOf(0, 2, 4)
        hideIndices.shuffle()
        val hiddenCells = hideIndices.take(Random.nextInt(1, 3))

        for (i in 0..4) {
            val idx = start + (i * step)
            grid[idx] = VisualCell(
                value = values[i],
                isEditable = hiddenCells.contains(i) || grid[idx].isEditable,
                userInput = grid[idx].userInput
            )
        }
    }

    /**
     * Scans the entire grid horizontally and vertically to find and validate completed equations.
     */
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