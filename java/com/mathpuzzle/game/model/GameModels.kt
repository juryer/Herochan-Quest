package com.mathpuzzle.game.model

import com.mathpuzzle.game.R

data class SaveData(
    val clearedLevels: MutableSet<Int> = mutableSetOf(),
    val highScores: MutableMap<Int, Int> = mutableMapOf(),
    val trainingHighScores: MutableList<Int> = mutableListOf(),
    val totalPlayCount: Int = 0,
    val version: Int = 1
)

data class EnemyData(
    val id: Int,
    val name: String,
    val hp: Int,
    val attackDamage: Int,
    val imageResId: Int,
    val level: Int,
    val subTitle: String = "最強スライムとの激闘",
    val bgResId: Int = 0
)

data class PlayerData(
    val maxHp: Int = 10_000,
    var currentHp: Int = 10_000
)

enum class BattleResult {
    ONGOING, PLAYER_WIN, PLAYER_LOSE
}

data class GridCell(
    val row: Int,
    val col: Int,
    var value: Int,
    var isSelected: Boolean = false,
    var isCleared: Boolean = false
)

object LevelConfig {

    val levels: List<EnemyData> = listOf(
        EnemyData(
            id = 1,
            name = "スライム",
            hp = 3000,
            attackDamage = 2000,
            imageResId = R.drawable.slime,
            level = 1,
            subTitle = "スライムとの激闘",
            bgResId = R.drawable.bg_battle_forest
        ),
        EnemyData(
            id = 2,
            name = "ゴブリン",
            hp = 5000,
            attackDamage = 2500,
            imageResId = R.drawable.goblin,
            level = 2,
            subTitle = "ゴブリンとの激闘",
            bgResId = R.drawable.bg_battle_cave
        ),
        EnemyData(
            id = 3,
            name = "サラマンダー",
            hp = 8000,
            attackDamage = 2500,
            imageResId = R.drawable.salamander,
            level = 3,
            subTitle = "サラマンダーとの激闘",
            bgResId = R.drawable.bg_battle_kazan
        ),
        EnemyData(
            id = 4,
            name = "アルティメットスライム",
            hp = 15000,
            attackDamage = 3000,
            imageResId = R.drawable.ultimateslime,
            level = 4,
            subTitle = "参上、究極のスライム",
            bgResId = R.drawable.bg_battle_forest
        ),
        EnemyData(
            id = 5,
            name = "ドラキュラ",
            hp = 20000,
            attackDamage = 4000,
            imageResId = R.drawable.dracula,
            level = 5,
            subTitle = "夜の貴公子との邂逅",
            bgResId = R.drawable.bg_battle_castle
        )
    )

    fun getEnemyForLevel(level: Int): EnemyData? = levels.find { it.level == level }
    fun maxLevel(): Int = levels.size
    fun calcPlayerDamage(tenCount: Int): Int = tenCount * 100
}
