package com.mathpuzzle.game.ui.battle

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mathpuzzle.game.R
import com.mathpuzzle.game.data.SaveDataManager
import com.mathpuzzle.game.databinding.ActivityBattleBinding
import com.mathpuzzle.game.model.BattleResult
import com.mathpuzzle.game.model.LevelConfig
import com.mathpuzzle.game.util.SoundManager

class BattleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL = "extra_level"
        fun start(context: Context, level: Int) {
            context.startActivity(Intent(context, BattleActivity::class.java).putExtra(EXTRA_LEVEL, level))
        }
    }

    private lateinit var binding: ActivityBattleBinding
    private val viewModel: BattleViewModel by viewModels()
    private var level: Int = 1
    private var resultHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBattleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) { }

        level = intent.getIntExtra(EXTRA_LEVEL, 1)
        val enemy = LevelConfig.getEnemyForLevel(level) ?: run { finish(); return }

        viewModel.init(enemy)
        setupUI(enemy)
        observeViewModel()
        SoundManager.playBgm(this, SoundManager.BgmTrack.BATTLE)
    }

    private fun setupUI(enemy: com.mathpuzzle.game.model.EnemyData) {
        if (enemy.bgResId != 0) binding.ivBattleBg.setImageResource(enemy.bgResId)
        else binding.ivBattleBg.setBackgroundColor(0xFF131D2B.toInt())

        if (enemy.imageResId != 0) binding.ivEnemy.setImageResource(enemy.imageResId)
        else binding.ivEnemy.setBackgroundColor(0xFF44AA77.toInt())

        try { binding.ivChar1.setImageResource(R.drawable.hero_girl) } catch (e: Exception) { binding.ivChar1.setBackgroundColor(0xFF4444AA.toInt()) }
        try { binding.ivChar2.setImageResource(R.drawable.dragonkids) } catch (e: Exception) { binding.ivChar2.setBackgroundColor(0xFF44AA44.toInt()) }
        try { binding.ivChar3.setImageResource(R.drawable.unicorn) } catch (e: Exception) { binding.ivChar3.setBackgroundColor(0xFFAA44AA.toInt()) }

        binding.panelChar1.setOnClickListener { showBurstDialog(BattleViewModel.BurstType.HERO) }
        binding.panelChar2.setOnClickListener { showBurstDialog(BattleViewModel.BurstType.DRAGON) }
        binding.panelChar3.setOnClickListener { showBurstDialog(BattleViewModel.BurstType.UNICORN) }

        binding.numberGridView.onCellTouched = { row, col ->
            viewModel.onCellTouched(row, col)
            SoundManager.playSe(SoundManager.SoundEffect.SELECT)
        }
        binding.numberGridView.onFingerReleased = { viewModel.onFingerReleased() }
        binding.numberGridView.onRectSelection = { sr, sc, er, ec -> viewModel.onRectSelection(sr, sc, er, ec) }

        binding.btnAttack.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            viewModel.onAttackButtonPressed()
        }

        binding.btnRetreat.setOnClickListener {
            SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            AlertDialog.Builder(this, R.style.GameDialog)
                .setTitle("撤退").setMessage("ダンジョンに戻りますか？")
                .setPositiveButton("撤退する") { _, _ -> finish() }
                .setNegativeButton("戻る", null).show()
        }
    }

    private fun showBurstDialog(type: BattleViewModel.BurstType) {
        if (!viewModel.canSelectBurst()) return

        val info = viewModel.burstInfoList.find { it.type == type } ?: return
        if (!viewModel.canUseBurst(type)) {
            val reason = if (viewModel.burstUsedThisTurn.value == true) "このターンはすでにバーストを使用しました"
            else "${info.name} はすでに使用済みです"
            AlertDialog.Builder(this, R.style.GameDialog)
                .setTitle("バースト使用不可").setMessage(reason).setPositiveButton("OK", null).show()
            return
        }

        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle("【${info.name}】")
            .setMessage("${info.description}\n\n発動しますか？")
            .setPositiveButton("発動する") { _, _ ->
                viewModel.activateBurst(type)
                SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
            }
            .setNegativeButton("やめる", null).show()
    }

    private fun observeViewModel() {
        val enemy = LevelConfig.getEnemyForLevel(level) ?: return
        val maxEnemyHp = enemy.hp

        viewModel.grid.observe(this) { binding.numberGridView.setCells(it) }

        viewModel.playerHp.observe(this) { hp ->
            val max = viewModel.getPlayerMaxHp()
            updateHpBar(binding.progressPlayerHp, hp, max)
            binding.tvPlayerHp.text = "$hp / $max"
        }

        viewModel.enemyHp.observe(this) { hp ->
            updateHpBar(binding.progressEnemyHp, hp, maxEnemyHp)
            binding.tvEnemyHp.text = "$hp / $maxEnemyHp"
        }

        viewModel.timer.observe(this) { sec ->
            binding.tvTimer.text = sec.toString()
            binding.tvTimer.setTextColor(
                if (sec <= 5) ContextCompat.getColor(this, R.color.timer_danger)
                else ContextCompat.getColor(this, R.color.timer_normal)
            )
        }

        viewModel.timerRunning.observe(this) { running ->
            binding.tvTimer.visibility = if (running) View.VISIBLE else View.INVISIBLE
            binding.numberGridView.isEnabled = running
            updateAttackButtonVisibility()
            updateCharPanelAlpha()
        }

        viewModel.attackButtonReady.observe(this) {
            updateAttackButtonVisibility()
            updateCharPanelAlpha()
        }

        viewModel.phaseMessage.observe(this) { msg ->
            if (msg.isNullOrEmpty()) binding.tvPhaseMessage.visibility = View.GONE
            else { binding.tvPhaseMessage.visibility = View.VISIBLE; binding.tvPhaseMessage.text = msg }
        }

        viewModel.specialMessage.observe(this) { msg ->
            if (msg.isNullOrEmpty()) binding.layoutSpecialMessage.visibility = View.GONE
            else {
                binding.tvSpecialMessage.text = msg
                binding.layoutSpecialMessage.visibility = View.VISIBLE
                binding.layoutSpecialMessage.alpha = 0f
                binding.layoutSpecialMessage.animate().alpha(1f).setDuration(300).start()
            }
        }

        viewModel.heroBurstUsed.observe(this) { updateCharPanelAlpha() }
        viewModel.dragonBurstUsed.observe(this) { updateCharPanelAlpha() }
        viewModel.unicornBurstUsed.observe(this) { updateCharPanelAlpha() }

        viewModel.damageEvent.observe(this) { event ->
            event ?: return@observe
            when (event.target) {
                BattleViewModel.DamageTarget.ENEMY -> shakeView(binding.ivEnemy)
                BattleViewModel.DamageTarget.PLAYER -> shakeView(binding.layoutPlayerArea)
            }
            SoundManager.playSe(SoundManager.SoundEffect.DAMAGE)
        }

        viewModel.battleResult.observe(this) { result ->
            if (result == BattleResult.ONGOING || resultHandled) return@observe
            resultHandled = true
            when (result) {
                BattleResult.PLAYER_WIN -> {
                    SoundManager.playBgm(this, SoundManager.BgmTrack.VICTORY, loop = false)
                    SaveDataManager.recordLevelClear(this, level, viewModel.getTotalDamageDealt())
                    showResultDialog(true)
                }
                BattleResult.PLAYER_LOSE -> {
                    SoundManager.playBgm(this, SoundManager.BgmTrack.GAME_OVER, loop = false)
                    showResultDialog(false)
                }
                else -> {}
            }
        }
    }

    private fun updateAttackButtonVisibility() {
        val ready = viewModel.attackButtonReady.value ?: true
        val running = viewModel.timerRunning.value ?: false
        binding.btnAttack.visibility = if (ready && !running) View.VISIBLE else View.GONE
    }

    private fun updateCharPanelAlpha() {
        val canSelect = viewModel.canSelectBurst()
        val heroUsed = viewModel.heroBurstUsed.value ?: false
        val dragonUsed = viewModel.dragonBurstUsed.value ?: false
        val unicornUsed = viewModel.unicornBurstUsed.value ?: false
        binding.panelChar1.alpha = if (!canSelect) 0.3f else if (heroUsed) 0.4f else 1.0f
        binding.panelChar2.alpha = if (!canSelect) 0.3f else if (dragonUsed) 0.4f else 1.0f
        binding.panelChar3.alpha = if (!canSelect) 0.3f else if (unicornUsed) 0.4f else 1.0f
    }

    private fun updateHpBar(progressBar: android.widget.ProgressBar, current: Int, max: Int) {
        progressBar.max = max
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, current).apply {
            duration = 500; interpolator = DecelerateInterpolator(); start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, -20f, 20f, -15f, 15f, -10f, 10f, 0f).apply {
            duration = 400; start()
        }
    }

    private fun showResultDialog(isWin: Boolean) {
        val totalDamage = viewModel.getTotalDamageDealt()
        val title = if (isWin) "クリア！" else "ゲームオーバー"
        val message = if (isWin) {
            "LEVEL $level クリア！\n合計ダメージ: $totalDamage"
        } else {
            "倒されてしまった…\n合計ダメージ: $totalDamage\n\nダンジョンに戻りますか？"
        }
        AlertDialog.Builder(this, R.style.GameDialog)
            .setTitle(title).setMessage(message).setCancelable(false)
            .setPositiveButton("戻る") { _, _ -> finish() }
            .apply {
                if (isWin && LevelConfig.getEnemyForLevel(level + 1) != null)
                    setNegativeButton("次のレベルへ") { _, _ -> start(this@BattleActivity, level + 1); finish() }
            }.show()
    }

    override fun onDestroy() { super.onDestroy(); SoundManager.stopBgm() }
}
