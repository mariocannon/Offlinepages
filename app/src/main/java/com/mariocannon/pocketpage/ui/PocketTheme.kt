package com.mariocannon.pocketpage.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Deep = Color(0xFF0B3D4F)
private val Sea = Color(0xFF6CC3D5)
private val Coral = Color(0xFFE2683C)

@Composable
fun PocketTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Sea, secondary = Coral)
    } else {
        lightColorScheme(primary = Deep, secondary = Coral, background = Color(0xFFF4F1EA))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
