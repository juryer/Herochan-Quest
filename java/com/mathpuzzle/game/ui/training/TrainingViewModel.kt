package com.mathpuzzle.game.ui.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathpuzzle.game.model.GridCell
import kotlinx.coroutines.*

class TrainingViewModel : ViewModel() {

    companion object {
        const val GRID_ROWS = 13
        const val GRID_COLS = 9
        const val TRAINING_TIME_SECONDS = 99
        const val TARGET_SUM = 10
        const val SPECIAL_SUM_20 = 20
        const val SPECIAL_SUM_30 = 30

        fun calcScore(cellCount: Int): Int =
            if (cellCount <= 3) 200 else 200 + (cellCount - 3) * 50
    }

    enum class BurstType { HERO, DRAGON, UNICORN }

    private val _grid = MutableLiveData<List<GridCell>>()
    val grid: LiveData<List<GridCell>> = _grid
    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score
    private val _timer = MutableLiveData<Int>(TRAINING_TIME_SECONDS)
    val timer: LiveData<Int> = _timer
    private val _isRunning = MutableLiveData<Boolean>(false)
    val isRunning: LiveData<Boolean> = _isRunning
    private val _isFinished = MutableLiveData<Boolean>(false)
    val isFinished: LiveData<Boolean> = _isFinished
    private val _selectedPath = MutableLiveData<List<GridCell>>(emptyList())
    val selectedPath: LiveData<List<GridCell>> = _selectedPath
    private val _heroBurstUsed = MutableLiveData<Boolean>(false)
    val heroBurstUsed: LiveData<Boolean> = _heroBurstUsed
    private val _dragonBurstUsed = MutableLiveData<Boolean>(false)
    val dragonBurstUsed: LiveData<Boolean> = _dragonBurstUsed
    private val _unicornBurstUsed = MutableLiveData<Boolean>(false)
    val unicornBurstUsed: LiveData<Boolean> = _unicornBurstUsed
    private val _dragonBurstActive = MutableLiveData<Boolean>(false)
    val dragonBurstActive: LiveData<Boolean> = _dragonBurstActive

    private var timerJob: Job? = null
    private val currentSelection = mutableListOf<GridCell>()

    fun init() {
        _score.value = 0; _timer.value = TRAINING_TIME_SECONDS
        _isRunning.value = false; _isFinished.value = false
        _heroBurstUsed.value = false; _dragonBurstUsed.value = false
        _unicornBurstUsed.value = false; _dragonBurstActive.value = false
        currentSelection.clear(); initGrid()
    }

    private fun initGrid() {
        val cells = mutableListOf<GridCell>()
        for (row in 0 until GRID_ROWS)
            for (col in 0 until GRID_COLS)
                cells.add(GridCell(row, col, (1..9).random()))
        _grid.value = cells
    }

    fun startTraining() {
        if (_isRunning.value == true) return
        _isRunning.value = true; _isFinished.value = false; _score.value = 0
        _heroBurstUsed.value = false; _dragonBurstUsed.value = false
        _unicornBurstUsed.value = false; _dragonBurstActive.value = false
        initGrid(); startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timer.value = TRAINING_TIME_SECONDS
        timerJob = viewModelScope.launch {
            for (i in TRAINING_TIME_SECONDS downTo 0) {
                _timer.value = i; if (i == 0) break; delay(1000L)
            }
            _isRunning.value = false; _isFinished.value = true
            val gridList = _grid.value?.toMutableList() ?: return@launch
            resetSelection(gridList)
        }
    }

    fun canUseBurst(type: BurstType): Boolean {
        if (_isRunning.value != true) return false
        return when (type) {
            BurstType.HERO -> _heroBurstUsed.value == false
            BurstType.DRAGON -> _dragonBurstUsed.value == false
            BurstType.UNICORN -> _unicornBurstUsed.value == false
        }
    }

    fun activateBurst(type: BurstType) {
        if (!canUseBurst(type)) return
        when (type) {
            BurstType.HERO -> {
                _heroBurstUsed.value = true
                initGrid(); currentSelection.clear(); _selectedPath.value = emptyList()
            }
            BurstType.DRAGON -> { _dragonBurstUsed.value = true; _dragonBurstActive.value = true }
            BurstType.UNICORN -> {
                _unicornBurstUsed.value = true
                val gridList = _grid.value?.toMutableList() ?: return
                gridList.filter { !it.isCleared && !it.isSelected }.shuffled().take(5).forEach { cell ->
                    gridList.find { it.row == cell.row && it.col == cell.col }
                        ?.apply { isCleared = true; value = 0 }
                }
                _grid.value = gridList
            }
        }
    }

    // ==========================================
    // グリッド操作
    // ==========================================

    fun onCellTouched(row: Int, col: Int) {
        if (_isRunning.value != true) return
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
        if (_isRunning.value != true) return
        val gridList = _grid.value?.toMutableList() ?: return

        currentSelection.forEach { it.isSelected = false }
        currentSelection.clear()

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
        if (_isRunning.value != true) return
        val gridList = _grid.value?.toMutableList() ?: return
        processSelection(currentSelection.sumOf { it.value }, gridList)
    }

    private fun processSelection(sum: Int, gridList: MutableList<GridCell>) {
        val count = currentSelection.size
        val multiplier = if (_dragonBurstActive.value == true) { _dragonBurstActive.value = false; 3 } else 1
        when {
            sum == TARGET_SUM && count >= 2 -> {
                _score.value = (_score.value ?: 0) + calcScore(count) * multiplier
                clearSelectedCells(gridList)
            }
            sum == SPECIAL_SUM_20 && count >= 2 -> {
                _score.value = (_score.value ?: 0) + 1000 * multiplier
                clearSelectedCells(gridList)
            }
            sum == SPECIAL_SUM_30 && count >= 2 -> {
                _score.value = (_score.value ?: 0) + 1500 * multiplier
                clearSelectedCells(gridList)
                initGrid()
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
        currentSelection.clear(); _selectedPath.value = emptyList(); _grid.value = gridList
    }

    private fun clearSelectedCells(gridList: MutableList<GridCell>) {
        currentSelection.forEach { selected ->
            gridList.find { it.row == selected.row && it.col == selected.col }
                ?.apply { isCleared = true; isSelected = false; value = 0 }
        }
        currentSelection.clear(); _selectedPath.value = emptyList(); _grid.value = gridList
    }

    fun getFinalScore(): Int = _score.value ?: 0
    override fun onCleared() { super.onCleared(); timerJob?.cancel() }
}
