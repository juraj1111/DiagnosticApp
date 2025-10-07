package com.example.diagnosticapp.viewmodel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.diagnosticapp.data.repository.PatientRepository
import java.io.File
class WritingViewModel(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val path = Path()
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val points = mutableListOf<String>() // for .svc data
    private var outputFilePath: String? = null   // Track saved file

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val timestamp = event.eventTime
        val isTouching = if (event.action != MotionEvent.ACTION_UP) 1 else 0
        val pressure = event.pressure
        val azimuth = event.getOrientation(0)  // orientation in radians
        val altitude = event.getAxisValue(MotionEvent.AXIS_TILT)  // approx, can vary by device

        if (event.action == MotionEvent.ACTION_DOWN) {
            path.moveTo(x, y)
        } else if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            path.lineTo(x, y)
            invalidate()
        }

        // Save data
        points.add("$x,$y,$timestamp,$isTouching,$pressure,$azimuth,$altitude")

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    fun clear() {
        path.reset() // Erases all drawing paths
        points.clear();
        invalidate() // Requests redraw
    }

    fun exportToFile(context: Context) : String{
        val filename = "drawing_${System.currentTimeMillis()}.svc"
        val file = File(context.getExternalFilesDir(null), filename)

        val content = points.joinToString("\n")
        file.writeText(points.joinToString("\n"))
        outputFilePath = file.absolutePath

        Log.d("DrawingView", "File saved at: ${file.absolutePath}")
        Log.d("DrawingView", "File content:\n$content")


        return outputFilePath!!
    }

    fun saveWriting(taskId: String?) {
        Log.e("DrawingView", "Trying to save writing for $taskId")

        if (taskId != null) {
            if (outputFilePath.isNullOrEmpty()) {
                // Auto-export if not done yet
                exportToFile(context)
            }

            if (!outputFilePath.isNullOrEmpty()) {
                Log.e("DrawingView", "Saving .svc file to repository")
                PatientRepository.updateWritingTask(taskId, outputFilePath!!)
            } else {
                Log.e("DrawingView", "Failed to generate outputFilePath")
            }
        } else {
            Log.e("DrawingView", "Task ID is null. Cannot save writing.")
        }
    }
}
