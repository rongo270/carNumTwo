// English comments only inside code
package com.rongo.carnumtwo.feature.game

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rongo.carnumtwo.R
import com.rongo.carnumtwo.core.storage.SettingsStorage
import com.rongo.carnumtwo.core.ui.BaseLocalizedActivity
import com.rongo.carnumtwo.feature.game.engine.GameController
import com.rongo.carnumtwo.feature.game.model.GameState
import com.rongo.carnumtwo.feature.game.render.GameRenderer
import kotlin.math.max
import kotlin.math.min

class GameActivity : BaseLocalizedActivity() {

    private lateinit var root: View
    private lateinit var grid: GridLayout
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnPause: ImageButton

    private lateinit var controller: GameController
    private lateinit var renderer: GameRenderer
    private lateinit var state: GameState

    private lateinit var cells: Array<Array<ImageView>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        root = findViewById(R.id.game_root)
        grid = findViewById(R.id.game_grid)
        btnLeft = findViewById(R.id.btn_left)
        btnRight = findViewById(R.id.btn_right)
        btnPause = findViewById(R.id.btn_pause)

        applyBottomInsetsToRoot()

        val settings = SettingsStorage(this).load()
        val cols = settings.gridX
        val rows = settings.gridY

        state = GameState(
            cols = cols,
            rows = rows,
            playerCol = cols / 2,
            paused = false
        )

        setupGrid(cols, rows)

        renderer = GameRenderer(cells, rows, cols)
        controller = GameController(state, renderer)

        // Render once now (will be re-rendered after sizing too)
        controller.init()

        btnLeft.setOnClickListener { controller.moveLeft() }
        btnRight.setOnClickListener { controller.moveRight() }

        btnPause.setOnClickListener {
            controller.togglePause()
            btnPause.setImageResource(if (controller.isPaused()) R.drawable.ic_play else R.drawable.ic_pause)
        }
    }

    private fun applyBottomInsetsToRoot() {
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseBottom + bars.bottom)
            insets
        }
    }

    private fun setupGrid(cols: Int, rows: Int) {
        grid.removeAllViews()
        grid.columnCount = cols
        grid.rowCount = rows

        cells = Array(rows) { r ->
            Array(cols) { c ->
                val cell = AppCompatImageView(this)
                cell.scaleType = ImageView.ScaleType.FIT_CENTER
                cell.setBackgroundResource(R.drawable.bg_cell)
                grid.addView(cell)
                cell
            }
        }

        // Size cells AFTER grid has a real width/height
        grid.post {
            val margin = dpToPx(6)

            // Compute cell size INCLUDING margins so bottom row never gets clipped
            val availableW = max(0, grid.width - (cols * margin * 2))
            val availableH = max(0, grid.height - (rows * margin * 2))

            val cellSizeByW = if (cols > 0) availableW / cols else 0
            val cellSizeByH = if (rows > 0) availableH / rows else 0
            val cellSize = max(1, min(cellSizeByW, cellSizeByH))

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val lp = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                        rowSpec = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                        setMargins(margin, margin, margin, margin)
                    }
                    cells[r][c].layoutParams = lp
                }
            }

            // Center the content inside the grid
            val contentW = cols * cellSize + cols * margin * 2
            val contentH = rows * cellSize + rows * margin * 2

            val padX = max(0, (grid.width - contentW) / 2)
            val padY = max(0, (grid.height - contentH) / 2)

            grid.setPadding(padX, padY, padX, padY)
            grid.clipToPadding = false

            // Re-render after sizing to guarantee spaceship visibility
            renderer.render(state)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
