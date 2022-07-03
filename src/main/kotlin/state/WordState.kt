package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 记忆单词的数据类 */
@ExperimentalSerializationApi
@Serializable
data class DataWordState(
    val wordVisible: Boolean = true,
    val phoneticVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val translationVisible: Boolean = true,
    val subtitlesVisible: Boolean = true,
    val speedVisible: Boolean = false,
    val isPlaySoundTips: Boolean = true,
    val soundTipsVolume: Float = 0.6F,
    val pronunciation: String = "us",
    val isAuto: Boolean = false,
    val index: Int = 0,
    val hardVocabularyIndex: Int = 0,
    var vocabularyName: String = "四级",
    var vocabularyPath: String = "vocabulary/大学英语/四级.json",
)

/** 记忆单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class WordState(dataWordState: DataWordState) {

    /**
     * 单词组件的可见性
     */
    var wordVisible by mutableStateOf(dataWordState.wordVisible)

    /**
     * 音标组件的可见性
     */
    var phoneticVisible by mutableStateOf(dataWordState.phoneticVisible)

    /**
     * 词型组件的可见性
     */
    var morphologyVisible by mutableStateOf(dataWordState.morphologyVisible)

    /**
     * 定义组件的可见性
     */
    var definitionVisible by mutableStateOf(dataWordState.definitionVisible)

    /**
     * 翻译组件的可见性
     */
    var translationVisible by mutableStateOf(dataWordState.translationVisible)

    /**
     * 字幕组件的可见性
     */
    var subtitlesVisible by mutableStateOf(dataWordState.subtitlesVisible)

    /**
     * 速度组件的可见性
     */
    var speedVisible by mutableStateOf(dataWordState.speedVisible)

    /**
     * 是否播放提示音
     */
    var isPlaySoundTips by mutableStateOf(dataWordState.isPlaySoundTips)

    /**
     * 提示音音量
     */
    var soundTipsVolume by mutableStateOf(dataWordState.soundTipsVolume)

    /**
     * 选择发音，有英音、美音、日语
     */
    var pronunciation by mutableStateOf(dataWordState.pronunciation)

    /**
     * 是否是自动切换
     */
    var isAuto by mutableStateOf(dataWordState.isAuto)

    /**
     * 当前单词的索引，从0开始，在标题栏显示的时候 +1
     */
    var index by mutableStateOf(dataWordState.index)

    /**
     * 困难词库的索引，从0开始，在标题栏显示的时候 +1
     */
    var hardVocabularyIndex by mutableStateOf(dataWordState.hardVocabularyIndex)

    /**
     * 当前单词的章节，从1开始
     */
    var chapter by mutableStateOf((dataWordState.index / 20) + 1)

    /**
     * 词库的名称
     */
    var vocabularyName by mutableStateOf(dataWordState.vocabularyName)

    /**
     * 当前正在学习的词库的路径
     */
    var vocabularyPath by mutableStateOf(dataWordState.vocabularyPath)
}