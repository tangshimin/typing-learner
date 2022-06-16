package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 抄写文本界面的数据类 */
@ExperimentalSerializationApi
@Serializable
data class DataTextState(
    val textPath:String = "",
    val currentIndex: Int = 0,
    val firstVisibleItemIndex: Int = 0,
)

/** 抄写文本界面的可观察状态类 */
@OptIn(ExperimentalSerializationApi::class)
class TextState(dataTextState: DataTextState){

    /** 文本文件的路径 */
    var textPath by mutableStateOf(dataTextState.textPath)

    /** 正在抄写的行数 */
    var currentIndex by mutableStateOf(dataTextState.currentIndex)

    /** 正在抄写的那一页的第一行行数 */
    var firstVisibleItemIndex by mutableStateOf(dataTextState.firstVisibleItemIndex)
}