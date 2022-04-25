package state

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.formdev.flatlaf.FlatLightLaf
import components.flatlaf.InitializeFileChooser
import data.Caption
import data.Word
import data.loadMutableVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.createMediaPlayerComponent
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
import javax.swing.JOptionPane

@ExperimentalSerializationApi
@Serializable
data class TypingState(
    val isDarkTheme: Boolean = true,
    val wordVisible: Boolean = true,
    val phoneticVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val translationVisible: Boolean = true,
    val subtitlesVisible: Boolean = false,
    val speedVisible: Boolean = false,
    val isPlayKeystrokeSound: Boolean = true,
    val keystrokeVolume: Float = 0.75F,
    val isPlaySoundTips: Boolean = true,
    val soundTipsVolume: Float = 0.6F,
    val audioVolume: Float = 0.8F,
    val videoVolume: Float = 0.8F,
    val pronunciation: String = "us",
    val isAuto: Boolean = false,
    val index: Int = 0,
    var vocabularyName: String = "四级",
    var vocabularyPath: String = "vocabulary/大学英语/四级.json"
)

/**
 * 把持久化的状态变成可观察的状态
 */
@OptIn(ExperimentalSerializationApi::class)
class MutableTypingState(typingState: TypingState) {
    /**
     * 是否是深色模式
     */
    var isDarkTheme by mutableStateOf(typingState.isDarkTheme)

    /**
     * 单词组件的可见性
     */
    var wordVisible by mutableStateOf(typingState.wordVisible)

    /**
     * 音标组件的可见性
     */
    var phoneticVisible by mutableStateOf(typingState.phoneticVisible)

    /**
     * 词型组件的可见性
     */
    var morphologyVisible by mutableStateOf(typingState.morphologyVisible)

    /**
     * 定义组件的可见性
     */
    var definitionVisible by mutableStateOf(typingState.definitionVisible)

    /**
     * 翻译组件的可见性
     */
    var translationVisible by mutableStateOf(typingState.translationVisible)

    /**
     * 字幕组件的可见性
     */
    var subtitlesVisible by mutableStateOf(typingState.subtitlesVisible)

    /**
     * 速度组件的可见性
     */
    var speedVisible by mutableStateOf(typingState.speedVisible)

    /**
     * 是否播放按键音效
     */
    var isPlayKeystrokeSound by mutableStateOf(typingState.isPlayKeystrokeSound)

    /**
     * 按键音效音量
     */
    var keystrokeVolume by mutableStateOf(typingState.keystrokeVolume)

    /**
     * 是否播放提示音
     */
    var isPlaySoundTips by mutableStateOf(typingState.isPlaySoundTips)

    /**
     * 提示音音量
     */
    var soundTipsVolume by mutableStateOf(typingState.soundTipsVolume)

    /**
     * 单词发音
     */
    var audioVolume by mutableStateOf(typingState.audioVolume)

    /**
     * 视频播放
     */
    var videoVolume by mutableStateOf(typingState.videoVolume)

    /**
     * 选择发音，有英音、美音、日语
     */
    var pronunciation by mutableStateOf(typingState.pronunciation)

    /**
     * 是否是自动切换
     */
    var isAuto by mutableStateOf(typingState.isAuto)

    /**
     * 当前单词的索引，从0开始，在标题栏显示的时候 +1
     */
    var index by mutableStateOf(typingState.index)

    /**
     * 当前单词的章节，从1开始
     */
    var chapter by mutableStateOf((typingState.index / 20) + 1)

    /**
     * 词库的名称
     */
    var vocabularyName by mutableStateOf(typingState.vocabularyName)

    /**
     * 词库的路径
     */
    var vocabularyPath by mutableStateOf(typingState.vocabularyPath)
}


/**
 * 速度组件可观察的状态
 */
class MutableSpeedState() {
    var isStart by mutableStateOf(false)
    var inputCount by mutableStateOf(0)
    var correctCount by mutableStateOf(0F)
    var wrongCount by mutableStateOf(0)
    var time: LocalTime by mutableStateOf(LocalTime.parse("00:00:00", DateTimeFormatter.ofPattern("HH:mm:ss")))
    var timer by mutableStateOf(Timer())
    var autoPauseTimer by mutableStateOf(Timer())
}


@ExperimentalSerializationApi
class AppState {
    /**
     * 应用程序的配置文件
     */
    private val settings = getSettingsFile()

    /**
     * 配置文件保存的状态
     */
    var typing: MutableTypingState = loadTypingState()

    /**
     * 当前单词的正确次数
     */
    var wordCorrectTime by mutableStateOf(0)

    /**
     * 当前单词的错误次数
     */
    var wordWrongTime by mutableStateOf(0)

    /**
     * 视频播放窗口
     */
    var videoPlayerWindow = createVideoPlayerWindow()

    /**
     * VLC 视频播放组件
     */
    var videoPlayerComponent = createMediaPlayerComponent()

    /**
     * 文件选择器，如果不提前加载反应会很慢
     */
    var futureFileChooser: FutureTask<JFileChooser> = InitializeFileChooser(typing.isDarkTheme)

    /**
     * 词库
     */
    var vocabulary = loadMutableVocabulary(typing.vocabularyPath)

    /**
     * 链接的视频或字幕词库 Map
     * vocabularyPath to captionsMap (word.value to word.captions)
     */
    var captionsMap = mutableMapOf<String, HashMap<String, List<Caption>>>()

    /**
     * 是否是默写模式
     */
    var isDictation by mutableStateOf(false)

    /**
     * 当前章节的正确数，主要用于默写模式
     */
    var chapterCorrectTime by mutableStateOf(0F)

    /**
     * 当前章节的错误数，主要用于默写模式
     */
    var chapterWrongTime by mutableStateOf(0F)

    /**
     * 默写模式的错误单词
     */
    val dictationWrongWords = mutableMapOf<Word, Int>()

    /**
     * 默写模式 -> 复习错误单词模式
     */
    var isReviewWrongList by mutableStateOf(false)

    /**
     * 默写的单词
     */
    var dictationWords = listOf<Word>()

    /**
     * 默写模式的索引
     */
    var dictationIndex by mutableStateOf(0)

    /**
     * 进入默写模式之前需要保存变量 `typing` 的一些状态,退出默写模式后恢复
     */
    private var typingStateMap: MutableMap<String, Boolean> = mutableMapOf()

    /**
     * 是否正在播放视频
     */
    var isPlaying by mutableStateOf(false)

    /**
     * 是否打开选择章节窗口
     */
    var openSelectChapter by mutableStateOf(false)

    /**
     * 打开设置
     */
    var openSettings by mutableStateOf(false)

    /**
     * 是否显示等待窗口
     */
    var loadingFileChooserVisible by mutableStateOf(false)

    /**
     * 是否显示【合并词库】窗口
     */
    var mergeVocabulary by mutableStateOf(false)
    /**
     * 是否显示【从文档生成词库】窗口
     */
    var generateVocabularyFromDocument by mutableStateOf(false)

    /**
     * 是否显示【从字幕文件生成词库】窗口
     */
    var generateVocabularyFromSubtitles by mutableStateOf(false)

    /**
     * 是否显示【从 MKV 生成词库】 窗口
     */
    var generateVocabularyFromMKV by mutableStateOf(false)


    /**
     * 速度组件的状态
     */
    var speed = MutableSpeedState()




    /**
     * 载入应用程序设置信息
     */
    private fun loadTypingState(): MutableTypingState {
        return if (settings.exists()) {
            try{
                val typingState = Json.decodeFromString<TypingState>(settings.readText())
                MutableTypingState(typingState)
            }catch (exception:Exception){
                FlatLightLaf.setup();
                JOptionPane.showMessageDialog(null,"设置信息解析错误，将使用默认设置。\n地址：$settings")
                MutableTypingState(TypingState())
            }

        }else{
            FlatLightLaf.setup();
            JOptionPane.showMessageDialog(null,"找不到设置文件，将使用默认设置。\n地址：$settings")
            MutableTypingState(TypingState())
        }
    }


    /**
     * 初始化视频播放窗口
     */
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

    /**
     * 保存应用程序设置信息
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun saveTypingState() {
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val typingState = TypingState(
            typing.isDarkTheme,
            typing.wordVisible,
            typing.phoneticVisible,
            typing.morphologyVisible,
            typing.definitionVisible,
            typing.translationVisible,
            typing.subtitlesVisible,
            typing.speedVisible,
            typing.isPlayKeystrokeSound,
            typing.keystrokeVolume,
            typing.isPlaySoundTips,
            typing.soundTipsVolume,
            typing.audioVolume,
            typing.videoVolume,
            typing.pronunciation,
            typing.isAuto,
            typing.index,
            typing.vocabularyName,
            typing.vocabularyPath
        )
        val json = format.encodeToString(typingState)
        settings.writeText(json)
    }

    /**
     * 获得当前单词
     */
    fun getCurrentWord(): Word {
        if (isDictation) {
            return dictationWords[dictationIndex]
        }
        return getWord(typing.index)
    }

    /**
     * 根据索引返回单词
     */
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


    /**
     * 为默写模式创建一个随机词汇表
    - 伪代码
    - 1 -> 0,19
    - 2 -> 20,39
    - 3 -> 40,59
    - if chapter == 2
    - start = 2 * 20 -20, end = 2 * 20  -1
    - if chapter == 3
    - start = 3 * 20 -20, end = 3 * 20 - 1
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


    /**
     * 进入默写模式，进入默写模式要保存好当前的状态，退出默写模式后再恢复
     */
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

    /**
     * 退出默写模式，恢复应用状态
     */
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

    /**
     * 重置章节计数器,清空默写模式存储的错误单词
     */
    val resetChapterTime: () -> Unit = {
        chapterCorrectTime = 0F
        chapterWrongTime = 0F
        dictationWrongWords.clear()
    }

    /**
     * 进入复习错误单词模式，复习错误单词模式属于默写模式的子模式，并且利用了默写模式的单词列表。
     */
    fun enterReviewMode(reviewList: List<Word>) {
        // 先把 typing 的状态恢复
        exitDictationMode()
        isDictation = true
        isReviewWrongList = true
        dictationWords = reviewList
        dictationIndex = 0
    }

    /**
     * 保存当前的词库
     */
    fun saveCurrentVocabulary() {
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        Thread(Runnable {
            val json = format.encodeToString(vocabulary.serializeVocabulary)
            val file = getResourcesFile(typing.vocabularyPath)
            file?.writeText(json)
        }).start()
    }

}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberAppState() = remember {
    AppState()
}

/**
 * 载入资源，资源在打包之前和打包之后的路径是不一样的
- 相关链接：#938 https://github.com/JetBrains/compose-jb/issues/938
- #938 的测试代码的地址
- https://github.com/JetBrains/compose-jb/blob/3070856954d4c653ea13a73aa77adb86a2788c66/gradle-plugins/compose/src/test/test-projects/application/resources/src/main/kotlin/main.kt
- 如果 System.getProperty("compose.application.resources.dir") 为 null,说明还没有打包
 */
fun composeAppResource(path: String): File {
    val property = "compose.application.resources.dir"
    val dir = System.getProperty(property)
    return if (dir != null) {
        //打包之后的环境
        File(dir).resolve(path)
    } else {// 开发环境
        // 通用资源
        var file = File("resources/common/$path")
        // window 操作系统专用资源
        if (!file.exists()) {
            file = File("resources/windows/$path")
        }
        // macOS 操作系统专用资源
        if (!file.exists() && isMacOS()) {
            file = File("resources/macOS/$path")
        }
        file
    }
}

/**
 * 用户的配置文件
 */
fun getSettingsFile(): File {
    val homeDir = File(System.getProperty("user.home"))
    val applicationDir = File(homeDir, ".qwerty-learner")
    if (!applicationDir.exists()) {
        applicationDir.mkdir()
    }
    return File(applicationDir, "setting.json")
}

/**
 * 获得资源文件
 * @param path 文件路径
 */
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