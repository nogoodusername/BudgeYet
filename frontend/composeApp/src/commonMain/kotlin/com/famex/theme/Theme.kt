package com.famex.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.famex.generated.resources.Res
import com.famex.generated.resources.manrope_bold
import com.famex.generated.resources.manrope_medium
import com.famex.generated.resources.manrope_regular
import com.famex.generated.resources.manrope_semibold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

private val DarkColorScheme = darkColorScheme(
    primary = Slate50,
    onPrimary = Slate900,
    secondary = BrandTeal,
    onSecondary = Color.White,
    tertiary = BrandCoral,
    error = ErrorRed,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate100,
    outline = SlateMuted,
    outlineVariant = Slate700,
)

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    secondary = BrandTeal,
    onSecondary = Color.White,
    tertiary = BrandCoral,
    error = ErrorRed,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = SlateBorder,
    onSurfaceVariant = SlateMuted,
    outline = SlateMuted,
    outlineVariant = SlateBorder,
)

val FamExShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

// Safe default (falls back to the system font) so reads outside FamExTheme don't crash;
// FamExTheme always overrides this with the real Manrope-backed instance below.
val LocalFamExTypography = staticCompositionLocalOf { famExTypography(FontFamily.Default) }

@OptIn(ExperimentalResourceApi::class)
@Composable
fun FamExTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val manrope = FontFamily(
        Font(Res.font.manrope_regular, weight = FontWeight.Normal),
        Font(Res.font.manrope_medium, weight = FontWeight.Medium),
        Font(Res.font.manrope_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.manrope_bold, weight = FontWeight.Bold)
    )
    val famExTypography = famExTypography(manrope)

    CompositionLocalProvider(LocalFamExTypography provides famExTypography) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = FamExShapes,
            typography = famExMaterialTypography(manrope),
            content = content
        )
    }
}
