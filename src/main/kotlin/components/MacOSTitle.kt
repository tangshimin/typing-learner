package components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow

/** 用于抄写字幕界面和抄写文本界面 */
@Composable
fun MacOSTitle(
    title: String,
    window: ComposeWindow,
    modifier: Modifier
) {
    Text(
        text = title,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier
    )
    window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
    window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
}

/** 用于记忆单词界面 */
@Composable
fun MacOSTitle(
    title: String,
    window: ComposeWindow,
) {
    Text(
        text = title,
        color = MaterialTheme.colors.onBackground,
    )
    window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
    window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
}