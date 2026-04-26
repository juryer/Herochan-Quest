package com.mathpuzzle.game.data

import android.content.Context
import com.google.gson.Gson
import com.mathpuzzle.game.model.SaveData

object SaveDataManager {

    private const val PREF_NAME = "math_puzzle_save"
    private const val KEY_SAVE_DATA = "save_data"
    private const val MAX_HIGH_SCORES = 5
    private val gson = Gson()

    fun load(context: Context): SaveData {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SAVE_DATA, null)
        return if (json != null) {
            try { gson.fromJson(json, SaveData::class.java) }
            catch (e: Exception) { SaveData() }
        } else SaveData()
    }

    fun save(context: Context, saveData: SaveData) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVE_DATA, gson.toJson(saveData)).apply()
    }

    fun recordLevelClear(context: Context, level: Int, score: Int) {
        val saveData = load(context)
        saveData.clearedLevels.add(level)
        val currentBest = saveData.highScores[level] ?: 0
        if (score > currentBest) saveData.highScores[level] = score
        save(context, saveData)
    }

    fun recordTrainingScore(context: Context, score: Int) {
        val saveData = load(context)
        saveData.trainingHighScores.add(score)
        val sorted = saveData.trainingHighScores.sortedDescending().take(MAX_HIGH_SCORES).toMutableList()
        saveData.trainingHighScores.clear()
        saveData.trainingHighScores.addAll(sorted)
        save(context, saveData)
    }

    fun getTrainingHighScores(context: Context): List<Int> {
        return load(context).trainingHighScores.sortedDescending().take(MAX_HIGH_SCORES)
    }

    fun isLevelUnlocked(context: Context, level: Int): Boolean {
        if (level <= 1) return true
        return load(context).clearedLevels.contains(level - 1)
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
