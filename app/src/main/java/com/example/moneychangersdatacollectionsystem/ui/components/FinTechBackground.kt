package com.example.moneychangersdatacollectionsystem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.moneychangersdatacollectionsystem.ui.theme.BluePrimary
import com.example.moneychangersdatacollectionsystem.ui.theme.IndigoCard
import com.example.moneychangersdatacollectionsystem.ui.theme.PurpleMesh

@Composable
fun FinTechBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Professional Mesh Gradient Effect
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(BluePrimary.copy(alpha = 0.1f), Color.White)
                )
            )

            drawCircle(
                color = PurpleMesh.copy(alpha = 0.05f),
                radius = canvasWidth,
                center = androidx.compose.ui.geometry.Offset(canvasWidth * 0.8f, 0f)
            )

            drawCircle(
                color = IndigoCard.copy(alpha = 0.05f),
                radius = canvasWidth * 0.8f,
                center = androidx.compose.ui.geometry.Offset(0f, canvasHeight * 0.9f)
            )
        }
        content()
    }
}
