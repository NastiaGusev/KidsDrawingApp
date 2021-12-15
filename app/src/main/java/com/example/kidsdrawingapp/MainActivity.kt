package com.example.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.get
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mainIMGBrushSize: ImageButton? = null
    private var currentPaintIMB: ImageButton? = null
    private var mainLAYColors: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.main_DrawingView)
        drawingView?.setSizeForBrush(20.toFloat())
        mainIMGBrushSize = findViewById(R.id.main_IMG_BrushSize)
        mainIMGBrushSize?.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        mainLAYColors = findViewById(R.id.main_LAY_Colors)
        currentPaintIMB = mainLAYColors!![0] as ImageButton
        currentPaintIMB!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size")

        val smallButton: ImageButton = brushDialog.findViewById(R.id.dialog_smallBrush)
        smallButton.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumButton: ImageButton = brushDialog.findViewById(R.id.dialog_mediumBrush)
        mediumButton.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeButton: ImageButton = brushDialog.findViewById(R.id.dialog_largeBrush)
        largeButton.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view != currentPaintIMB) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            currentPaintIMB?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            currentPaintIMB = imageButton
        }

    }
}