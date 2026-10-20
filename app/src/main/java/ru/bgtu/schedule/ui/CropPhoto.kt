package ru.bgtu.schedule.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CropPhotoDialog(source: Bitmap, onCancel: () -> Unit, onSave: (Bitmap) -> Unit) {
    val image = remember(source) { source.asImageBitmap() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var frame by remember { mutableStateOf(IntSize.Zero) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = LocalBottomInset.current + 16.dp),
        ) {
            Text(S.cropTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(S.cropHint, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val crop = min(constraints.maxWidth, constraints.maxHeight) * 0.86f
                val base = max(crop / source.width, crop / source.height)
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { frame = it.size }
                        .pointerInput(source, crop) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                offset = clampOffset(offset + pan, nextScale, base, crop, source.width, source.height)
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val drawW = source.width * base * scale
                        val drawH = source.height * base * scale
                        val left = (size.width - drawW) / 2f + offset.x
                        val top = (size.height - drawH) / 2f + offset.y
                        drawImage(
                            image,
                            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                            dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
                        )
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val half = crop / 2f
                        val dim = androidx.compose.ui.graphics.Color(0xB30E1422)
                        drawRect(dim, size = Size(size.width, (cy - half).coerceAtLeast(0f)))
                        drawRect(dim, topLeft = Offset(0f, cy + half), size = Size(size.width, (size.height - cy - half).coerceAtLeast(0f)))
                        drawRect(dim, topLeft = Offset(0f, cy - half), size = Size((cx - half).coerceAtLeast(0f), crop))
                        drawRect(dim, topLeft = Offset(cx + half, cy - half), size = Size((size.width - cx - half).coerceAtLeast(0f), crop))
                        drawRoundRect(
                            color = BrandBlue,
                            topLeft = Offset(cx - half, cy - half),
                            size = Size(crop, crop),
                            cornerRadius = CornerRadius(28f, 28f),
                            style = Stroke(width = 4f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = scale,
                onValueChange = { value ->
                    val crop = min(frame.width, frame.height) * 0.86f
                    val base = if (crop <= 0f) 1f else max(crop / source.width, crop / source.height)
                    scale = value
                    offset = clampOffset(offset, value, base, crop, source.width, source.height)
                },
                valueRange = 1f..4f,
                colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue, inactiveTrackColor = Line),
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(S.cancel, color = Muted)
                }
                Button(
                    onClick = {
                        val crop = min(frame.width, frame.height) * 0.86f
                        val base = max(crop / source.width, crop / source.height)
                        onSave(cropSquare(source, scale, offset, base, crop))
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                ) { Text(S.done) }
            }
        }
    }
}

private fun clampOffset(offset: Offset, scale: Float, base: Float, crop: Float, width: Int, height: Int): Offset {
    val maxX = ((width * base * scale - crop) / 2f).coerceAtLeast(0f)
    val maxY = ((height * base * scale - crop) / 2f).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

private fun cropSquare(source: Bitmap, scale: Float, offset: Offset, base: Float, crop: Float): Bitmap {
    val out = 720
    val result = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val srcSize = (crop / (base * scale)).coerceAtLeast(1f)
    val left = ((source.width - srcSize) / 2f - offset.x / (base * scale)).coerceIn(0f, (source.width - srcSize).coerceAtLeast(0f))
    val top = ((source.height - srcSize) / 2f - offset.y / (base * scale)).coerceIn(0f, (source.height - srcSize).coerceAtLeast(0f))
    val src = Rect(
        left.roundToInt(),
        top.roundToInt(),
        (left + srcSize).roundToInt().coerceAtMost(source.width),
        (top + srcSize).roundToInt().coerceAtMost(source.height),
    )
    canvas.drawBitmap(source, src, Rect(0, 0, out, out), Paint(Paint.FILTER_BITMAP_FLAG))
    return result
}
