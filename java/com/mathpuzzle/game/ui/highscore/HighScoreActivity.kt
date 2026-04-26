package com.mathpuzzle.game.ui.highscore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mathpuzzle.game.data.SaveDataManager
import com.mathpuzzle.game.databinding.ActivityHighScoreBinding

class HighScoreActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HighScoreActivity::class.java))
        }
    }

    private lateinit var binding: ActivityHighScoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        loadHighScores()
    }

    private fun loadHighScores() {
        val scores = SaveDataManager.getTrainingHighScores(this)
        listOf(binding.tvRank1, binding.tvRank2, binding.tvRank3, binding.tvRank4, binding.tvRank5)
            .forEachIndexed { index, tv ->
                tv.text = if (index < scores.size) "${index + 1}位：${scores[index]} pt" else "${index + 1}位：---"
            }
    }
}
