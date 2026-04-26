package com.mathpuzzle.game.ui.battle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mathpuzzle.game.model.GridCell

class NumberGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onCellTouched: ((row: Int, col: Int) -> Unit)? = null
    var onFingerReleased: (() -> Unit)? = null
    var onRectSelection: ((startRow: Int, startCol: Int, endRow: Int, endCol: Int) -> Unit)? = null

    var gridRows: Int = BattleViewModel.GRID_ROWS
    var gridCols: Int = BattleViewModel.GRID_COLS

    private val rows get() = gridRows
    private val cols get() = gridCols

    private var cells: List<GridCell> = emptyList()
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private var touchStartRow = -1
    private var touchStartCol = -1
    private var touchCurrentRow = -1
    private var touchCurrentCol = -1
    private var isDiagonalDrag = false

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDEEFF"); style = Paint.Style.FILL
    }

    // 選択中・プレビュー全て同じ薄いオレンジで統一
    private val selectedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5D08A"); style = Paint.Style.FILL
    }

    private val clearedCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT; style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AABBCC"); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#223344"); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    // 選択セルの数字も通常と同じ色で統一
    private val selectedNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#223344"); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    fun setCells(newCells: List<GridCell>) { cells = newCells; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = minOf(w.toFloat() / cols, h.toFloat() / rows)
        offsetX = (w - cellSize * cols) / 2f
        offsetY = (h - cellSize * rows) / 2f
        numberPaint.textSize = cellSize * 0.45f
        selectedNumberPaint.textSize = cellSize * 0.45f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty()) return
        for (cell in cells) drawCell(canvas, cell)
        if (isDiagonalDrag && touchStartRow >= 0 && touchCurrentRow >= 0) {
            drawRectPreview(canvas)
        }
    }

    private fun drawCell(canvas: Canvas, cell: GridCell) {
        val left = offsetX + cell.col * cellSize + 2f
        val top = offsetY + cell.row * cellSize + 2f
        val right = left + cellSize - 4f
        val bottom = top + cellSize - 4f
        val radius = cellSize * 0.12f
        when {
            cell.isCleared -> canvas.drawRoundRect(left, top, right, bottom, radius, radius, clearedCellPaint)
            cell.isSelected -> {
                // 薄いオレンジで統一
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, selectedCellPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, cellBorderPaint)
                val cx = left + (right - left) / 2f
                val cy = top + (bottom - top) / 2f - (selectedNumberPaint.descent() + selectedNumberPaint.ascent()) / 2f
                canvas.drawText(cell.value.toString(), cx, cy, selectedNumberPaint)
            }
            else -> {
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, cellPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, cellBorderPaint)
                val cx = left + (right - left) / 2f
                val cy = top + (bottom - top) / 2f - (numberPaint.descent() + numberPaint.ascent()) / 2f
                canvas.drawText(cell.value.toString(), cx, cy, numberPaint)
            }
        }
    }

    private fun drawRectPreview(canvas: Canvas) {
        val minRow = minOf(touchStartRow, touchCurrentRow)
        val maxRow = maxOf(touchStartRow, touchCurrentRow)
        val minCol = minOf(touchStartCol, touchCurrentCol)
        val maxCol = maxOf(touchStartCol, touchCurrentCol)
        for (r in minRow..maxRow) {
            for (c in minCol..maxCol) {
                val cell = cells.find { it.row == r && it.col == c }
                if (cell != null && !cell.isCleared && !cell.isSelected) {
                    val left = offsetX + c * cellSize + 2f
                    val top = offsetY + r * cellSize + 2f
                    val right = left + cellSize - 4f
                    val bottom = top + cellSize - 4f
                    val radius = cellSize * 0.12f
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, selectedCellPaint)
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, cellBorderPaint)
                    val cx = left + (right - left) / 2f
                    val cy = top + (bottom - top) / 2f - (numberPaint.descent() + numberPaint.ascent()) / 2f
                    canvas.drawText(cell.value.toString(), cx, cy, numberPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val col = ((event.x - offsetX) / cellSize).toInt().coerceIn(0, cols - 1)
        val row = ((event.y - offsetY) / cellSize).toInt().coerceIn(0, rows - 1)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartRow = row; touchStartCol = col
                touchCurrentRow = row; touchCurrentCol = col
                isDiagonalDrag = false
                onCellTouched?.invoke(row, col)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (row != touchCurrentRow || col != touchCurrentCol) {
                    touchCurrentRow = row; touchCurrentCol = col
                    val dr = touchCurrentRow - touchStartRow
                    val dc = touchCurrentCol - touchStartCol
                    if (dr != 0 && dc != 0) isDiagonalDrag = true
                    if (!isDiagonalDrag) onCellTouched?.invoke(row, col)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDiagonalDrag) {
                    onRectSelection?.invoke(touchStartRow, touchStartCol, touchCurrentRow, touchCurrentCol)
                    onFingerReleased?.invoke()
                } else {
                    onFingerReleased?.invoke()
                }
                touchStartRow = -1; touchStartCol = -1
                touchCurrentRow = -1; touchCurrentCol = -1
                isDiagonalDrag = false
                invalidate()
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width * rows / cols)
    }
}
