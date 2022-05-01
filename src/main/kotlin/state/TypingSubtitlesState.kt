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
    val subtitlesPath:String = "",
    val subtitlesTrackID: Int = 0,
    val trackDescription: String = "",
    val subtitlesTrackSize: Int = 0,
    val captionIndex: Int = 0,
    val firstVisibleItemIndex:Int = 0,
    var sentenceMaxLength :Int = 0
)

/** 抄写单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class TypingSubtitlesState(typingSubtitlesData :TypingSubtitlesData){

    /** 抄写字幕时的 MKV 视频文件的路径 */
    var videoPath by mutableStateOf(typingSubtitlesData.videoPath)

    /** 抄写字幕时的字幕文件的路径 */
    var subtitlesPath by mutableStateOf(typingSubtitlesData.subtitlesPath)

    /** 抄写字幕时的字幕的轨道 ID */
    var subtitlesTrackID by mutableStateOf(typingSubtitlesData.subtitlesTrackID)

    /** 选择的字幕名称  */
    var trackDescription by mutableStateOf(typingSubtitlesData.trackDescription)

    /** 字幕轨道的数量  */
    var subtitlesTrackSize by mutableStateOf(typingSubtitlesData.subtitlesTrackSize)

    /** 抄写字幕的索引  */
    var captionIndex by mutableStateOf(typingSubtitlesData.captionIndex)

    /** 抄写字幕时屏幕顶部的行索引  */
    var firstVisibleItemIndex by mutableStateOf(typingSubtitlesData.firstVisibleItemIndex)

    /** 字幕的最大长度，用来计算字幕的宽度  */
    var sentenceMaxLength by mutableStateOf(typingSubtitlesData.sentenceMaxLength)
}
