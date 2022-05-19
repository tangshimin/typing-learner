package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 抄写单词的数据类 */
@ExperimentalSerializationApi
@Serializable
data class TypingSubtitlesData(
    val videoPath: String = "",
    val subtitlesPath: String = "",
    val trackID: Int = 0,
    val trackDescription: String = "",
    val trackSize: Int = 0,
    val currentIndex: Int = 0,
    val firstVisibleItemIndex: Int = 0,
    var sentenceMaxLength: Int = 0
)

/** 抄写单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class TypingSubtitlesState(typingSubtitlesData: TypingSubtitlesData) {

    /** 抄写字幕时的 MKV 视频文件的路径 */
    var videoPath by mutableStateOf(typingSubtitlesData.videoPath)

    /** 抄写字幕时的字幕文件的路径 */
    var subtitlesPath by mutableStateOf(typingSubtitlesData.subtitlesPath)

    /** 抄写字幕时的字幕的轨道 ID,
     *  如果等于 -1 表示不使用内置的轨道，
     *  而是使用外部的字幕。
     */
    var trackID by mutableStateOf(typingSubtitlesData.trackID)

    /** 选择的字幕名称  */
    var trackDescription by mutableStateOf(typingSubtitlesData.trackDescription)

    /** 字幕轨道的数量  */
    var trackSize by mutableStateOf(typingSubtitlesData.trackSize)

    /** 抄写字幕的索引  */
    var currentIndex by mutableStateOf(typingSubtitlesData.currentIndex)

    /** 抄写字幕时屏幕顶部的行索引  */
    var firstVisibleItemIndex by mutableStateOf(typingSubtitlesData.firstVisibleItemIndex)

    /** 字幕的最大长度，用来计算字幕的宽度  */
    var sentenceMaxLength by mutableStateOf(typingSubtitlesData.sentenceMaxLength)
}
