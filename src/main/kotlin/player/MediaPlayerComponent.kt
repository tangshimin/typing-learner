package player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.sun.jna.NativeLibrary
import state.getResourcesFile
import uk.co.caprica.vlcj.binding.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.util.*


val LocalMediaPlayerComponent = staticCompositionLocalOf<Component> {
    error("LocalMediaPlayerComponent isn't provided")
}

@Composable
fun rememberMediaPlayerComponent():Component = remember {
    val mediaPlayerComponent = createMediaPlayerComponent()
    mediaPlayerComponent
}

/**
 * 初始化视频播放组件
 */
fun createMediaPlayerComponent() :Component{
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    // 如果用户本地没有 VLC 就使用内置的 VLC
    if(!NativeDiscovery().discover() && os.indexOf("windows") >= 0){
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), getResourcesFile("VLC").absolutePath ?: "")
    }

    // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
    return  if (isMacOS()) {
        CallbackMediaPlayerComponent()
    } else {
        EmbeddedMediaPlayerComponent()
    }
}


fun isMacOS(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0
}
fun isWindows(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("windows") >= 0
}

fun Component.mediaPlayer(): MediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}