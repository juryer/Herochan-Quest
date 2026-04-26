package com.mathpuzzle.game.ui.dungeon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mathpuzzle.game.R
import com.mathpuzzle.game.data.SaveDataManager
import com.mathpuzzle.game.databinding.ActivityDungeonMenuBinding
import com.mathpuzzle.game.model.LevelConfig
import com.mathpuzzle.game.ui.battle.BattleActivity
import com.mathpuzzle.game.util.SoundManager

class DungeonMenuActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DungeonMenuActivity::class.java))
        }
    }

    private lateinit var binding: ActivityDungeonMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDungeonMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.rvLevels.layoutManager = LinearLayoutManager(this)
        setupLevelList()
    }

    override fun onResume() { super.onResume(); setupLevelList() }

    private fun setupLevelList() {
        val saveData = SaveDataManager.load(this)
        binding.rvLevels.adapter = LevelAdapter(
            levels = LevelConfig.levels,
            clearedLevels = saveData.clearedLevels,
            highScores = saveData.highScores,
            onLevelSelected = { level ->
                SoundManager.playSe(SoundManager.SoundEffect.BUTTON_CLICK)
                BattleActivity.start(this, level)
            }
        )
    }
}

class LevelAdapter(
    private val levels: List<com.mathpuzzle.game.model.EnemyData>,
    private val clearedLevels: Set<Int>,
    private val highScores: Map<Int, Int>,
    private val onLevelSelected: (Int) -> Unit
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    private val unlockedLevels = levels.filter { enemy ->
        enemy.level == 1 || (enemy.level - 1) in clearedLevels
    }

    class LevelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLevelTitle: TextView = view.findViewById(R.id.tvLevelTitle)
        val tvSubTitle: TextView = view.findViewById(R.id.tvSubTitle)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val viewLockOverlay: View = view.findViewById(R.id.viewLockOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LevelViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false))

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val enemy = unlockedLevels[position]
        val isCleared = enemy.level in clearedLevels
        holder.tvLevelTitle.text = "LEVEL${enemy.level}"
        holder.tvSubTitle.text = enemy.subTitle
        holder.viewLockOverlay.visibility = View.GONE
        holder.tvStatus.text = if (isCleared) "✅" else ""
        val params = holder.itemView.layoutParams
        params.width = RecyclerView.LayoutParams.MATCH_PARENT
        holder.itemView.layoutParams = params
        holder.itemView.setOnClickListener { onLevelSelected(enemy.level) }
    }

    override fun getItemCount() = unlockedLevels.size
}
