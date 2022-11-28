package player

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import data.Caption
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Color
import java.awt.Component
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * @param window 视频播放窗口,  使用 JFrame 的一个原因是 swingPanel 重组的时候会产生闪光,
 * 相关 Issue: https://github.com/JetBrains/compose-jb/issues/1800,
 * 等Jetbrains 把 bug 修复了再重构。
 * @param setIsPlaying 设置是否正在播放视频
 * @param volume 音量
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 * @param videoPlayerComponent 视频播放组件
 * @param bounds 视频播放窗口的位置和大小
 * @param externalSubtitlesVisible 是否加载外部字幕
 * 使用 JFrame 的一个原因是 swingPanel 重组的时候会产生闪光,等Jetbrains 把 bug 修复了再重构
 */
fun play(
    window: JFrame,
    setIsPlaying: (Boolean) -> Unit,
    volume: Float,
    playTriple: Triple<Caption, String, Int>,
    videoPlayerComponent: Component,
    bounds: Rectangle,
    externalSubtitlesVisible:Boolean = false,
) {


    val playIcon = FlatSVGIcon(File("src/main/resources/icon/play_arrow_white_24dp.svg"))
    val pauseIcon = FlatSVGIcon(File("src/main/resources/icon/pause_white_24dp.svg"))
    val stopIcon = FlatSVGIcon(File("src/main/resources/icon/stop_white_24dp.svg"))
    if(FlatLaf.isLafDark()){
        playIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
        pauseIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
        stopIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
    }

    val playButton = FlatButton()
    playButton.isContentAreaFilled = false
    playButton.buttonType = FlatButton.ButtonType.roundRect
    playButton.icon = pauseIcon

    val stopButton = FlatButton()
    stopButton.isContentAreaFilled = false
    stopButton.buttonType = FlatButton.ButtonType.roundRect
    stopButton.icon = stopIcon

    val controlPanel = JPanel()
    controlPanel.isOpaque = false
    controlPanel.bounds = Rectangle(0,bounds.size.height - 50 ,bounds.size.width,50)
    controlPanel.isVisible = false
    controlPanel.add(playButton)
    controlPanel.add(stopButton)


    val embeddedMediaPlayerComponent = videoPlayerComponent as EmbeddedMediaPlayerComponent
    val playAction :() -> Unit = {
        if(videoPlayerComponent.mediaPlayer().status().isPlaying){
            videoPlayerComponent.mediaPlayer().controls().pause()
            playButton.icon = playIcon
            controlPanel.isVisible = true
        }else{
            videoPlayerComponent.mediaPlayer().controls().play()
            playButton.icon = pauseIcon
        }
        videoPlayerComponent.requestFocusInWindow()
    }
    playButton.addActionListener { playAction() }
    val keyListener = object: KeyAdapter() {
        override fun keyPressed(keyeEvent: KeyEvent) {
            if(keyeEvent.keyCode == 32){
                playAction()
            }
        }
    }
    val mouseListener = object: MouseAdapter(){
        override fun mouseClicked(e: MouseEvent?) {
           if(e?.button == 1){
               playAction()
           }
        }

        override fun mouseEntered(e: MouseEvent?) {
            if(!controlPanel.isVisible){
                controlPanel.isVisible = true
            }
        }

    }

    // 关闭操作的公共函数，一次用于播放完毕，自动关闭，一次用于停止按钮的 clicked 动作。
    val closeFunc:() -> Unit = {
        if(videoPlayerComponent.mediaPlayer().status().isPlaying){
        videoPlayerComponent.mediaPlayer().controls().pause()
        }
        setIsPlaying(false)
        window.isVisible = false
        EventQueue.invokeLater {
            window.remove(videoPlayerComponent)
            window.remove(controlPanel)
        }

        videoPlayerComponent.removeKeyListener(keyListener)
        embeddedMediaPlayerComponent.removeMouseListener(mouseListener)
    }

    val mediaPlayerEventListener = object:MediaPlayerEventAdapter(){
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            videoPlayerComponent.requestFocusInWindow()
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }
        override fun finished(mediaPlayer: MediaPlayer) {
            closeFunc()
            videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    }

    val closeAction:() -> Unit = {
        closeFunc()
        videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(mediaPlayerEventListener)
    }
    stopButton.addActionListener { closeAction() }

    embeddedMediaPlayerComponent.videoSurfaceComponent().addMouseListener(mouseListener)
    videoPlayerComponent.addKeyListener(keyListener)
    videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(mediaPlayerEventListener)
    videoPlayerComponent.bounds = Rectangle(0, 0, bounds.size.width, bounds.size.height)

    window.size = bounds.size
    window.location = bounds.location
    window.layout = null
    window.contentPane.add(controlPanel)
    window.contentPane.add(videoPlayerComponent)
    window.isVisible = true

    val caption = playTriple.first
    val relativeVideoPath = playTriple.second
    val trackId = playTriple.third
    val start = parseTime(caption.start)
    val end = parseTime(caption.end)
    // 使用内部字幕轨道,通常是从 MKV 生成的词库
    if(trackId != -1){
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":sub-track=$trackId", ":start-time=$start", ":stop-time=$end")
    // 自动加载外部字幕
    }else if(externalSubtitlesVisible){
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":sub-autodetect-file",":start-time=$start", ":stop-time=$end")
    }else{
        // 视频有硬字幕，加载了外部字幕会发生重叠。
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":no-sub-autodetect-file",":start-time=$start", ":stop-time=$end")
    }
}



/**
 * 播放音频
 */
fun play(
    setIsPlaying: (Boolean) -> Unit,
    audioPlayerComponent: AudioPlayerComponent,
    volume: Float,
    caption: Caption,
    videoPath:String,
){


    audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    val start = parseTime(caption.start)
    val end = parseTime(caption.end)
    audioPlayerComponent.mediaPlayer().media()
        .play(videoPath,  ":start-time=$start",  ":stop-time=$end")
}

fun parseTime(time:String):Double{
    var duration = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss.SSS")).toNanoOfDay().toDouble()
    duration = duration.div(1000_000_000)
    return duration
}