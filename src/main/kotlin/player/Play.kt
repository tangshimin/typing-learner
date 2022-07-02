package player

import data.Caption
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.awt.Component
import java.awt.EventQueue
import java.awt.Rectangle
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JFrame

/**
 * @param window 视频播放窗口
 * @param setIsPlaying 设置是否正在播放视频
 * @param volume 音量
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 * @param videoPlayerComponent 视频播放组件
 * @param bounds 视频播放窗口的位置和大小
 * 使用 JFrame 的一个原因是 swingPanel 重组的时候会产生闪光,等Jetbrains 把 bug 修复了再重构
 */
fun play(
    window: JFrame,
    setIsPlaying: (Boolean) -> Unit,
    volume: Float,
    playTriple: Triple<Caption, String, Int>,
    videoPlayerComponent: Component,
    bounds: Rectangle
) {
    val caption = playTriple.first
    val relativeVideoPath = playTriple.second
    val trackId = playTriple.third
    window.size = bounds.size
    window.location = bounds.location

    val start = parseTime(caption.start)
    val end = parseTime(caption.end)

    videoPlayerComponent.bounds = Rectangle(0, 0, bounds.size.width, bounds.size.height)
    videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            window.isVisible = false
            EventQueue.invokeLater {
                window.remove(videoPlayerComponent)
            }
            videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    window.layout = null
    window.contentPane.add(videoPlayerComponent)
    window.isVisible = true
    // 使用内部字幕轨道,通常是从 MKV 生成的词库
    if(trackId != -1){
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":sub-track=$trackId", ":start-time=$start", ":stop-time=$end")
    // 不使用内部字幕轨道，通常是从字幕生成的词库
    }else{
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":start-time=$start", ":stop-time=$end")
    }
}

/** 使用外部字幕 */
fun play(
    window: JFrame,
    setIsPlaying: (Boolean) -> Unit,
    videoPlayerComponent: Component,
    volume: Float,
    caption: Caption,
    videoPath:String,
    subtitlePath:String,
    showSubtitles:Boolean,
    bounds: Rectangle
) {
    window.size = bounds.size
    window.location = bounds.location

    val start = parseTime(caption.start)
    val end = parseTime(caption.end)
    videoPlayerComponent.bounds = Rectangle(0, 0, bounds.size.width, bounds.size.height)

    videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            if(showSubtitles){
                mediaPlayer.subpictures().setSubTitleUri(subtitlePath)
            }
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            window.isVisible = false
            EventQueue.invokeLater {
                window.remove(videoPlayerComponent)
            }
            videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    window.layout = null
    window.contentPane.add(videoPlayerComponent)
    window.isVisible = true

    if(showSubtitles){
        videoPlayerComponent.mediaPlayer().media()
            .play(videoPath,  ":sub-text-scale=100",":start-time=$start", ":stop-time=$end")
    }else{
        videoPlayerComponent.mediaPlayer().media()
            .play(videoPath,  ":no-sub-autodetect-file",":sub-text-scale=100",":start-time=$start", ":stop-time=$end")
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
    subtitlePath:String,
){
    val start = parseTime(caption.start)
    val end = parseTime(caption.end)

    audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.subpictures().setSubTitleUri(subtitlePath)
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    audioPlayerComponent.mediaPlayer().media()
        .play(videoPath,  ":start-time=$start",  ":stop-time=$end")
}

fun parseTime(time:String):Double{
    var duration = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss.SSS")).toNanoOfDay().toDouble()
    duration = duration.div(1000_000_000)
    return duration
}