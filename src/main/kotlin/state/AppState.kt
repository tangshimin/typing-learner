package state

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.res.useResource
import components.flatlaf.InitializeFileChooser
import data.Caption
import data.Word
import data.loadMutableVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.isMacOS
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.FutureTask
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JFrame

@ExperimentalSerializationApi
@Serializable
data class TypingState(
    val darkTheme: Boolean = true,
    val speedVisible: Boolean = false,
    val wordVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val subtitlesVisible: Boolean = false,
    val translationVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val keystrokeSound: Boolean = true,
    val keystrokeVolume: Float = 0.75F,
    val soundTips: Boolean = true,
    val soundTipsVolume: Float = 0.7F,
    val audioVolume: Float = 0.8F,
    val phoneticVisible: Boolean = true,
    val pronunciation: String = "us",
    val isAuto: Boolean = false,
    val index: Int = 0,
    val chapter: Int = 1,
    var vocabularyName: String = "四级",
    var vocabularyPath: String = "vocabulary/大学英语/四级.json"
)

@OptIn(ExperimentalSerializationApi::class)
class MutableTypingState(typingState: TypingState) {
    var darkTheme by mutableStateOf(typingState.darkTheme)
    var speedVisible by mutableStateOf(typingState.speedVisible)
    var wordVisible by mutableStateOf(typingState.wordVisible)
    var morphologyVisible by mutableStateOf(typingState.morphologyVisible)
    var translationVisible by mutableStateOf(typingState.translationVisible)
    var definitionVisible by mutableStateOf(typingState.definitionVisible)
    var subtitlesVisible by mutableStateOf(typingState.subtitlesVisible)
    var keystrokeSound by mutableStateOf(typingState.keystrokeSound)
    var keystrokeVolume by mutableStateOf(typingState.keystrokeVolume)
    var soundTips by mutableStateOf(typingState.soundTips)
    var soundTipsVolume by mutableStateOf(typingState.soundTipsVolume)
    var audioVolume by mutableStateOf(typingState.audioVolume)
    var phoneticVisible by mutableStateOf(typingState.phoneticVisible)
    var pronunciation by mutableStateOf(typingState.pronunciation)
    var isAuto by mutableStateOf(typingState.isAuto)
    var index by mutableStateOf(typingState.index)
    var chapter by mutableStateOf(typingState.chapter)
    var vocabularyName by mutableStateOf(typingState.vocabularyName)
    var vocabularyPath by mutableStateOf(typingState.vocabularyPath)
}






@ExperimentalSerializationApi
class AppState {

    private val settings = composeAppResource("settings.json")

    //Serializable State
    var typing: MutableTypingState = loadTypingState()

    var videoPlayerWindow = createVideoPlayerWindow()

    var vocabulary = loadMutableVocabulary(typing.vocabularyPath)

    // subtitleName to captionsMap (word.value to word.captions)
    var captionsMap = mutableMapOf<String, HashMap<String, List<Caption>>>()

    // 默写模式
    var isDictation by mutableStateOf(false)
    var isReviewWrongList by mutableStateOf(false)
    var dictationWords = listOf<Word>()
    var dictationIndex by mutableStateOf(0)

    // 进入默写模式之前需要保存变量 `typing` 的一些状态,退出默写模式后恢复
    private var typingStateMap: MutableMap<String, Boolean> = mutableMapOf()


    var isPlaying by mutableStateOf(false)
    var openSelectChapter by mutableStateOf(false)
    var openSettings by mutableStateOf(false)

    //  page
    var selectVocabulary by mutableStateOf(false)
    var loadingFileChooserVisible by mutableStateOf(false)
    var generateVocabularyFromDocument by mutableStateOf(false)
    var generateVocabularyFromSubtitles by mutableStateOf(false)
    var generateVocabularyFromMKV by mutableStateOf(false)


    // Speed
    var isStart by mutableStateOf(false)
    var inputCount by mutableStateOf(0)
    var correctCount by mutableStateOf(0F)
    var wrongCount by mutableStateOf(0)
    var time: LocalTime by mutableStateOf(LocalTime.parse("00:00:00", DateTimeFormatter.ofPattern("HH:mm:ss")))
    var timer by mutableStateOf(Timer())
    var autoPauseTimer by mutableStateOf(Timer())


    // FileChooser
    var futureFileChooser: FutureTask<JFileChooser> = InitializeFileChooser(typing.darkTheme)


    private fun loadTypingState(): MutableTypingState {
        if (settings.exists()) {
            val typingState = Json.decodeFromString<TypingState>(settings.readText())
            return MutableTypingState(typingState)
        }
        return MutableTypingState(TypingState())
    }



    @OptIn(ExperimentalComposeUiApi::class)
    private fun createVideoPlayerWindow(): JFrame {
        val window = JFrame()
        window.title = "视频播放窗口"
        ResourceLoader.Default.load("logo/logo.png").use { inputStream ->
            val image = ImageIO.read(inputStream)
            window.iconImage = image
        }
        window.isUndecorated = true
        window.isAlwaysOnTop = true
        return window
    }


    @OptIn(ExperimentalComposeUiApi::class)
    fun saveTypingState() {
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
//        Thread(Runnable {
        val typingState = TypingState(
            typing.darkTheme,
            typing.speedVisible,
            typing.wordVisible,
            typing.morphologyVisible,
            typing.subtitlesVisible,
            typing.translationVisible,
            typing.definitionVisible,
            typing.keystrokeSound,
            typing.keystrokeVolume,
            typing.soundTips,
            typing.soundTipsVolume,
            typing.audioVolume,
            typing.phoneticVisible,
            typing.pronunciation,
            typing.isAuto,
            typing.index,
            typing.chapter,
            typing.vocabularyName,
            typing.vocabularyPath
        )
        val json = format.encodeToString(typingState)
        settings.writeText(json)
//        }).start()

    }

    fun getCurrentWord(): Word {
        if (isDictation) {
            return dictationWords[dictationIndex]
        }
        return getWord(typing.index)
    }

    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0..size) {
            vocabulary.wordList[index]
        } else {
            typing.index = 0
            saveTypingState()
            vocabulary.wordList[0]
        }

    }

    /*
    1 -> 0,19
    2 -> 20,39
    3 -> 40,59
    if chapter == 2
    start = 2 * 20 -20, end = 2 * 20  -1
    if chapter == 3
    start = 3 * 20 -20, end = 3 * 20 - 1
     */
    fun generateDictationWords(currentWord: String): List<Word> {
        val start = typing.chapter * 20 - 20
        val end = typing.chapter * 20
        println("Dictation words size: ${vocabulary.wordList.subList(start, end).shuffled().size}")
        var list = vocabulary.wordList.subList(start, end).shuffled()
        // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
        while (list[0].value == currentWord) {
            list = vocabulary.wordList.subList(start, end).shuffled()
        }
        return list
    }


    fun enterDictationMode() {
        val currentWord = getCurrentWord().value
        dictationWords = generateDictationWords(currentWord)
        dictationIndex = 0
        // 先保存状态
        typingStateMap["isAuto"] = typing.isAuto
        typingStateMap["wordVisible"] = typing.wordVisible
        typingStateMap["phoneticVisible"] = typing.phoneticVisible
        typingStateMap["definitionVisible"] = typing.definitionVisible
        typingStateMap["morphologyVisible"] = typing.morphologyVisible
        typingStateMap["translationVisible"] = typing.translationVisible
        typingStateMap["subtitlesVisible"] = typing.subtitlesVisible
        // 再改变状态
        typing.isAuto = true
        typing.wordVisible = false
        typing.phoneticVisible = false
        typing.definitionVisible = false
        typing.morphologyVisible = false
        typing.translationVisible = false
        typing.subtitlesVisible = false

        isDictation = true
    }

    fun exitDictationMode() {
        // 恢复状态
        typing.isAuto = typingStateMap["isAuto"]!!
        typing.wordVisible = typingStateMap["wordVisible"]!!
        typing.phoneticVisible = typingStateMap["phoneticVisible"]!!
        typing.definitionVisible = typingStateMap["definitionVisible"]!!
        typing.morphologyVisible = typingStateMap["morphologyVisible"]!!
        typing.translationVisible = typingStateMap["translationVisible"]!!
        typing.subtitlesVisible = typingStateMap["subtitlesVisible"]!!

        isDictation = false
        isReviewWrongList = false
    }

    
    // 复习错误单词，还是在默写模式，并且利用了默写模式的单词列表。
    fun enterReviewMode(reviewList: List<Word>) {
        // 先把 typing 的状态恢复
        exitDictationMode()
        isDictation = true
        isReviewWrongList = true
        dictationWords = reviewList
        dictationIndex = 0
    }


}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberAppState() = remember {
    AppState()
}

/**
相关链接：#938 https://github.com/JetBrains/compose-jb/issues/938
#938 的测试代码的地址
https://github.com/JetBrains/compose-jb/blob/3070856954d4c653ea13a73aa77adb86a2788c66/gradle-plugins/compose/src/test/test-projects/application/resources/src/main/kotlin/main.kt
 如果 System.getProperty("compose.application.resources.dir") 为 null,说明还没有打包
 */
fun composeAppResource(path: String): File {
    val property = "compose.application.resources.dir"
    val dir = System.getProperty(property)
    return if (dir != null) {
        //打包之后的环境
        File(dir).resolve(path)
    } else {
        // 开发环境
        // 通用资源
        var file = File("resources/common/$path")
        if (!file.exists()) {
            file = File("resources/windows/$path")
        }
        if (!file.exists() && isMacOS()) {
            file = File("resources/macOS/$path")
        }
        file
    }
}

// TODO 调用这个方法的地方都有加错误处理
fun getResourcesFile(path: String): File? {
    var file: File? = null
    try {
        file = if (File(path).isAbsolute) {
            File(path)
        } else {
            composeAppResource(path)
        }
    } catch (exception: FileNotFoundException) {
        exception.printStackTrace()
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
    return file
}