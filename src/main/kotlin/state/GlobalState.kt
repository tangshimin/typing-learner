package state

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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
    val primaryColorValue: ULong = 18377412168996880384UL,
    val textStyle: String = "H2",
    val letterSpacing: Float = 5F,
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

    /**
     * 字体样式
     */
    var textStyle by mutableStateOf(globalData.textStyle)

    /**
     * 字体大小
     */
    var fontSize by mutableStateOf(TextUnit.Unspecified)
    /**
     *  字间隔空
     */
    var letterSpacing by mutableStateOf((globalData.letterSpacing).sp)
}
@Composable
 fun computeFontSize(textStyle: String): TextUnit {
   return when(textStyle){
        "H1" ->{
            MaterialTheme.typography.h1.fontSize
        }
        "H2" ->{
            MaterialTheme.typography.h2.fontSize
        }
        "H3" ->{
            MaterialTheme.typography.h3.fontSize
        }
        "H4" ->{
            MaterialTheme.typography.h4.fontSize
        }
        "H5" ->{
            MaterialTheme.typography.h5.fontSize
        }
        "H6" ->{
            MaterialTheme.typography.h6.fontSize
        }
        "Subtitle1" ->{
            MaterialTheme.typography.subtitle1.fontSize
        }
        "Subtitle2" ->{
            MaterialTheme.typography.subtitle2.fontSize
        }
        "Body1" ->{
            MaterialTheme.typography.body1.fontSize
        }
        "Body2" ->{
            MaterialTheme.typography.body2.fontSize
        }
        "Caption" ->{
            MaterialTheme.typography.caption.fontSize
        }
        "Overline" ->{
            MaterialTheme.typography.overline.fontSize
        }
        else ->{ MaterialTheme.typography.h2.fontSize
        }

    }
}
