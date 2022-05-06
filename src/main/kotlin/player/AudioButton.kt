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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Component
import java.io.File
import java.net.URL


@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AudioButton(
    word: String,
    volume: Float,
    pronunciation: String,
) {
    if (pronunciation != "false") {
        val scope = rememberCoroutineScope()
        val audioPlayerComponent = LocalMediaPlayerComponent.current
        var isPlaying by remember { mutableStateOf(false) }

        /**
         * 防止用户频繁按 Enter 键，频繁的调用 VLC 导致程序崩溃
         */
        var isAutoPlay by remember { mutableStateOf(true) }

        val playAudio = {
            playAudio(word, volume, pronunciation, audioPlayerComponent,
                changePlayerState = { isPlaying = it },
                setIsAutoPlay = { isAutoPlay = it })
        }
        Column(
            modifier = Modifier
                .height(66.dp)
                .padding(top = 13.dp)
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
                        if(!isPlaying){
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
            SwingPanel(
                modifier = Modifier.size(DpSize(0.dp, 0.dp)),
                factory = {
                    audioPlayerComponent
                }
            )
        }

        SideEffect {
            if (isAutoPlay) {
                playAudio()
            }

        }
    }

}

fun playAudio(
    word: String,
    volume: Float,
    pronunciation: String,
    mediaPlayerComponent: Component,
    changePlayerState: (Boolean) -> Unit,
    setIsAutoPlay: (Boolean) -> Unit,
) {
    val path = getAudioPath(word, pronunciation)
    if (path.isNotEmpty()) {
        changePlayerState(true)
        setIsAutoPlay(false)
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                mediaPlayer.audio().setVolume((volume * 100).toInt())
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                changePlayerState(false)
                setIsAutoPlay(true)
                mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
            }
        })
        mediaPlayerComponent.mediaPlayer().media().play(path)
    }

}

fun getAudioPath(word: String, pronunciation: String): String {
    var audioDir = File("audio")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }

    var path = ""
    val type: Any = when (pronunciation) {
        "us" -> "type=2"
        "uk" -> "type=1"
        "jp" -> "le=jap"
        else -> println(pronunciation)
    }
    val fileName = word + "_" + pronunciation
    if (audioDir.listFiles().isNotEmpty()) {
        audioDir.listFiles().forEach { file ->
            if (file.nameWithoutExtension == fileName) {
                path = file.absolutePath
            }
        }
    }
    var mutableWord = word
    if (pronunciation == "us" || pronunciation == "uk") {
        mutableWord = mutableWord.replace(" ", "-")
    }
    // 这里从服务器读了一次，返回 URL player 会再读一次
    if (path.isEmpty()) {
        val audioURL = "https://dict.youdao.com/dictvoice?audio=${mutableWord}&${type}"
        try {
            val audioBytes = URL(audioURL).readBytes()
            val file = File("audio/${fileName}.mp3")
            file.writeBytes(audioBytes)
            path = file.absolutePath
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    return path
}