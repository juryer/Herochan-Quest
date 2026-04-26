package com.mathpuzzle.game.ui.training

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mathpuzzle.game.R
import com.mathpuzzle.game.data.SaveDataManager
import com.mathpuzzle.game.databinding.ActivityTrainingBinding
import com.mathpuzzle.game.util.ImageLoader

class TrainingActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, TrainingActivity::class.java))
        }
    }

    private lateinit var binding: ActivityTrainingBinding
    private val viewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 画面端スワイプの「戻る」ジェスチャーを無効化
        onBackPressedDispatcher.addCallback(this) { }

        viewModel.init()
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        val scores = SaveDataManager.getTrainingHighScores(this)
        binding.tvHighScore.text = if (scores.isNotEmpty()) "ハイスコア: ${scores.first()}" else "ハイスコア: ---"

        // トレーニングモードは14行で表示
        binding.numberGridView.gridRows = 14

        try { binding.ivChar1.setImageResource(R.drawable.hero_girl) } catch (e: Exception) { binding.ivChar1.setBackgroundColor(0xFF4444AA.toInt()) }
        try { binding.ivChar2.setImageResource(R.drawable.dragonkids) } catch (e: Exception) { binding.ivChar2.setBackgroundColor(0xFF44AA44.toInt()) }
        try { binding.ivChar3.setImageResource(R.drawable.unicorn) } catch (e: Exception) { binding.ivChar3.setBackgroundColor(0xFFAA44AA.toInt()) }

        setRandomHiddenImage()

        binding.panelChar1.setOnClickListener { viewModel.activateBurst(TrainingViewModel.BurstType.HERO) }
        binding.panelChar2.setOnClickListener { viewModel.activateBurst(TrainingViewModel.BurstType.DRAGON) }
        binding.panelChar3.setOnClickListener { viewModel.activateBurst(TrainingViewModel.BurstType.UNICORN) }

        binding.numberGridView.onCellTouched = { row, col -> viewModel.onCellTouched(row, col) }
        binding.numberGridView.onFingerReleased = { viewModel.onFingerReleased() }
        binding.numberGridView.onRectSelection = { sr, sc, er, ec -> viewModel.onRectSelection(sr, sc, er, ec) }

        binding.btnStart.setOnClickListener { setRandomHiddenImage(); viewModel.startTraining() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setRandomHiddenImage() {
        val resId = ImageLoader.HiddenImages.getRandom()
        if (resId != null) binding.ivHiddenImage.setImageResource(resId)
        else {
            binding.ivHiddenImage.setImageResource(android.R.color.transparent)
            binding.ivHiddenImage.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_dark))
        }
    }

    private fun observeViewModel() {
        viewModel.grid.observe(this) { binding.numberGridView.setCells(it) }
        viewModel.score.observe(this) { binding.tvScore.text = it.toString() }
        viewModel.timer.observe(this) { sec ->
            binding.tvTimer.text = sec.toString()
            binding.tvTimer.setTextColor(
                if (sec <= 10) ContextCompat.getColor(this, R.color.timer_danger)
                else ContextCompat.getColor(this, R.color.timer_normal)
            )
        }
        viewModel.isRunning.observe(this) { running ->
            binding.btnStart.visibility = if (running) View.GONE else View.VISIBLE
            binding.numberGridView.isEnabled = running
        }
        viewModel.heroBurstUsed.observe(this) { used ->
            binding.panelChar1.alpha = if (used) 0.4f else 1.0f
            binding.panelChar1.isClickable = !used
        }
        viewModel.dragonBurstUsed.observe(this) { used ->
            binding.panelChar2.alpha = if (used) 0.4f else 1.0f
            binding.panelChar2.isClickable = !used
        }
        viewModel.unicornBurstUsed.observe(this) { used ->
            binding.panelChar3.alpha = if (used) 0.4f else 1.0f
            binding.panelChar3.isClickable = !used
        }
        viewModel.dragonBurstActive.observe(this) { active ->
            binding.panelChar2.setBackgroundColor(
                if (active) 0xFFFFAA00.toInt() else 0x00000000.toInt()
            )
        }
        viewModel.isFinished.observe(this) { finished ->
            if (!finished) return@observe
            val score = viewModel.getFinalScore()
            SaveDataManager.recordTrainingScore(this, score)
            val scores = SaveDataManager.getTrainingHighScores(this)
            binding.tvHighScore.text = "ハイスコア: ${scores.firstOrNull() ?: 0}"
            AlertDialog.Builder(this, R.style.GameDialog)
                .setTitle("タイムアップ！")
                .setMessage("スコア: $score\n\nもう一度挑戦しますか？")
                .setCancelable(false)
                .setPositiveButton("もう一度") { _, _ -> setRandomHiddenImage(); viewModel.startTraining() }
                .setNegativeButton("戻る") { _, _ -> finish() }
                .show()
        }
    }
}
