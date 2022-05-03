package state

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.formdev.flatlaf.FlatLightLaf
import components.flatlaf.InitializeFileChooser
import data.Word
import data.loadMutableVocabulary
import dialog.RecentItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.createMediaPlayerComponent
import player.isMacOS
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.FutureTask
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane


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
    val wrongColorValue: ULong = 18446462598732840960UL,
)

/** 全局的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class GlobalState(globalData :GlobalData){
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
     * 错误颜色
     */
    var wrongColorValue by mutableStateOf(globalData.wrongColorValue)
}

/**
 * 速度组件可观察的状态
 */
class MutableSpeedState {
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
     * 应用程序的全局状态
     */
    var global:GlobalState = loadGlobalState()

    /**
     * 记忆单词的配置文件保存的状态
     */
    var typingWord: TypingWordState = loadTypingWordState()

    /**
     * 抄写字幕的可观察的状态
     */
    var typingSubtitles: TypingSubtitlesState = loadTypingSubtitlesState()

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
    var futureFileChooser: FutureTask<JFileChooser> = InitializeFileChooser(global.isDarkTheme)

    /**
     * 词库
     */
    var vocabulary = loadMutableVocabulary(typingWord.vocabularyPath)

    /**
     * 最近生成的词库列表
     */
    var recentList = readRecentList()
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
    private var typingWordStateMap: MutableMap<String, Boolean> = mutableMapOf()

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
     * 是否显示【过滤词库】窗口
     */
    var filterVocabulary by mutableStateOf(false)
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


    private fun loadGlobalState():GlobalState{

       val globalSettings = getGlobalSettingsFile()
        return if(globalSettings.exists()){
            try {
                val globalData = Json.decodeFromString<GlobalData>(globalSettings.readText())
                GlobalState(globalData)
            }catch (exception:Exception){
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null,"设置信息解析错误，将使用默认设置。\n地址：$globalSettings")
                GlobalState(GlobalData())
            }
        }else{
            GlobalState(GlobalData())
        }
    }


    /**
     * 载入应用程序设置信息
     */
    private fun loadTypingWordState(): TypingWordState {
        val typingWordSettings =  getWordSettingsFile()
        return if (typingWordSettings.exists()) {
            try{
                val typingWordData = Json.decodeFromString<TypingWordData>(typingWordSettings.readText())
                TypingWordState(typingWordData)
            }catch (exception:Exception){
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null,"设置信息解析错误，将使用默认设置。\n地址：$typingWordSettings")
                TypingWordState(TypingWordData())
            }

        }else{
            TypingWordState(TypingWordData())
        }
    }

    /**
     * 载入抄写字幕的配置信息
     */
    private fun loadTypingSubtitlesState():TypingSubtitlesState{
        val typingSubtitlesSetting = getSubtitlesSettingsFile()
        return if(typingSubtitlesSetting.exists()){
            try{
                val typingSubtitlesData = Json.decodeFromString<TypingSubtitlesData>(typingSubtitlesSetting.readText())
                TypingSubtitlesState(typingSubtitlesData)
            }catch (exception:Exception){
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null,"设置信息解析错误，将使用默认设置。\n地址：$typingSubtitlesSetting")
                TypingSubtitlesState(TypingSubtitlesData())
            }
        }else{
            TypingSubtitlesState(TypingSubtitlesData())
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

    /** 保存全局的设置信息 */
    fun saveGlobalState() {
        val globalData = GlobalData(
            global.type,
            global.isDarkTheme,
            global.audioVolume,
            global.videoVolume,
            global.keystrokeVolume,
            global.isPlayKeystrokeSound,
            global.wrongColorValue
        )
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val json = format.encodeToString(globalData)
        val settings = getGlobalSettingsFile()
        settings.writeText(json)
    }
    /**
     * 保存记忆单词的设置信息
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun saveTypingWordState() {
        val typingWordData = TypingWordData(
            typingWord.wordVisible,
            typingWord.phoneticVisible,
            typingWord.morphologyVisible,
            typingWord.definitionVisible,
            typingWord.translationVisible,
            typingWord.subtitlesVisible,
            typingWord.speedVisible,
            typingWord.isPlaySoundTips,
            typingWord.soundTipsVolume,
            typingWord.pronunciation,
            typingWord.isAuto,
            typingWord.index,
            typingWord.vocabularyName,
            typingWord.vocabularyPath,
        )
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val json = format.encodeToString(typingWordData)
        val settings =  getWordSettingsFile()
        settings.writeText(json)
    }

    /** 保存抄写字幕的配置信息 */
    fun saveTypingSubtitlesState() {
        val typingSubtitlesData = TypingSubtitlesData(
            typingSubtitles.videoPath,
            typingSubtitles.subtitlesPath,
            typingSubtitles.trackID,
            typingSubtitles.trackDescription,
            typingSubtitles.trackSize,
            typingSubtitles.currentIndex,
            typingSubtitles.firstVisibleItemIndex,
            typingSubtitles.sentenceMaxLength,
        )
        val format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
         val json = format.encodeToString(typingSubtitlesData)
        val typingSubtitlesSetting = getSubtitlesSettingsFile()
         typingSubtitlesSetting.writeText(json)
    }
    /**
     * 获得当前单词
     */
    fun getCurrentWord(): Word {
        if (isDictation) {
            return dictationWords[dictationIndex]
        }
        return getWord(typingWord.index)
    }

    /**
     * 根据索引返回单词
     */
    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0..size) {
            vocabulary.wordList[index]
        } else {
            typingWord.index = 0
            saveTypingWordState()
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
        val start = typingWord.chapter * 20 - 20
        val end = typingWord.chapter * 20
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
        typingWordStateMap["isAuto"] = typingWord.isAuto
        typingWordStateMap["wordVisible"] = typingWord.wordVisible
        typingWordStateMap["phoneticVisible"] = typingWord.phoneticVisible
        typingWordStateMap["definitionVisible"] = typingWord.definitionVisible
        typingWordStateMap["morphologyVisible"] = typingWord.morphologyVisible
        typingWordStateMap["translationVisible"] = typingWord.translationVisible
        typingWordStateMap["subtitlesVisible"] = typingWord.subtitlesVisible
        // 再改变状态
        typingWord.isAuto = true
        typingWord.wordVisible = false
        typingWord.phoneticVisible = false
        typingWord.definitionVisible = false
        typingWord.morphologyVisible = false
        typingWord.translationVisible = false
        typingWord.subtitlesVisible = false

        isDictation = true
    }

    /**
     * 退出默写模式，恢复应用状态
     */
    fun exitDictationMode() {
        // 恢复状态
        typingWord.isAuto = typingWordStateMap["isAuto"]!!
        typingWord.wordVisible = typingWordStateMap["wordVisible"]!!
        typingWord.phoneticVisible = typingWordStateMap["phoneticVisible"]!!
        typingWord.definitionVisible = typingWordStateMap["definitionVisible"]!!
        typingWord.morphologyVisible = typingWordStateMap["morphologyVisible"]!!
        typingWord.translationVisible = typingWordStateMap["translationVisible"]!!
        typingWord.subtitlesVisible = typingWordStateMap["subtitlesVisible"]!!

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
     * 改变词库
     */
    fun changeVocabulary(file:File){
        vocabulary = loadMutableVocabulary(file.absolutePath)
        typingWord.vocabularyName = file.nameWithoutExtension
        typingWord.vocabularyPath = file.absolutePath
        if (isDictation) {
            exitDictationMode()
            resetChapterTime()
        }
        typingWord.chapter = 1
        typingWord.index = 0
        wordCorrectTime = 0
        wordWrongTime = 0
        saveTypingWordState()
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
            val file = getResourcesFile(typingWord.vocabularyPath)
            file.writeText(json)
        }).start()
    }

    /**
     * 读取最近生成的词库列表
     */
    private fun readRecentList(): SnapshotStateList<RecentItem> {
        val recentListFile = getRecentListFile()
        var list =  if(recentListFile.exists()){
             try {
                Json.decodeFromString<List<RecentItem>>(recentListFile.readText())
            }catch (exception:Exception){
                listOf()
            }

        }else{
            listOf()
        }
        list = list.sortedByDescending { it.time }
        val mutableStateList = mutableStateListOf<RecentItem>()
        mutableStateList.addAll(list)
        return mutableStateList
    }
    private fun getRecentListFile():File{
        val settingsDir =getSettingsDirectory()
        return  File(settingsDir,"recentList.json")
    }

    fun saveToRecentList(name:String, path: String) {

        Thread(Runnable {
            val item = RecentItem(LocalDateTime.now().toString(),name,path)
            if (!recentList.contains(item)) {
                if(recentList.size==30){
                    recentList.removeAt(29)
                }
                recentList.add(0,item)
            }else{
                recentList.remove(item)
                recentList.add(0,item)
            }
            val serializeList = mutableListOf<RecentItem>()
            serializeList.addAll(recentList)

            val json = jsonBuilder.encodeToString(serializeList)
            val recentListFile = getRecentListFile()
            recentListFile.writeText(json)
        }).start()

    }
    fun removeInvalidRecentItem(recentItem: RecentItem){
        Thread(Runnable {
            recentList.remove(recentItem)
            val serializeList = mutableListOf<RecentItem>()
            serializeList.addAll(recentList)
            val json = jsonBuilder.encodeToString(recentList)
            val recentListFile = getRecentListFile()
            recentListFile.writeText(json)
        }).start()

    }
}

val jsonBuilder = Json {
    prettyPrint = true
    encodeDefaults = true
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

/** 获取应用程序的配置文件的目录 */
fun getSettingsDirectory():File{
    val homeDir = File(System.getProperty("user.home"))
    val applicationDir = File(homeDir, ".qwerty-learner")
    if (!applicationDir.exists()) {
        applicationDir.mkdir()
    }
    return applicationDir
}
/** 获取全局的配置文件 */
private fun getGlobalSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "AppSettings.json")
}
/** 获取记忆单词的配置文件 */
private fun getWordSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingWordSettings.json")
}

/** 获取抄写字幕的配置文件 */
private fun getSubtitlesSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingSubtitlesSettings.json")
}

/**
 * 获得资源文件
 * @param path 文件路径
 */
fun getResourcesFile(path: String): File {
    val file = if (File(path).isAbsolute) {
        File(path)
    } else {
        composeAppResource(path)
    }
    return file
}