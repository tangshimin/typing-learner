package player

import com.sun.jna.NativeLibrary
import state.getResourcesFile
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.awt.Desktop
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JOptionPane
import javax.swing.event.HyperlinkEvent


/**
 * 初始化视频播放组件
 */
fun createMediaPlayerComponent(): Component {
    // 如果是 Windows 就使用内置的 VLC 播放器
    if (isWindows()) {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), getResourcesFile("VLC").absolutePath ?: "")
    } else{
        NativeDiscovery().discover()
    }

    // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
    return if (isMacOS()) {
        // macOS 可能没有安装 VLC 播放器
        try{
            NativeLibrary.getInstance("vlc")
        }catch ( exception:UnsatisfiedLinkError){
            val message = JEditorPane()
            message.contentType = "text/html"
            message.text = "没有安装 <a href='https://www.videolan.org/'>VLC 视频播放器</a><br>"+
                    "typing-learner 需要 VLC 朗读单词发音和播放视频<br>"
            message.addHyperlinkListener {
                if(it.eventType == HyperlinkEvent.EventType.ACTIVATED){
                    Desktop.getDesktop().browse(it.url.toURI())
                }
            }
            message.isEditable = false
            JOptionPane.showMessageDialog(null, message)
        }
        CallbackMediaPlayerComponent()
    } else if(isWindows()){
        val args = listOf(
            "--video-title=vlcj video output",
            "--no-snapshot-preview",
            "--quiet",
            "--intf=dummy")
        val mediaPlayerFactory = MediaPlayerFactory(null,args )
        EmbeddedMediaPlayerComponent(mediaPlayerFactory, null, null, null, null)
    }else{
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