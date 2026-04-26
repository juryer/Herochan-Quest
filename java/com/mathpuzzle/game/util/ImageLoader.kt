package com.mathpuzzle.game.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.mathpuzzle.game.R

object ImageLoader {

    object GameImages {
        val BG_MAIN_MENU: Int     = 0 // TODO: R.drawable.bg_main_menu
        val BG_DUNGEON: Int       = 0 // TODO: R.drawable.bg_dungeon_menu
        val ENEMY_GOBLIN: Int     = 0 // TODO: R.drawable.enemy_goblin
        val ENEMY_ORC: Int        = 0 // TODO: R.drawable.enemy_orc
        val ENEMY_DRAGON: Int     = 0 // TODO: R.drawable.enemy_dragon
        val ENEMY_DEMON_LORD: Int = 0 // TODO: R.drawable.enemy_demon_lord
    }

    /**
     * トレーニングモードの隠し画像リスト
     * ==========================================
     * 【追加方法】
     * 1. res/drawable/ に画像を置く（推奨サイズ: 900×1000px）
     * 2. このリストに R.drawable.xxx を追加するだけ！
     * ==========================================
     */
    object HiddenImages {
        val list: List<Int> = listOf(
            // TODO: ここに画像を追加してください
            R.drawable.training_secret_1,
            R.drawable.training_secret_2,
            // R.drawable.training_secret_3,
        )

        fun getRandom(): Int? = if (list.isEmpty()) null else list.random()
    }

    fun loadInto(context: Context, resId: Int, imageView: ImageView, placeholderColor: Int = 0xFF888888.toInt()) {
        if (resId != 0) {
            imageView.setImageDrawable(ContextCompat.getDrawable(context, resId))
        } else {
            imageView.setBackgroundColor(placeholderColor)
            imageView.setImageDrawable(null)
        }
    }

    fun getDrawable(context: Context, resId: Int): Drawable? {
        return if (resId != 0) ContextCompat.getDrawable(context, resId) else null
    }
}
