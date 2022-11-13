package state

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.formdev.flatlaf.FlatLightLaf
import ui.flatlaf.initializeFileChooser
import data.getHardVocabularyFile
import data.loadMutableVocabulary
import data.loadMutableVocabularyByName
import dialog.RecentItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.createMediaPlayerComponent
import player.isMacOS
import player.isWindows
import theme.createColors
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.FutureTask
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane

/** 所有界面共享的状态 */
@ExperimentalSerializationApi
class AppState {

    /** 全局状态里需要持久化的状态 */
    var global: GlobalState = loadGlobalState()

    /** Material 颜色 */
    var colors by mutableStateOf(createColors(global.isDarkTheme, global.primaryColor))

    /** 视频播放窗口 */
    var videoPlayerWindow = createVideoPlayerWindow()

    /** VLC 视频播放组件 */
    var videoPlayerComponent = createMediaPlayerComponent()

    /** 文件选择器，如果不提前加载反应会很慢 */
    var futureFileChooser: FutureTask<JFileChooser> = initializeFileChooser(global.isDarkTheme)

    /** 困难词库 */
    var hardVocabulary = loadMutableVocabularyByName("HardVocabulary")

    /** 最近生成的词库列表 */
    var recentList = readRecentList()

    /** 打开设置 */
    var openSettings by mutableStateOf(false)

    /** 是否显示等待窗口 */
    var loadingFileChooserVisible by mutableStateOf(false)

    /** 是否显示【合并词库】窗口 */
    var mergeVocabulary by mutableStateOf(false)

    /** 是否显示【过滤词库】窗口 */
    var filterVocabulary by mutableStateOf(false)

    /** 是否显示【导入词库到熟悉词库】窗口 */
    var importFamiliarVocabulary by mutableStateOf(false)

    /** 是否显示【从文档生成词库】窗口 */
    var generateVocabularyFromDocument by mutableStateOf(false)

    /** 是否显示【从字幕文件生成词库】窗口 */
    var generateVocabularyFromSubtitles by mutableStateOf(false)

    /** 是否显示【从 MKV 生成词库】 窗口 */
    var generateVocabularyFromMKV by mutableStateOf(false)

    /** 显示软件更新对话框 */
    var showUpdateDialog by mutableStateOf(false)

    /** 软件的最新版本 */
    var latestVersion by mutableStateOf("")

    /** 版本说明 **/
    var releaseNote by mutableStateOf("")

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
                    global.wordTextStyle,
                    global.detailTextStyle,
                    global.letterSpacing.value,
                    global.position.x.value,
                    global.position.y.value,
                    global.size.width.value,
                    global.size.height.value,
                    global.placement,
                    global.autoUpdate,
                    global.ignoreVersion,
                    global.bncNum,
                    global.frqNum
                )
                val json = encodeBuilder.encodeToString(globalData)
                val settings = getGlobalSettingsFile()
                settings.writeText(json)
            }
        }
    }

    /** 改变词库 */
    fun changeVocabulary(
        vocabularyFile: File,
        typingWord: WordState,
        index: Int
    ) {
        val newVocabulary = loadMutableVocabulary(vocabularyFile.absolutePath)
        if(newVocabulary.wordList.size>0){

            // 把困难词库的索引保存在 typingWord.
            if(typingWord.vocabulary.name == "HardVocabulary"){
                typingWord.hardVocabularyIndex = typingWord.index
            }else{
                // 保存当前词库的索引到最近列表,
                saveToRecentList(typingWord.vocabulary.name, typingWord.vocabularyPath,typingWord.index)
            }

            typingWord.vocabulary = newVocabulary
            typingWord.vocabularyName = vocabularyFile.nameWithoutExtension
            typingWord.vocabularyPath = vocabularyFile.absolutePath
            typingWord.chapter = (index / 20) + 1
            typingWord.index = index
            vocabularyChanged = true
            typingWord.saveTypingWordState()
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
        return list.toMutableStateList()
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

    fun removeRecentItem(recentItem: RecentItem) {
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
        val set = mutableSetOf<String>()
        audioDir.list()?.let { set.addAll(it) }
        return set
    }

    val changeTheme: (Boolean) -> Unit = {
        runBlocking {
            launch {
                global.isDarkTheme = it
                colors = createColors(global.isDarkTheme, global.primaryColor)
                saveGlobalState()
            }
        }

    }

    val backToHome: () -> Unit = {
        runBlocking {
            launch {
                global.type = TypingType.WORD
                saveGlobalState()
            }
        }
    }

    /** 搜索 */
    var searching by  mutableStateOf(false)
    /** 打开搜索 **/
    val openSearch:() -> Unit = {
        searching = true
    }

    val openLoadingDialog:() -> Unit = {
        if(isWindows()) {
            loadingFileChooserVisible = true
        }
    }

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