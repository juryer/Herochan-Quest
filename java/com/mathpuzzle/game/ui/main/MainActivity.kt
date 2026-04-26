package com.mathpuzzle.game.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mathpuzzle.game.databinding.ActivityMainBinding
import com.mathpuzzle.game.ui.dungeon.DungeonMenuActivity
import com.mathpuzzle.game.ui.highscore.HighScoreActivity
import com.mathpuzzle.game.ui.howtoplay.HowToPlayActivity
import com.mathpuzzle.game.ui.training.TrainingActivity
import com.mathpuzzle.game.util.SoundManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)

        binding.btnDungeon.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            DungeonMenuActivity.start(this)
        }
        binding.btnTraining.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            TrainingActivity.start(this)
        }
        binding.btnHighScore.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            HighScoreActivity.start(this)
        }
        binding.btnHowToPlay.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            HowToPlayActivity.start(this)
        }
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "「設定」は近日実装予定です！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() { super.onResume(); SoundManager.playBgm(this, SoundManager.BgmTrack.MAIN_MENU) }
    override fun onPause() { super.onPause(); SoundManager.stopBgm() }
}
