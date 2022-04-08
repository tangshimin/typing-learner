package player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VideoPlayer(windowWidth: Dp, playerVisible: Boolean) {
    val mediaPlayerComponent = LocalMediaPlayerComponent.current
    var size = if (windowWidth > 800.dp && windowWidth < 1080.dp) {
        DpSize(642.dp,390.dp)
    } else if (windowWidth > 1080.dp) {
        DpSize(1005.dp,610.dp)
    } else {
        DpSize(540.dp,304.dp)
    }
    if(!playerVisible) size = DpSize(0.dp,0.dp)
    SwingPanel(
        background = Color(MaterialTheme.colors.background.toArgb()),
        modifier = Modifier.size(size).padding(start = 50.dp),
        factory = {
            mediaPlayerComponent
        }
    )
}



