package com.example.moneychangersdatacollectionsystem.ui.screens.collection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ManualCropScreen — Interactive photo cropper.
 *
 * FIXES APPLIED:
 *  #1  Unused imports removed: android.graphics.Matrix, kotlin.math.abs.
 *      Both were imported but never referenced anywhere in this file.
 *
 *  #2  saveBitmapToInternalStorage() — try-catch with corrupt file cleanup.
 *      If bitmap.compress() throws (e.g., out of disk space), the previous
 *      version left a 0-byte corrupt JPEG on disk with no error propagated.
 *      Now the corrupt file is deleted before re-throwing so the caller can
 *      surface a meaningful error to the user.
 */
@Composable
fun ManualCropScreen(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(imageUri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                stream?.close()

                val reqSize = 2048
                var inSampleSize = 1
                if (options.outHeight > reqSize || options.outWidth > reqSize) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= reqSize && halfWidth / inSampleSize >= reqSize) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                val freshStream = context.contentResolver.openInputStream(imageUri)
                bitmap = BitmapFactory.decodeStream(freshStream, null, decodeOptions)
                freshStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading || bitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Loading image...", color = Color.White)
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { containerSize = it.size }
    ) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()

        LaunchedEffect(containerSize) {
            if (containerSize.width > 0 && cropRect == Rect.Zero) {
                val initialWidth = maxW * 0.8f
                val initialHeight = initialWidth / 1.586f
                val left = (maxW - initialWidth) / 2
                val top = (maxH - initialHeight) / 2
                cropRect = Rect(left, top, left + initialWidth, top + initialHeight)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            bitmap?.let { b ->
                androidx.compose.foundation.Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val touchPos = change.position
                            val handleSize = 40.dp.toPx()
                            val minSize = 50.dp.toPx()

                            val isTopLeft = (touchPos - cropRect.topLeft).getDistance() < handleSize
                            val isTopRight = (touchPos - cropRect.topRight).getDistance() < handleSize
                            val isBottomLeft = (touchPos - cropRect.bottomLeft).getDistance() < handleSize
                            val isBottomRight = (touchPos - cropRect.bottomRight).getDistance() < handleSize

                            cropRect = when {
                                isTopLeft -> Rect(
                                    (cropRect.left + dragAmount.x).coerceIn(0f, cropRect.right - minSize),
                                    (cropRect.top + dragAmount.y).coerceIn(0f, cropRect.bottom - minSize),
                                    cropRect.right, cropRect.bottom
                                )
                                isTopRight -> Rect(
                                    cropRect.left,
                                    (cropRect.top + dragAmount.y).coerceIn(0f, cropRect.bottom - minSize),
                                    (cropRect.right + dragAmount.x).coerceIn(cropRect.left + minSize, maxW),
                                    cropRect.bottom
                                )
                                isBottomLeft -> Rect(
                                    (cropRect.left + dragAmount.x).coerceIn(0f, cropRect.right - minSize),
                                    cropRect.top, cropRect.right,
                                    (cropRect.bottom + dragAmount.y).coerceIn(cropRect.top + minSize, maxH)
                                )
                                isBottomRight -> Rect(
                                    cropRect.left, cropRect.top,
                                    (cropRect.right + dragAmount.x).coerceIn(cropRect.left + minSize, maxW),
                                    (cropRect.bottom + dragAmount.y).coerceIn(cropRect.top + minSize, maxH)
                                )
                                cropRect.contains(touchPos) -> {
                                    val newLeft = (cropRect.left + dragAmount.x).coerceIn(0f, maxW - cropRect.width)
                                    val newTop = (cropRect.top + dragAmount.y).coerceIn(0f, maxH - cropRect.height)
                                    Rect(newLeft, newTop, newLeft + cropRect.width, newTop + cropRect.height)
                                }
                                else -> cropRect
                            }
                        }
                    }
            ) {
                val path = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRect(cropRect)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(path, Color.Black.copy(alpha = 0.6f))

                drawRect(
                    color = Color.Cyan,
                    topLeft = cropRect.topLeft,
                    size = cropRect.size,
                    style = Stroke(width = 2.dp.toPx())
                )

                val handleRadius = 6.dp.toPx()
                listOf(cropRect.topLeft, cropRect.topRight, cropRect.bottomLeft, cropRect.bottomRight)
                    .forEach { drawCircle(Color.Cyan, handleRadius, it) }

                val thirdW = cropRect.width / 3
                val thirdH = cropRect.height / 3
                for (i in 1..2) {
                    drawLine(Color.White.copy(0.3f), Offset(cropRect.left + thirdW * i, cropRect.top), Offset(cropRect.left + thirdW * i, cropRect.bottom), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(0.3f), Offset(cropRect.left, cropRect.top + thirdH * i), Offset(cropRect.right, cropRect.top + thirdH * i), strokeWidth = 1.dp.toPx())
                }
            }
        }

        Text(
            "Drag Corners to Resize • Drag Box to Move",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledIconButton(
                onClick = onCancel,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            FilledIconButton(
                onClick = {
                    bitmap?.let { b ->
                        try {
                            val cropped = performCrop(b, cropRect, containerSize)
                            val uri = saveBitmapToInternalStorage(context, cropped)
                            onCropSuccess(uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Crop failed — user stays on crop screen, can retry
                        }
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF43A047)),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

private fun performCrop(bitmap: Bitmap, cropRect: Rect, containerSize: IntSize): Bitmap {
    val viewW = containerSize.width.toFloat()
    val viewH = containerSize.height.toFloat()
    val bitmapW = bitmap.width.toFloat()
    val bitmapH = bitmap.height.toFloat()

    val scale = minOf(viewW / bitmapW, viewH / bitmapH)
    val actualImgW = bitmapW * scale
    val actualImgH = bitmapH * scale
    val imgLeftOffset = (viewW - actualImgW) / 2
    val imgTopOffset = (viewH - actualImgH) / 2

    val pixelLeft = ((cropRect.left - imgLeftOffset) / scale).toInt().coerceIn(0, bitmap.width)
    val pixelTop = ((cropRect.top - imgTopOffset) / scale).toInt().coerceIn(0, bitmap.height)
    val pixelRight = ((cropRect.right - imgLeftOffset) / scale).toInt().coerceIn(0, bitmap.width)
    val pixelBottom = ((cropRect.bottom - imgTopOffset) / scale).toInt().coerceIn(0, bitmap.height)

    val width = (pixelRight - pixelLeft).coerceAtLeast(1)
    val height = (pixelBottom - pixelTop).coerceAtLeast(1)
    val safeWidth = if (pixelLeft + width > bitmap.width) bitmap.width - pixelLeft else width
    val safeHeight = if (pixelTop + height > bitmap.height) bitmap.height - pixelTop else height

    return Bitmap.createBitmap(bitmap, pixelLeft, pixelTop, safeWidth, safeHeight)
}

/**
 * FIX #2: Wrapped in try-catch with corrupt file cleanup.
 *
 * If bitmap.compress() throws (out of disk space, I/O error), the previous
 * version left a 0-byte corrupt JPEG on disk with no error propagated.
 * Now the file is deleted before re-throwing so the ViewModel/caller can
 * handle the failure appropriately (e.g., show a retry button).
 *
 * Saves to context.filesDir/collections/ — declared in file_paths.xml
 * so FileProvider can serve these URIs if needed.
 */
private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri {
    val directory = File(context.filesDir, "collections").apply {
        if (!exists()) mkdirs()
    }
    val file = File(directory, "cropped_${UUID.randomUUID()}.jpg")
    try {
        FileOutputStream(file).use { out ->
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            if (!success) throw IllegalStateException("Bitmap.compress() returned false")
            out.flush()
        }
    } catch (e: Exception) {
        if (file.exists()) file.delete() // Remove 0-byte corrupt file
        throw e
    }
    return Uri.fromFile(file)
}