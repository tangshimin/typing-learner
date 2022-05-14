package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 全局的数据类 */
@ExperimentalSerializationApi
@Serializable
data class GlobalData(
    val type: TypingType = TypingType.WORD,
    val isDarkTheme: Boolean = true,
    val audioVolume: Float = 0.8F,
    val videoVolume: Float = 0.8F,
    val keystrokeVolume: Float = 0.75F,
    val isPlayKeystrokeSound: Boolean = true,
    val primaryColorValue: ULong = 18377412168996880384UL
)

/** 全局的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class GlobalState(globalData: GlobalData) {
    /**
     * 练习的类型
     */
    var type by mutableStateOf(globalData.type)

    /**
     * 是否是深色模式
     */
    var isDarkTheme by mutableStateOf(globalData.isDarkTheme)

    /**
     * 单词发音的音量
     */
    var audioVolume by mutableStateOf(globalData.audioVolume)

    /**
     * 视频播放的音量
     */
    var videoVolume by mutableStateOf(globalData.videoVolume)

    /**
     * 按键音效音量
     */
    var keystrokeVolume by mutableStateOf(globalData.keystrokeVolume)

    /**
     * 是否播放按键音效
     */
    var isPlayKeystrokeSound by mutableStateOf(globalData.isPlayKeystrokeSound)

    /**
     * 主色调，默认为绿色
     */
    var primaryColor by mutableStateOf(Color(globalData.primaryColorValue))
}