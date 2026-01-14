package com.rongo.carnumtwo.core.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rongo.carnumtwo.feature.score.ScoreItem

class ScoreStorage(context: Context) {
    private val prefs = context.getSharedPreferences("scores_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Retrieves the list of top scores.
     */
    fun getTopScores(): List<ScoreItem> {
        val json = prefs.getString("top_scores", null) ?: return emptyList()
        val type = object : TypeToken<List<ScoreItem>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Checks if the new score qualifies for the top 10 list.
     */
    fun isHighScore(newScore: Int): Boolean {
        val scores = getTopScores()
        // If list is not full, any score is a high score
        if (scores.size < 10) return true

        // Otherwise, check if it beats the lowest score
        val lowestScore = scores.minByOrNull { it.score }?.score ?: 0
        return newScore > lowestScore
    }

    /**
     * Adds a new score with player name and location, then sorts and trims the list.
     */
    fun addScore(name: String, score: Int, lat: Double, lon: Double) {
        val currentList = getTopScores().toMutableList()
        currentList.add(ScoreItem(name, score, lat, lon))

        // Sort descending by score and keep only top 10
        val top10 = currentList.sortedByDescending { it.score }.take(10)
        saveList(top10)
    }

    private fun saveList(list: List<ScoreItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString("top_scores", json).apply()
    }
}