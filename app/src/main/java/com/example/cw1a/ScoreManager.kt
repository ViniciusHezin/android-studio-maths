package com.example.cw1a

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_scores")

/**
 * Manages persistent local storage for the game's high scores using Jetpack DataStore.
 */
class ScoreManager(private val context: Context) {
    private val highScoreKey = intPreferencesKey("high_score")
    private val advancedHighScoreKey = intPreferencesKey("advanced_high_score")

    // --- NORMAL MODE SAVE DATA ---
    val highScoreFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[highScoreKey] ?: 0
    }

    suspend fun saveHighScore(score: Int) {
        context.dataStore.edit { settings ->
            val currentHigh = settings[highScoreKey] ?: 0
            if (score > currentHigh) {
                settings[highScoreKey] = score
            }
        }
    }

    // --- ADVANCED MODE SAVE DATA ---
    val advancedHighScoreFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[advancedHighScoreKey] ?: 0
    }

    suspend fun saveAdvancedHighScore(score: Int) {
        context.dataStore.edit { settings ->
            val currentHigh = settings[advancedHighScoreKey] ?: 0
            if (score > currentHigh) {
                settings[advancedHighScoreKey] = score
            }
        }
    }
}