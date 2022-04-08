package theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

//val indigo400 = Color(0xFF5C6BC0)
//val graySurface = Color(0xFF2A2A2A)
//val lightGray = Color(0xFFD3D3D3)
//val spotifyGreen = Color(0xFF1db954)
//val green600 = Color(0xFF43A047)
//val blue500 = Color(0xFF111827)
//val spotifyBlack = Color(0xff100c08)
//val darkGray = Color(0xFF565656)
//
//val spotifyGradient = listOf(spotifyGreen, Color.Yellow, spotifyGreen.copy(alpha = 0.8f))

//val DarkColorScheme = darkColors(
//    primary = green600,
//    primaryVariant = green600,
//    secondary = Color.White,
//    background = darkBackground,
//    surface = darkBackground,
//    onPrimary = darkGray,
//    onSecondary = lightGray,
//    onBackground = Color.White,
//    onSurface = Color.White,
//    error = Color.Red,
//
//)
//val  LightColorScheme= lightColors(
//    primary = green600,
//    primaryVariant = green600,
//    secondary = Color.Black,
//    background = Color.White,
//    surface = Color.White,
//    onPrimary = Color.White,
//    onSecondary = graySurface,
//    onBackground = spotifyBlack,
//    onSurface = spotifyBlack
//)

val green = Color(0xFF09af00)// Color(46, 125, 50)
val green700 = Color(0x41c300)
val green600 = Color(0xFF43A047)
val blue500 = Color(0xFF111827)
val IDEADarkThemeOnBackground = Color(133, 144, 151)
val DarkColorScheme = darkColors(
    primary = green,
    onBackground = IDEADarkThemeOnBackground
)
val LightColorScheme = lightColors(
    primary = green,
)