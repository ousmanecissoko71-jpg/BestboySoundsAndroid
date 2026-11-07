package com.bestboy.sounds.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
private val Palette = darkColorScheme(primary=ElectricViolet,onPrimary=DeepBlack,secondary=AccentGold,background=DeepBlack,surface=MainDarkPurple,onBackground=TextPrimary,onSurface=TextPrimary)
@Composable fun BestboySoundsTheme(content: @Composable () -> Unit){ MaterialTheme(colorScheme=Palette,content=content) }
