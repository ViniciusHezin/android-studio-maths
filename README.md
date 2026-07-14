# Compose Math Puzzle (Android)

> This coursework is an Android mobile app assignment built solely with Jetpack Compose. The goal is to develop an interactive math puzzle game where users solve a dynamically generated grid of equations. It features real-time validation with color-coded feedback, dynamic equation updates, a scoring system, and game controls like a "New Game" button.

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

## Screenshots & Demo

Start Screen: 
<img width="498" height="917" alt="image" src="https://github.com/user-attachments/assets/0115862b-1e17-4d49-b382-b00be4e505fe" />
Setting the difficulty:
<img width="460" height="486" alt="image" src="https://github.com/user-attachments/assets/aca62c89-4529-4ddd-a1b8-6476cd7618a3" />
First Gamescreen:
<img width="467" height="607" alt="image" src="https://github.com/user-attachments/assets/38a973c2-fe86-4e15-a4ee-eda915d2042b" />
Entering numbers:
<img width="461" height="568" alt="image" src="https://github.com/user-attachments/assets/64daa47b-c8bc-4318-b9ac-156548daa730" />
Validating answers: 
<img width="460" height="624" alt="image" src="https://github.com/user-attachments/assets/d9efa22b-208f-441e-88fa-1d0f1cbeb6bc" />
Finishing the first grid:
<img width="471" height="537" alt="image" src="https://github.com/user-attachments/assets/b8c0428a-4e94-4826-bd83-c3eab7ca5b70" />

Advanced mode (60s timer)
<img width="467" height="596" alt="image" src="https://github.com/user-attachments/assets/9b3133ef-040d-448c-89da-a7734aa39fc0" />
Game-over Screen when timer ends:
<img width="440" height="569" alt="image" src="https://github.com/user-attachments/assets/90c9ea96-9676-49ed-8740-bbbdacfd303c" />


## Features & Functionality

*   **100% Declarative UI:** The entire user interface is built strictly using Jetpack Compose, completely avoiding legacy XML Views and third-party UI libraries.
*   **Dynamic Equation Grid:** Automatically generates non-overlapping math equations for each session.
*   **Real-Time State Validation:** 
    *   Users can input and edit numbers in empty cells.
    *   Immediate color-coded feedback: Green for correct equations, Red for incorrect.
*   **Intersecting Cell Logic:** Handles complex state updates where a single empty cell is shared between multiple intersecting equations, ensuring both equations recalculate and validate simultaneously.
*   **Live Scoring System:** Tracks and actively updates the user's score in the top-right corner based on correct inputs.

## Tech Stack & Architecture

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Architecture:** Unidirectional Data Flow (UDF) 
*   **State Management:** Extensively utilized Compose State (`remember`, `MutableState`) to trigger highly optimized recompositions only when specific cell values or game scores change.

## Technical Challenges & Solutions
Dynamic Grid Generation & Equation Placement

Challenge: Scaling the board dynamically from a 5x5 up to a 20x20 grid presented significant spatial challenges. Initially, randomly generating starting coordinates for the equations (which require 5 contiguous squares) often resulted in failed placements. Equations would hit grid boundaries or violate the strict "no overlapping" rule, leaving the grid sparsely populated or failing to fill the board efficiently.
Solution: To resolve this, I implemented a coordinate-based validation matrix paired with a backtracking algorithm. Instead of randomly guessing starting coordinates and failing, the algorithm first scans the 2D matrix to identify all valid, contiguous horizontal and vertical slots that can accommodate the equation's length. It then randomly selects from these guaranteed valid slots. If the algorithm traps itself in a layout where no more equations can fit, the backtracking mechanism reverts the last placement to try an alternative configuration, ensuring a dense and fully utilized grid regardless of the chosen dimensions.
State Management for Intersecting Equations

Challenge: If a user changed a number in a shared cell, multiple independent equations needed to instantly validate their new totals and update their UI color state (red/green) without causing unnecessary recomposition of the entire grid.
Solution: Elevated the grid state to a centralized ViewModel, ensuring that each cell observed its specific state, triggering localized recompositions rather than refreshing the whole screen.
