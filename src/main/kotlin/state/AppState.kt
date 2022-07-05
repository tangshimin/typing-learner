package state

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.formdev.flatlaf.FlatLightLaf
import components.flatlaf.InitializeFileChooser
import data.*
import dialog.RecentItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.createMediaPlayerComponent
import player.isMacOS
import theme.createColors
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


@ExperimentalSerializationApi
class AppState {

    /** 应用程序的全局状态 */
    var global: GlobalState = loadGlobalState()

    /** Material 颜色 */
    var colors by mutableStateOf(createColors(global.isDarkTheme, global.primaryColor))

    /** 记忆单词的配置文件保存的状态 */
    var typingWord: WordState = loadWordState()

    /** 抄写字幕的可观察的状态 */
    var typingSubtitles: SubtitlesState = loadSubtitlesState()

    /** 抄写文本的可观察状态 */
    var typingText: TextState = loadTextState()

    /** 视频播放窗口 */
    var videoPlayerWindow = createVideoPlayerWindow()

    /** VLC 视频播放组件 */
    var videoPlayerComponent = createMediaPlayerComponent()

    /** 文件选择器，如果不提前加载反应会很慢 */
    var futureFileChooser: FutureTask<JFileChooser> = InitializeFileChooser(global.isDarkTheme)

    /** 当前正在学习的词库 */
    var vocabulary = loadMutableVocabulary(typingWord.vocabularyPath)

    /** 困难词库 */
    var hardVocabulary = loadHardMutableVocabulary()

    /** 最近生成的词库列表 */
    var recentList = readRecentList()

    /** 是否是听写模式 */
    var isDictation by mutableStateOf(false)

    /** 听写模式 -> 复习错误单词模式 */
    var isReviewWrongList by mutableStateOf(false)

    /** 听写的单词 */
    var dictationWords = listOf<Word>()

    /** 听写模式的索引 */
    var dictationIndex by mutableStateOf(0)

    /** 是否正在播放视频 */
    var isPlaying by mutableStateOf(false)

    /** 是否打开选择章节窗口 */
    var openSelectChapter by mutableStateOf(false)

    /** 打开设置 */
    var openSettings by mutableStateOf(false)

    /** 是否显示等待窗口 */
    var loadingFileChooserVisible by mutableStateOf(false)

    /** 是否显示【合并词库】窗口 */
    var mergeVocabulary by mutableStateOf(false)

    /** 是否显示【过滤词库】窗口 */
    var filterVocabulary by mutableStateOf(false)

    /**
     * 是否显示【从文档生成词库】窗口
     */
    var generateVocabularyFromDocument by mutableStateOf(false)

    /** 是否显示【从字幕文件生成词库】窗口 */
    var generateVocabularyFromSubtitles by mutableStateOf(false)

    /** 是否显示【从 MKV 生成词库】 窗口 */
    var generateVocabularyFromMKV by mutableStateOf(false)

    /** 本地缓存的单词发音列表 */
    var audioSet = loadAudioSet()

    var vocabularyChanged by mutableStateOf(false)

    /** 加载全局的设置信息 */
    private fun loadGlobalState(): GlobalState {
        val globalSettings = getGlobalSettingsFile()
        return if (globalSettings.exists()) {
            try {
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val globalData = decodeFormat.decodeFromString<GlobalData>(globalSettings.readText())
                GlobalState(globalData)
            } catch (exception: Exception) {
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$globalSettings")
                GlobalState(GlobalData())
            }
        } else {
            GlobalState(GlobalData())
        }
    }


    /** 加载应用记忆单词界面的设置信息 */
    private fun loadWordState(): WordState {
        val typingWordSettings = getWordSettingsFile()
        return if (typingWordSettings.exists()) {
            try {
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val dataWordState = decodeFormat.decodeFromString<DataWordState>(typingWordSettings.readText())
                WordState(dataWordState)
            } catch (exception: Exception) {
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingWordSettings")
                WordState(DataWordState())
            }

        } else {
            WordState(DataWordState())
        }
    }

    /** 加载抄写字幕的配置信息 */
    private fun loadSubtitlesState(): SubtitlesState {
        val typingSubtitlesSetting = getSubtitlesSettingsFile()
        return if (typingSubtitlesSetting.exists()) {
            try {
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val dataSubtitlesState = decodeFormat.decodeFromString<DataSubtitlesState>(typingSubtitlesSetting.readText())
                SubtitlesState(dataSubtitlesState)
            } catch (exception: Exception) {
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingSubtitlesSetting")
                SubtitlesState(DataSubtitlesState())
            }
        } else {
            SubtitlesState(DataSubtitlesState())
        }
    }

    /** 加载抄写文本的配置信息 */
    private fun loadTextState():TextState{
        val typingTextSetting = getTextSettingsFile()
        return if(typingTextSetting.exists()){
            try{
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val dataTextState = decodeFormat.decodeFromString<DataTextState>(typingTextSetting.readText())
                TextState(dataTextState)
            }catch (exception:Exception){
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingTextSetting")
                TextState(DataTextState())
            }

        }else{
            TextState(DataTextState())
        }
    }


    /** 初始化视频播放窗口 */
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
        runBlocking {
            launch {
                val globalData = GlobalData(
                    global.type,
                    global.isDarkTheme,
                    global.audioVolume,
                    global.videoVolume,
                    global.keystrokeVolume,
                    global.isPlayKeystrokeSound,
                    global.primaryColor.value,
                    global.textStyle,
                    global.letterSpacing.value,
                    global.position.x.value,
                    global.position.y.value,
                    global.size.width.value,
                    global.size.height.value,
                    global.placement
                )
                val json = encodeBuilder.encodeToString(globalData)
                val settings = getGlobalSettingsFile()
                settings.writeText(json)
            }
        }
    }

    /** 保存记忆单词的设置信息 */
    @OptIn(ExperimentalComposeUiApi::class)
    fun saveTypingWordState() {
        runBlocking {
            launch {
                val dataWordState = DataWordState(
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
                    typingWord.hardVocabularyIndex,
                    typingWord.vocabularyName,
                    typingWord.vocabularyPath,
                )

                val json = encodeBuilder.encodeToString(dataWordState)
                val settings = getWordSettingsFile()
                settings.writeText(json)
            }
        }

    }

    /** 保存抄写字幕的配置信息 */
    fun saveTypingSubtitlesState() {
        runBlocking {
            launch {
                val dataSubtitlesState = DataSubtitlesState(
                    typingSubtitles.mediaPath,
                    typingSubtitles.subtitlesPath,
                    typingSubtitles.trackID,
                    typingSubtitles.trackDescription,
                    typingSubtitles.trackSize,
                    typingSubtitles.currentIndex,
                    typingSubtitles.firstVisibleItemIndex,
                    typingSubtitles.sentenceMaxLength,
                    typingSubtitles.currentCaptionVisible,
                    typingSubtitles.notWroteCaptionVisible,
                    typingSubtitles.externalSubtitlesVisible,
                )

                val json = encodeBuilder.encodeToString(dataSubtitlesState)
                val typingSubtitlesSetting = getSubtitlesSettingsFile()
                typingSubtitlesSetting.writeText(json)
            }
        }
    }

    /** 保持抄写文本的配置信息 */
    fun saveTypingTextState(){
        runBlocking {
            launch {
                val dataTextState = DataTextState(
                    typingText.textPath,
                    typingText.currentIndex,
                    typingText.firstVisibleItemIndex,
                )
                val json = encodeBuilder.encodeToString(dataTextState)
                val typingTextSetting = getTextSettingsFile()
                typingTextSetting.writeText(json)
            }
        }
    }

    /** 获得当前单词 */
    fun getCurrentWord(): Word {
        if (isDictation) {
            return dictationWords[dictationIndex]
        }
        return getWord(typingWord.index)
    }

    /** 根据索引返回单词 */
    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0 until size) {
            vocabulary.wordList[index]
        } else {
            // 如果用户使用编辑器修改了索引，并且不在单词列表的范围以内，就把索引改成0。
            typingWord.index = 0
            saveTypingWordState()
            vocabulary.wordList[0]
        }

    }


    /**
     * 为听写模式创建一个随机词汇表
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
        var list = vocabulary.wordList.subList(start, end).shuffled()
        // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
        while (list[0].value == currentWord) {
            list = vocabulary.wordList.subList(start, end).shuffled()
        }
        return list
    }

    /** 改变词库 */
    fun changeVocabulary(file: File,index: Int) {
        val newVocabulary = loadMutableVocabulary(file.absolutePath)
        if(newVocabulary.wordList.size>0){

            // 把困难词库的索引保存在 typingWord.
            if(vocabulary.name == "HardVocabulary"){
                typingWord.hardVocabularyIndex = typingWord.index
            }else{
                // 保存当前词库的索引到最近列表,
                saveToRecentList(vocabulary.name, typingWord.vocabularyPath,typingWord.index)
            }

            vocabulary = newVocabulary
            typingWord.vocabularyName = file.nameWithoutExtension
            typingWord.vocabularyPath = file.absolutePath

//            if (isDictation) {
//                exitDictationMode()
//            }

            typingWord.chapter = (index / 20) + 1
            typingWord.index = index
            vocabularyChanged = true
            saveTypingWordState()
        }
    }

    fun findVocabularyIndex(file:File):Int{
        var index = 0
        for (recentItem in recentList) {
            if(file.absolutePath == recentItem.path){
                index = recentItem.index
            }
        }
        return index
    }

    /** 保存当前的词库 */
    fun saveCurrentVocabulary() {
        runBlocking {
            launch {
                val json = encodeBuilder.encodeToString(vocabulary.serializeVocabulary)
                val file = getResourcesFile(typingWord.vocabularyPath)
                file.writeText(json)
            }
        }
    }
    /** 保存困难词库 */
    fun saveHardVocabulary(){
        runBlocking {
            launch {
                val json = encodeBuilder.encodeToString(hardVocabulary.serializeVocabulary)
                val file = getHardVocabularyFile()
                file.writeText(json)
            }
        }
    }

    /** 读取最近生成的词库列表 */
    private fun readRecentList(): SnapshotStateList<RecentItem> {
        val recentListFile = getRecentListFile()
        var list = if (recentListFile.exists()) {
            try {
                Json.decodeFromString<List<RecentItem>>(recentListFile.readText())
            } catch (exception: Exception) {
                listOf()
            }

        } else {
            listOf()
        }
        list = list.sortedByDescending { it.time }
        val mutableStateList = mutableStateListOf<RecentItem>()
        mutableStateList.addAll(list)
        return mutableStateList
    }

    private fun getRecentListFile(): File {
        val settingsDir = getSettingsDirectory()
        return File(settingsDir, "recentList.json")
    }

    fun saveToRecentList(name: String, path: String,index: Int) {
        runBlocking {
            launch {
                val item = RecentItem(LocalDateTime.now().toString(), name, path,index)
                if (!recentList.contains(item)) {
                    if (recentList.size == 1000) {
                        recentList.removeAt(999)
                    }
                    recentList.add(0, item)
                } else {
                    recentList.remove(item)
                    recentList.add(0, item)
                }
                val serializeList = mutableListOf<RecentItem>()
                serializeList.addAll(recentList)

                val json = encodeBuilder.encodeToString(serializeList)
                val recentListFile = getRecentListFile()
                recentListFile.writeText(json)
            }
        }

    }

    fun removeInvalidRecentItem(recentItem: RecentItem) {
        runBlocking {
            launch {
                recentList.remove(recentItem)
                val serializeList = mutableListOf<RecentItem>()
                serializeList.addAll(recentList)
                val json = encodeBuilder.encodeToString(serializeList)
                val recentListFile = getRecentListFile()
                recentListFile.writeText(json)
            }
        }
    }

    private fun loadAudioSet(): MutableSet<String> {
        val audioDir = getAudioDirectory()
        if (!audioDir.exists()) {
            audioDir.mkdir()
        }
        var set = mutableSetOf<String>()
        set.addAll(audioDir.list())
        return set
    }
}

/** 速度组件可观察的状态 */
class MutableSpeedState {
    var isStart by mutableStateOf(false)
    var inputCount by mutableStateOf(0)
    var correctCount by mutableStateOf(0F)
    var wrongCount by mutableStateOf(0)
    var time: LocalTime by mutableStateOf(LocalTime.parse("00:00:00", DateTimeFormatter.ofPattern("HH:mm:ss")))
    var timer by mutableStateOf(Timer())
    var autoPauseTimer by mutableStateOf(Timer())
}

/** 序列化配置 */
private val encodeBuilder = Json {
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
            file = File("resources/macos/$path")
        }
        file
    }
}

fun getAudioDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val audioDir = File(homeDir, ".typing-learner/audio")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }
    return audioDir
}

/** 获取应用程序的配置文件的目录 */
fun getSettingsDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val applicationDir = File(homeDir, ".typing-learner")
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

/** 获取抄写文本的配置文件 */
private fun getTextSettingsFile():File{
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingTextSettings.json")
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