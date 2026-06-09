package com.example.paintapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.applyCanvas

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaintApp()
        }
    }
}

/**
 * Represents a single line segment
 */
data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun PaintApp() {

    val context = LocalContext.current

    // Current selected brush color
    var currentColor by remember {
        mutableStateOf(Color.Black)
    }

    // Brush thickness
    var brushSize by remember {
        mutableStateOf(10f)
    }

    // Eraser mode
    var isEraser by remember {
        mutableStateOf(false)
    }

    // Stores all lines drawn on screen
    val lines = remember {
        mutableStateListOf<Line>()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        //--------------------------------------------------
        // TOP TOOLBAR
        //--------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            // Color picker row
            ColorPicker { selectedColor ->
                currentColor = selectedColor
                isEraser = false
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brush size selector
            BrushSizeSelector(
                currentSize = brushSize
            ) {
                brushSize = it
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Eraser
                Button(
                    onClick = {
                        isEraser = true
                    }
                ) {
                    Text("Eraser")
                }

                // Clear canvas
                Button(
                    onClick = {
                        lines.clear()
                    }
                ) {
                    Text("Reset")
                }

                // Save image
                Button(
                    onClick = {
                        saveDrawingToGallery(
                            context = context,
                            lines = lines
                        )
                    }
                ) {
                    Text("Save")
                }
            }
        }

        //--------------------------------------------------
        // DRAWING CANVAS
        //--------------------------------------------------

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {

                    detectDragGestures { change, dragAmount ->

                        val startPoint =
                            change.position - dragAmount

                        val endPoint =
                            change.position

                        val line = Line(
                            start = startPoint,
                            end = endPoint,

                            // White color acts as eraser
                            color =
                                if (isEraser)
                                    Color.White
                                else
                                    currentColor,

                            strokeWidth = brushSize
                        )

                        lines.add(line)
                    }
                }
        ) {

            // Draw every line stored in list
            lines.forEach { line ->

                drawLine(
                    color = line.color,
                    start = line.start,
                    end = line.end,
                    strokeWidth = line.strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ColorPicker(
    onColorSelected: (Color) -> Unit
) {

    val context = LocalContext.current

    val colors = listOf(
        Pair(Color.Red, "Red"),
        Pair(Color.Green, "Green"),
        Pair(Color.Blue, "Blue"),
        Pair(Color.Black, "Black")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        colors.forEach { (color, label) ->

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
                    .clickable {

                        onColorSelected(color)

                        Toast
                            .makeText(
                                context,
                                label,
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
            )
        }
    }
}

@Composable
fun BrushSizeSelector(
    currentSize: Float,
    onSizeChange: (Float) -> Unit
) {

    var text by remember {
        mutableStateOf(currentSize.toInt().toString())
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        BasicTextField(
            value = text,

            onValueChange = {

                text = it

                val size =
                    it.toFloatOrNull()

                if (size != null) {
                    onSizeChange(size)
                }
            },

            textStyle = TextStyle(
                fontSize = 18.sp
            ),

            modifier = Modifier
                .width(70.dp)
                .background(
                    Color.LightGray,
                    CircleShape
                )
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text("px")
    }
}

/**
 * Save drawing into Gallery
 */
fun saveDrawingToGallery(
    context: Context,
    lines: List<Line>
) {

    // Create blank bitmap
    val bitmap =
        Bitmap.createBitmap(
            1080,
            1920,
            Bitmap.Config.ARGB_8888
        )

    // Draw all lines on bitmap
    bitmap.applyCanvas {

        drawColor(
            android.graphics.Color.WHITE
        )

        lines.forEach { line ->

            val paint =
                android.graphics.Paint().apply {

                    color =
                        line.color.toArgb()

                    strokeWidth =
                        line.strokeWidth

                    style =
                        android.graphics.Paint.Style.STROKE

                    strokeCap =
                        android.graphics.Paint.Cap.ROUND
                }

            drawLine(
                line.start.x,
                line.start.y,
                line.end.x,
                line.end.y,
                paint
            )
        }
    }

    val values = ContentValues().apply {

        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            "Drawing_${System.currentTimeMillis()}.png"
        )

        put(
            MediaStore.Images.Media.MIME_TYPE,
            "image/png"
        )

        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Pictures/PaintApp"
        )
    }

    val resolver =
        context.contentResolver

    val uri =
        resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

    if (uri != null) {

        resolver
            .openOutputStream(uri)
            ?.use { stream ->

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )
            }

        Toast.makeText(
            context,
            "Saved Successfully",
            Toast.LENGTH_SHORT
        ).show()

    } else {

        Toast.makeText(
            context,
            "Save Failed",
            Toast.LENGTH_SHORT
        ).show()
    }
}