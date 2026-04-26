package com.mathpuzzle.game.ui.battle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathpuzzle.game.model.*
import kotlinx.coroutines.*

class BattleViewModel : ViewModel() {

    companion object {
        const val GRID_ROWS = 10
        const val GRID_COLS = 9
        const val OPERATION_TIME_SECONDS = 15
        const val TARGET_SUM = 10
        const val SPECIAL_SUM_20 = 20

        // ========================================
        // バランス調整エリア
        // ========================================
        const val BASE_DAMAGE = 200
        const val BONUS_PER_EXTRA_CELL = 50
        const val SPECIAL_20_DAMAGE = 800
        const val COMBO_RATE = 0.1
        const val HERO_BURST_RATE = 0.3
        const val UNICORN_HEAL_RATE = 5
        // ========================================

        fun calcDamage(cellCount: Int): Int =
            if (cellCount <= 3) BASE_DAMAGE
            else BASE_DAMAGE + (cellCount - 3) * BONUS_PER_EXTRA_CELL

        fun comboMultiplier(comboCount: Int): Double =
            1.0 + (comboCount - 1) * COMBO_RATE
    }

    enum class BurstType { HERO, DRAGON, UNICORN }

    data class BurstInfo(val type: BurstType, val name: String, val description: String)

    val burstInfoList = listOf(
        BurstInfo(BurstType.HERO, "ゴーゴードライブ", "攻撃開始後の盤面で、パネルの約30%の数字を5に変えるよ！"),
        BurstInfo(BurstType.DRAGON, "ドラゴンソウル", "そのターンの攻撃を2回行うよ！"),
        BurstInfo(BurstType.UNICORN, "ホーリーホーン", "そのターンに与えたダメージの${UNICORN_HEAL_RATE}倍分HPを回復するよ！")
    )

    private val _grid = MutableLiveData<List<GridCell>>()
    val grid: LiveData<List<GridCell>> = _grid
    private val _playerHp = MutableLiveData<Int>()
    val playerHp: LiveData<Int> = _playerHp
    private val _enemyHp = MutableLiveData<Int>()
    val enemyHp: LiveData<Int> = _enemyHp
    private val _timer = MutableLiveData<Int>()
    val timer: LiveData<Int> = _timer
    private val _timerRunning = MutableLiveData<Boolean>(false)
    val timerRunning: LiveData<Boolean> = _timerRunning
    private val _turnDamage = MutableLiveData<Int>(0)
    val turnDamage: LiveData<Int> = _turnDamage
    private val _battleResult = MutableLiveData<BattleResult>(BattleResult.ONGOING)
    val battleResult: LiveData<BattleResult> = _battleResult
    private val _phaseMessage = MutableLiveData<String>("")
    val phaseMessage: LiveData<String> = _phaseMessage
    private val _specialMessage = MutableLiveData<String?>(null)
    val specialMessage: LiveData<String?> = _specialMessage
    private val _selectedPath = MutableLiveData<List<GridCell>>(emptyList())
    val selectedPath: LiveData<List<GridCell>> = _selectedPath
    private val _damageEvent = MutableLiveData<DamageEvent?>()
    val damageEvent: LiveData<DamageEvent?> = _damageEvent
    private val _burstUsedThisTurn = MutableLiveData<Boolean>(false)
    val burstUsedThisTurn: LiveData<Boolean> = _burstUsedThisTurn
    private val _heroBurstUsed = MutableLiveData<Boolean>(false)
    val heroBurstUsed: LiveData<Boolean> = _heroBurstUsed
    private val _dragonBurstUsed = MutableLiveData<Boolean>(false)
    val dragonBurstUsed: LiveData<Boolean> = _dragonBurstUsed
    private val _unicornBurstUsed = MutableLiveData<Boolean>(false)
    val unicornBurstUsed: LiveData<Boolean> = _unicornBurstUsed
    private val _attackButtonReady = MutableLiveData<Boolean>(true)
    val attackButtonReady: LiveData<Boolean> = _attackButtonReady

    private lateinit var playerData: PlayerData
    private lateinit var enemyData: EnemyData
    private var timerJob: Job? = null
    private val currentSelection = mutableListOf<GridCell>()
    private var isOperationPhase = false
    private var activeBurst: BurstType? = null
    private var special20Triggered = false
    private var comboCount = 0
    private var heroBurstReserved = false
    private var totalDamageDealt = 0

    data class DamageEvent(val target: DamageTarget, val amount: Int)
    enum class DamageTarget { ENEMY, PLAYER }

    fun canSelectBurst(): Boolean = _attackButtonReady.value == true && _timerRunning.value == false

    fun canUseBurst(type: BurstType): Boolean {
        if (!canSelectBurst()) return false
        if (_burstUsedThisTurn.value == true) return false
        return when (type) {
            BurstType.HERO -> _heroBurstUsed.value == false
            BurstType.DRAGON -> _dragonBurstUsed.value == false
            BurstType.UNICORN -> _unicornBurstUsed.value == false
        }
    }

    fun activateBurst(type: BurstType) {
        if (!canUseBurst(type)) return
        activeBurst = type
        _burstUsedThisTurn.value = true
        when (type) {
            BurstType.HERO -> { _heroBurstUsed.value = true; heroBurstReserved = true }
            BurstType.DRAGON -> _dragonBurstUsed.value = true
            BurstType.UNICORN -> _unicornBurstUsed.value = true
        }
    }

    private fun resetBurstsExceptHero() {
        _dragonBurstUsed.value = false
        _unicornBurstUsed.value = false
    }

    private fun applyHeroBurst() {
        val gridList = _grid.value?.toMutableList() ?: return
        val activeCells = gridList.filter { !it.isCleared }
        val count = (activeCells.size * HERO_BURST_RATE).toInt()
        activeCells.shuffled().take(count).forEach { cell ->
            gridList.find { it.row == cell.row && it.col == cell.col }?.value = 5
        }
        _grid.value = gridList
        heroBurstReserved = false
    }

    fun init(enemy: EnemyData) {
        enemyData = enemy
        playerData = PlayerData()
        _playerHp.value = playerData.maxHp
        _enemyHp.value = enemy.hp
        _turnDamage.value = 0
        totalDamageDealt = 0
        _battleResult.value = BattleResult.ONGOING
        _phaseMessage.value = ""
        _specialMessage.value = null
        _burstUsedThisTurn.value = false
        _heroBurstUsed.value = false
        _dragonBurstUsed.value = false
        _unicornBurstUsed.value = false
        _attackButtonReady.value = true
        activeBurst = null
        special20Triggered = false
        comboCount = 0
        heroBurstReserved = false
        initGrid()
    }

    private fun initGrid() {
        val cells = mutableListOf<GridCell>()
        for (row in 0 until GRID_ROWS)
            for (col in 0 until GRID_COLS)
                cells.add(GridCell(row, col, (1..9).random()))
        _grid.value = cells
        if (heroBurstReserved) applyHeroBurst()
    }

    // ==========================================
    // グリッド操作
    // ==========================================

    fun onCellTouched(row: Int, col: Int) {
        if (!isOperationPhase) return
        val gridList = _grid.value?.toMutableList() ?: return
        val cell = gridList.find { it.row == row && it.col == col } ?: return
        if (cell.isCleared) return

        // すでに選択済みのセルに戻ってきたらそれ以降をundo
        val existingIndex = currentSelection.indexOfFirst { it.row == row && it.col == col }
        if (existingIndex >= 0) {
            val toRemove = currentSelection.subList(existingIndex + 1, currentSelection.size).toList()
            toRemove.forEach { removed ->
                gridList.find { it.row == removed.row && it.col == removed.col }?.isSelected = false
            }
            currentSelection.subList(existingIndex + 1, currentSelection.size).clear()
            _selectedPath.value = currentSelection.toList()
            _grid.value = gridList
            return
        }

        val lastCell = currentSelection.lastOrNull()
        if (lastCell == null) {
            currentSelection.add(cell); cell.isSelected = true
        } else {
            if (!isAdjacentIgnoringEmpty(lastCell, cell, gridList)) return
            currentSelection.add(cell); cell.isSelected = true
        }

        _selectedPath.value = currentSelection.toList()
        _grid.value = gridList
    }

    /**
     * 矩形選択：セルの選択状態をセットするだけ
     * 確定（processSelection）は onFingerReleased で行う
     */
    fun onRectSelection(startRow: Int, startCol: Int, endRow: Int, endCol: Int) {
        if (!isOperationPhase) return
        val gridList = _grid.value?.toMutableList() ?: return

        // 既存の選択をリセット
        currentSelection.forEach { it.isSelected = false }
        currentSelection.clear()

        // 矩形範囲内のセルを選択状態にセット
        for (r in minOf(startRow, endRow)..maxOf(startRow, endRow))
            for (c in minOf(startCol, endCol)..maxOf(startCol, endCol)) {
                val cell = gridList.find { it.row == r && it.col == c }
                if (cell != null && !cell.isCleared) {
                    cell.isSelected = true
                    currentSelection.add(cell)
                }
            }

        _selectedPath.value = currentSelection.toList()
        _grid.value = gridList
        // ここでは processSelection を呼ばない → onFingerReleased で確定
    }

    /**
     * 指を離したタイミングで選択を確定
     * 直線・矩形どちらもここで判定
     */
    fun onFingerReleased() {
        if (!isOperationPhase) return
        val gridList = _grid.value?.toMutableList() ?: return
        processSelection(currentSelection.sumOf { it.value }, gridList)
    }

    private fun processSelection(sum: Int, gridList: MutableList<GridCell>) {
        val count = currentSelection.size
        when {
            sum == TARGET_SUM && count >= 2 -> {
                comboCount++
                val baseDmg = calcDamage(count)
                val multiplied = (baseDmg * comboMultiplier(comboCount)).toInt()
                _turnDamage.value = (_turnDamage.value ?: 0) + multiplied
                clearSelectedCells(gridList)
            }
            sum == SPECIAL_SUM_20 && count >= 2 -> {
                comboCount++
                val multiplied = (SPECIAL_20_DAMAGE * comboMultiplier(comboCount)).toInt()
                _turnDamage.value = (_turnDamage.value ?: 0) + multiplied
                special20Triggered = true
                clearSelectedCells(gridList)
            }
            else -> resetSelection(gridList)
        }
    }

    private fun isAdjacentIgnoringEmpty(from: GridCell, to: GridCell, grid: List<GridCell>): Boolean {
        if (from.col == to.col) {
            val dir = if (to.row > from.row) 1 else -1; var r = from.row + dir
            while (r != to.row) {
                if (grid.find { it.row == r && it.col == from.col }?.isCleared == false) return false
                r += dir
            }
            return true
        }
        if (from.row == to.row) {
            val dir = if (to.col > from.col) 1 else -1; var c = from.col + dir
            while (c != to.col) {
                if (grid.find { it.row == from.row && it.col == c }?.isCleared == false) return false
                c += dir
            }
            return true
        }
        return false
    }

    private fun resetSelection(gridList: MutableList<GridCell>) {
        currentSelection.forEach { it.isSelected = false }
        currentSelection.clear()
        _selectedPath.value = emptyList()
        _grid.value = gridList
    }

    private fun clearSelectedCells(gridList: MutableList<GridCell>) {
        currentSelection.forEach { selected ->
            gridList.find { it.row == selected.row && it.col == selected.col }
                ?.apply { isCleared = true; isSelected = false; value = 0 }
        }
        currentSelection.clear()
        _selectedPath.value = emptyList()
        _grid.value = gridList
    }

    fun onAttackButtonPressed() {
        if (_battleResult.value != BattleResult.ONGOING) return
        if (_timerRunning.value == true) return
        if (_attackButtonReady.value == false) return
        isOperationPhase = true
        _timerRunning.value = true
        _attackButtonReady.value = false
        _turnDamage.value = 0
        _phaseMessage.value = ""
        _specialMessage.value = null
        special20Triggered = false
        comboCount = 0
        initGrid()
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timer.value = OPERATION_TIME_SECONDS
        timerJob = viewModelScope.launch {
            for (i in OPERATION_TIME_SECONDS downTo 0) {
                _timer.value = i; if (i == 0) break; delay(1000L)
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        isOperationPhase = false
        _timerRunning.value = false
        val gridList = _grid.value?.toMutableList() ?: return
        resetSelection(gridList)
        val baseDamage = _turnDamage.value ?: 0

        viewModelScope.launch {
            if (special20Triggered) {
                _specialMessage.value = "ブレイブソード！\n仲間たちのバーストがみなぎった！！"
                delay(2000L)
                _specialMessage.value = null
                resetBurstsExceptHero()
                special20Triggered = false
            }

            val attackCount = if (activeBurst == BurstType.DRAGON) 2 else 1
            val onceHitDamage = baseDamage
            val totalDamage = onceHitDamage * attackCount

            if (activeBurst == BurstType.DRAGON) {
                _phaseMessage.value = "ドラゴンソウル発動！1回目の攻撃！ ${onceHitDamage} ダメージ！"
                _enemyHp.value = maxOf(0, (_enemyHp.value ?: 0) - onceHitDamage)
                _damageEvent.value = DamageEvent(DamageTarget.ENEMY, onceHitDamage)
                delay(100); _damageEvent.value = null
                delay(1200L)

                if ((_enemyHp.value ?: 0) > 0) {
                    _phaseMessage.value = "2回目の攻撃！ ${onceHitDamage} ダメージ！"
                    _enemyHp.value = maxOf(0, (_enemyHp.value ?: 0) - onceHitDamage)
                    _damageEvent.value = DamageEvent(DamageTarget.ENEMY, onceHitDamage)
                    delay(100); _damageEvent.value = null
                    delay(1200L)
                }
            } else {
                _phaseMessage.value = "プレイヤーの攻撃！ ${totalDamage} ダメージ！"
                _enemyHp.value = maxOf(0, (_enemyHp.value ?: 0) - totalDamage)
                _damageEvent.value = DamageEvent(DamageTarget.ENEMY, totalDamage)
                delay(100); _damageEvent.value = null
                delay(1200L)
            }

            totalDamageDealt += totalDamage

            if (activeBurst == BurstType.UNICORN && totalDamage > 0) {
                val healAmount = totalDamage * UNICORN_HEAL_RATE
                playerData = playerData.copy(currentHp = minOf(playerData.currentHp + healAmount, playerData.maxHp))
                _playerHp.value = playerData.currentHp
                _phaseMessage.value = "ホーリーホーン発動！ ${healAmount} HP回復！"
                delay(1200L)
            }

            if (activeBurst != BurstType.HERO) activeBurst = null
            _burstUsedThisTurn.value = false

            if ((_enemyHp.value ?: 0) <= 0) {
                _phaseMessage.value = "勝利！ ${enemyData.name} を倒した！"
                _battleResult.value = BattleResult.PLAYER_WIN
                return@launch
            }

            delay(600L)
            _phaseMessage.value = "${enemyData.name} の攻撃！ ${enemyData.attackDamage} ダメージ！"
            playerData = playerData.copy(currentHp = maxOf(0, playerData.currentHp - enemyData.attackDamage))
            _playerHp.value = playerData.currentHp
            _damageEvent.value = DamageEvent(DamageTarget.PLAYER, enemyData.attackDamage)
            delay(100); _damageEvent.value = null
            delay(1200L)

            if (playerData.currentHp <= 0) {
                _phaseMessage.value = "ゲームオーバー…"
                _battleResult.value = BattleResult.PLAYER_LOSE
                return@launch
            }

            _phaseMessage.value = ""
            delay(1000L)
            _attackButtonReady.value = true
        }
    }

    fun getEnemyData() = if (::enemyData.isInitialized) enemyData else null
    fun getPlayerMaxHp() = if (::playerData.isInitialized) playerData.maxHp else 10_000
    fun getTotalDamageDealt(): Int = totalDamageDealt

    override fun onCleared() { super.onCleared(); timerJob?.cancel() }
}
