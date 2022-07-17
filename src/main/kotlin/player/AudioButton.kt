package player


import LocalCtrl
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Word
import kotlinx.coroutines.launch
import state.AppState
import state.getAudioDirectory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import java.net.URL

val LocalAudioPlayerComponent = staticCompositionLocalOf<AudioPlayerComponent> {
    error("LocalMediaPlayerComponent isn't provided")
}

@Composable
fun rememberAudioPlayerComponent(): AudioPlayerComponent = remember {
    AudioPlayerComponent()
}

/** 记忆单词界面的播放按钮
 * @param audioPath 发音的绝对路径
 * @param volume 音量
 * @param pronunciation 音音 或 美音
 * @param paddingTop 顶部填充
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AudioButton(
    audioPath: String,
    volume: Float,
    pronunciation: String,
    paddingTop: Dp,
) {
    if (pronunciation != "false") {
        val scope = rememberCoroutineScope()
        val audioPlayerComponent = LocalAudioPlayerComponent.current
        var isPlaying by remember { mutableStateOf(false) }

        /**
         * 防止用户频繁按 Enter 键，频繁的调用 VLC 导致程序崩溃
         */
        var isAutoPlay by remember { mutableStateOf(true) }

        val playAudio = {
            playAudio(audioPath, volume,  audioPlayerComponent,
                changePlayerState = { isPlaying = it },
                setIsAutoPlay = { isAutoPlay = it })
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(66.dp)
                .width(IntrinsicSize.Max)
        ) {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        Text(text = "朗读发音 $ctrl+J", modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                ),
            ) {
                val tint by animateColorAsState(if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground)
                IconToggleButton(
                    checked = isPlaying,
                     modifier = Modifier.padding(top = paddingTop),
                    onCheckedChange = {
                        if (!isPlaying) {
                            scope.launch {
                                playAudio()
                            }
                        }
                    }) {
                    Crossfade(isPlaying) { isPlaying ->
                        if (isPlaying) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        } else {
                            Icon(
                                Icons.Filled.VolumeDown,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        }
                    }

                }
            }
        }

        SideEffect {
            if (isAutoPlay) {
                playAudio()
            }

        }
    }

}

/**
 * 搜索界面的播放按钮
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AudioButton(
    word: Word,
    state:AppState,
    volume: Float,
    pronunciation: String,
) {
    if (pronunciation != "false") {
        val scope = rememberCoroutineScope()
        val audioPlayerComponent = LocalAudioPlayerComponent.current
        var isPlaying by remember { mutableStateOf(false) }

        val playAudio = {
            val audioPath = getAudioPath(
                word = word.value,
                audioSet = state.audioSet,
                addToAudioSet = {state.audioSet.add(it)},
                pronunciation = state.typingWord.pronunciation
            )
            playAudio(audioPath, volume,  audioPlayerComponent,
                changePlayerState = { isPlaying = it },
                setIsAutoPlay = { })
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(48.dp)
                .width(IntrinsicSize.Max)
        ) {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        Text(text = "朗读发音 $ctrl+J", modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                ),
            ) {
                val tint by animateColorAsState(if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground)
                IconToggleButton(
                    checked = isPlaying,
                    onCheckedChange = {
                        if (!isPlaying) {
                            scope.launch {
                                playAudio()
                            }
                        }
                    }) {
                    Crossfade(isPlaying) { isPlaying ->
                        if (isPlaying) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        } else {
                            Icon(
                                Icons.Filled.VolumeDown,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        }
                    }

                }
            }
        }

    }

}
fun playAudio(
    audioPath: String,
    volume: Float,
    audioPlayerComponent: AudioPlayerComponent,
    changePlayerState: (Boolean) -> Unit,
    setIsAutoPlay: (Boolean) -> Unit,
) {
    if (audioPath.isNotEmpty()) {
        changePlayerState(true)
        setIsAutoPlay(false)
        audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                mediaPlayer.audio().setVolume((volume * 100).toInt())
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                changePlayerState(false)
                setIsAutoPlay(true)
                audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
            }
        })
        audioPlayerComponent.mediaPlayer().media().play(audioPath)
    }

}

fun getAudioPath(
    word: String,
    audioSet:Set<String>,
    addToAudioSet:(String) -> Unit,
    pronunciation: String
): String {
    val audioDir = getAudioDirectory()
    var path = ""
    val type: Any = when (pronunciation) {
        "us" -> "type=2"
        "uk" -> "type=1"
        "jp" -> "le=jap"
        else -> println(pronunciation)
    }
    val fileName = word + "_" + pronunciation + ".mp3"
    // 先查询本地有没有
    if (audioSet.contains(fileName)) {
        path = File(audioDir, fileName).absolutePath
    }
    // 没有就从有道服务器下载
    if (path.isEmpty()) {
        // 如果单词有空格，查询单词发音会失败,所以要把单词的空格替换成短横。
        var mutableWord = word
        if (pronunciation == "us" || pronunciation == "uk") {
            mutableWord = mutableWord.replace(" ", "-")
        }
        val audioURL = "https://dict.youdao.com/dictvoice?audio=${mutableWord}&${type}"
        try {
            val audioBytes = URL(audioURL).readBytes()
            val file = File(audioDir, fileName)
            file.writeBytes(audioBytes)
            path = file.absolutePath
            addToAudioSet(file.name)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    return path
}