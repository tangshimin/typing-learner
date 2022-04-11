import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import components.*
import components.flatlaf.UpdateFlatLaf
import data.Caption
import data.loadMutableVocabulary
import data.loadVocabulary
import data.saveVocabulary
import dialog.AboutDialog
import dialog.GenerateVocabulary
import dialog.SelectChapterDialog
import data.VocabularyType
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import state.getResourcesFile
import state.rememberAppState
import theme.DarkColorScheme
import theme.LightColorScheme
import java.awt.Component
import java.awt.Rectangle
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(ExperimentalComposeUiApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalSerializationApi::class
)
fun main() = application {
    var isOpen by remember { mutableStateOf(true) }
    val state = rememberAppState()
    UpdateFlatLaf(state.typing.darkTheme, state)

    CompositionLocalProvider(
        LocalMediaPlayerComponent provides rememberMediaPlayerComponent(),
        LocalCtrl provides rememberCtrl()
    ) {
        val mediaPlayerComponent = LocalMediaPlayerComponent.current
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Maximized,
            size = DpSize(1030.dp, 862.dp),
        )
        var title = if (state.isDictation) {
            if (state.isReviewWrongList) {
                "复习错误单词 - ${state.dictationIndex + 1}"
            } else "默写模式 - ${state.dictationIndex + 1}"
        } else {
            "${state.typing.index + 1}"
        }
        title = "${state.typing.vocabularyName} - $title"
        if (isOpen) {
            Window(
                title = if(isMacOS()) "" else title ,
                icon = painterResource("logo/logo.svg"),
                state = windowState,
                onCloseRequest = {
                    isOpen = false
                    mediaPlayerComponent.mediaPlayer().release()
                },
            ) {
                MaterialTheme(colors = if (state.typing.darkTheme) DarkColorScheme else LightColorScheme) {
                    WindowMenuBar(state)
                    MenuDialogs(state)
                    // 视频播放器的位置，大小
                    val videoBounds = computeVideoBounds(windowState,state.openSettings)
                    Box(Modifier.background(
                        MaterialTheme.colors.background
                    ).onPreviewKeyEvent {
                        globalShortcuts(
                            it,
                            state,
                            mediaPlayerComponent,
                            videoBounds,
                        )
                    }
                    ) {

                        Row {
                            TypingSidebar(state)
                            if (state.openSettings) {
                                Divider(Modifier.fillMaxHeight().width(1.dp))
                            }
                            Box(Modifier.fillMaxSize()) {
                                val endPadding = 0.dp

                                Typing(
                                    state = state,
                                    videoBounds = videoBounds,
                                    modifier = Modifier.align(Alignment.Center)
                                        .padding(end = endPadding)
                                )
                                val speedAlignment = Alignment.TopEnd
                                Speed(
                                    state = state,
                                    modifier = Modifier
                                        .width(IntrinsicSize.Max)
                                        .align(speedAlignment)
                                        .padding(end = endPadding)
                                )


                            }
                        }
                        Settings(state, modifier = Modifier.align(Alignment.TopStart))
                    }
                }
            }

        }
    }

}

/**
 * 菜单栏
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
private fun FrameWindowScope.WindowMenuBar(state: AppState) = MenuBar {
    Menu("词库(V)", mnemonic = 'V') {
        Item("选择词库", onClick = {
            state.loadingFileChooserVisible = true
            Thread(Runnable {
                val fileChooser =  state.futureFileChooser.get()
                fileChooser.dialogTitle = "选择词库"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = getResourcesFile("vocabulary")
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile
                    state.vocabulary = loadMutableVocabulary(file.absolutePath)
                    state.typing.vocabularyName = file.nameWithoutExtension
                    state.typing.vocabularyPath = file.absolutePath
                    state.typing.chapter = 1
                    state.typing.index = 0
                    state.saveTypingState()
                    state.loadingFileChooserVisible = false
                } else {
                    state.loadingFileChooserVisible = false
                }
            }).start()

        }, shortcut = KeyShortcut(Key.O, ctrl = true)
        )


        Item("从文档生成词库", onClick = {
            state.generateVocabularyFromDocument = true
        })
        Item("从字幕生成词库", onClick = {
            state.generateVocabularyFromSubtitles = true
        })
        Item("从 MKV 生成词库", onClick = {
            state.generateVocabularyFromMKV = true
        })
    }
    Menu("字幕(S)",mnemonic = 'S'){
        var showImportVocabulary by remember { mutableStateOf(false) }
        if (showImportVocabulary) {
            ImportVocabularyDialog(
                state = state,
                close = {
                    showImportVocabulary = false
                }
            )
        }
        // TODO 这个逻辑有缺陷，应用程序没有要求用户必须保存到字幕文件夹，也没有明显的提示
        val enableImportVocabulary = state.vocabulary.type == VocabularyType.DOCUMENT && subtitleDirectoryIsNotEmpty()
        // 当前词库类型为文档，同时字幕文件夹有文件
        Item(
            "导入字幕",
            enabled = enableImportVocabulary,
            onClick = { showImportVocabulary = true },
        )

    }
    Menu("章节(C)", mnemonic = 'C') {
        Item(
            "选择章节",
            onClick = {
                state.openSelectChapter = true
            },
        )
    }
    var aboutDialogVisible by remember { mutableStateOf(false) }
    Menu("帮助(H)", mnemonic = 'H') {
        Item("检查更新(U)", mnemonic = 'U', onClick = { println("点击 检查更新") })
        Item("关于(A)", mnemonic = 'A', onClick = {aboutDialogVisible = true })
        if(aboutDialogVisible){
            AboutDialog(
                close = {aboutDialogVisible = false}
            )
        }

    }
}

/**
 * 设置
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun Settings(state: AppState, modifier: Modifier) {
    Box(modifier = modifier) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(if (state.openSettings) 217.dp else 48.dp)
                    .shadow(
                        elevation = if (state.openSettings) 0.dp else 0.dp,
                        shape = if (state.openSettings) RectangleShape else RoundedCornerShape(50)
                    )
                    .background(MaterialTheme.colors.background)
                    .clickable {
                        state.openSettings = !state.openSettings
                    }) {

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val ctrl = LocalCtrl.current
                            Text(text = "设置 $ctrl+1", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {

                    Icon(
                        if (state.openSettings) Icons.Filled.ArrowBack else Icons.Filled.Tune,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable { state.openSettings = !state.openSettings }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )

                }

                if (state.openSettings) {
                    Divider(Modifier.height(48.dp).width(1.dp))
                }
            }

        }
    }
}

val LocalCtrl = staticCompositionLocalOf<String> {
    error("LocalCtrl isn't provided")
}

/**
 * 本地的 Ctrl 键
 */
@Composable
fun rememberCtrl(): String = remember {
    if (isMacOS()) "⌃" else "Ctrl"
}

/**
 * 全局快捷键
 */
@OptIn(ExperimentalSerializationApi::class)
@ExperimentalComposeUiApi
fun globalShortcuts(
    it: KeyEvent,
    state: AppState,
    mediaPlayerComponent: Component,
    videoBounds: Rectangle,
): Boolean {
    return when {
        (it.isCtrlPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
            state.typing.isAuto = !state.typing.isAuto
            true
        }
        (it.isCtrlPressed && it.key == Key.D && it.type == KeyEventType.KeyUp) -> {
            state.typing.darkTheme = !state.typing.darkTheme
            true
        }
        (it.isCtrlPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
            state.typing.phoneticVisible = !state.typing.phoneticVisible
            true
        }
        (it.isCtrlPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
            state.typing.morphologyVisible = !state.typing.morphologyVisible
            true
        }
        (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
            state.typing.definitionVisible = !state.typing.definitionVisible
            true
        }
        (it.isCtrlPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
            state.typing.translationVisible = !state.typing.translationVisible
            true
        }
        (it.isCtrlPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
            state.typing.wordVisible = !state.typing.wordVisible
            true
        }

        (it.isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
            val word = state.getCurrentWord()
            playAudio(
                word = word.value,
                pronunciation = state.typing.pronunciation,
                mediaPlayerComponent = mediaPlayerComponent,
                changePlayerState = {}
            )
            true
        }
        (it.isCtrlPressed && it.isShiftPressed && it.key == Key.Z && it.type == KeyEventType.KeyUp) -> {
            if (state.vocabulary.type == VocabularyType.DOCUMENT) {
                val playTriple = getCaption(state, 0)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)
            } else {
                val caption = state.getCurrentWord().captions[0]
                val playTriple = Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)
            }
            true
        }
        (it.isCtrlPressed && it.isShiftPressed && it.key == Key.X && it.type == KeyEventType.KeyUp) -> {
            if (state.getCurrentWord().links.size >= 2) {
                val playTriple = getCaption(state, 1)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)

            } else if (state.getCurrentWord().captions.size >= 2) {
                val caption = state.getCurrentWord().captions[1]
                val playTriple = Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)
            }
            true
        }
        (it.isCtrlPressed && it.isShiftPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
            if (state.getCurrentWord().links.size >= 3) {
                val playTriple = getCaption(state, 2)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)
            } else if (state.getCurrentWord().captions.size >= 3) {
                val caption = state.getCurrentWord().captions[2]
                val playTriple = Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                shortcutPlay(state,  playTriple, mediaPlayerComponent, videoBounds)
            }
            true
        }
        (it.isCtrlPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
            state.typing.subtitlesVisible = !state.typing.subtitlesVisible
            true
        }

        (it.isCtrlPressed && it.key == Key.M && it.type == KeyEventType.KeyUp) -> {
            state.typing.keystrokeSound = !state.typing.keystrokeSound
            true
        }
        (it.isCtrlPressed && it.key == Key.W && it.type == KeyEventType.KeyUp) -> {
            state.typing.soundTips = !state.typing.soundTips
            true
        }
        (it.isCtrlPressed && it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
            state.openSettings = !state.openSettings
            true
        }
        (it.isCtrlPressed && it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
            state.typing.speedVisible = !state.typing.speedVisible
            true
        }
        (it.isCtrlPressed && it.isShiftPressed && it.key == Key.Spacebar && it.type == KeyEventType.KeyUp) -> {
            reset(state)
            true
        }

        (it.isCtrlPressed && it.key == Key.Spacebar && it.type == KeyEventType.KeyUp) -> {
            startTimer(state)
            true
        }

        else -> false
    }
}

/**
 * 用快捷键播放视频
 * @param state 应用程序的状态
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 * @param mediaPlayerComponent 视频播放组件
 * @param videoBounds 视频播放器的位置和大小
 */
@OptIn(ExperimentalSerializationApi::class)
private fun shortcutPlay(
    state: AppState,
    playTriple: Triple<Caption, String, Int>?,
    mediaPlayerComponent: Component,
    videoBounds: Rectangle
) {
    if (playTriple != null) {
        if (!state.isPlaying) {
            val file = File(playTriple.second)
            if (file.exists()) {
                state.isPlaying = true
                Thread(Runnable {
                    play(
                        window = state.videoPlayerWindow,
                        setIsPlaying = { state.isPlaying = it },
                        state.typing.audioVolume,
                        playTriple,
                        mediaPlayerComponent,
                        videoBounds
                    )
                }).start()
            }
        } else {
            println("通知用户，视频地址错误")
        }
    }
}

/**
 * 对话框
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalMaterialApi::class, ExperimentalSerializationApi::class)
@ExperimentalComposeUiApi
@Composable
fun MenuDialogs(state: AppState) {
    if (state.openSelectChapter) {
        SelectChapterDialog(state)
    }

    if(state.loadingFileChooserVisible){
        LoadingDialog()
    }
    if (state.generateVocabularyFromDocument) {
        GenerateVocabulary(
            state = state,
            title = "从文档生成词库",
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.generateVocabularyFromSubtitles) {
        GenerateVocabulary(
            state = state,
            title = "从字幕生成词库",
            type = VocabularyType.SUBTITLES
        )
    }

    if (state.generateVocabularyFromMKV) {
        GenerateVocabulary(
            state = state,
            title = "从 MKV 生成词库",
            type = VocabularyType.MKV
        )
    }
}


/**
 * 等待窗口
 */
@Composable
fun LoadingDialog() {
    Dialog(
        title = "正在加载文件选择器",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = {},
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(300.dp,300.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(Modifier.width(300.dp).height(300.dp)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * 链接字幕词库窗口
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
fun ImportVocabularyDialog(
    state: AppState,
    close: () -> Unit
) {
    /**
     * 预览的单词列表
     */
    val previewWords = remember { mutableStateListOf<Pair<String, List<Caption>>>() }

    /**
     * 当前词库链接到字幕词库的字幕的数量
     */
    var linkCounter by remember { mutableStateOf(0) }

    /**
     * 准备链接的单词和字幕
     */
    val prepareLinks = remember { mutableStateMapOf<String, List<String>>() }

    /**
     * 视频地址
     */
    var relateVideoPath by remember { mutableStateOf("") }

    /**
     * 字幕轨道 ID
     */
    var subtitlesTrackId by remember { mutableStateOf(0) }
    var vocabularyType by remember { mutableStateOf(VocabularyType.DOCUMENT) }
    var vocabularyTypeWrong by remember { mutableStateOf(false) }
    var extractCaptionResultInfo by remember { mutableStateOf("") }

    /**
     * 点击【导入】后执行的回调函数
     */
    val import: () -> Unit = {
        if (prepareLinks.isNotEmpty()) {
            state.vocabulary.wordList.forEach { word ->
                val links = prepareLinks[word.value]
                if (!links.isNullOrEmpty()) {
                    word.links.addAll(links)
                }
            }
            saveVocabulary(state.vocabulary.vocabulary, state.typing.vocabularyPath)
        }
    }
    val clear: () -> Unit = {
        previewWords.clear()
        linkCounter = 0
        prepareLinks.clear()
        relateVideoPath = ""
        extractCaptionResultInfo = ""
        subtitlesTrackId = 0
        vocabularyTypeWrong = false
        vocabularyType = VocabularyType.DOCUMENT

    }

    /**
     * 用户选择字幕文件后，用这个函数提取相关信息
     */
    val extractCaption: (File) -> Unit = {
        Thread(Runnable {
            val selectedVocabulary = loadVocabulary(it.absolutePath)
            if (selectedVocabulary.type != VocabularyType.DOCUMENT) {
                relateVideoPath = selectedVocabulary.relateVideoPath
                subtitlesTrackId = selectedVocabulary.subtitlesTrackId
                vocabularyType = selectedVocabulary.type
                var linkedCounter = 0
                val wordCaptionsMap = HashMap<String, List<Caption>>()
                selectedVocabulary.wordList.forEach { word ->
                    wordCaptionsMap.put(word.value, word.captions)
                }
                state.vocabulary.wordList.forEach { word ->
                    if (wordCaptionsMap.containsKey(word.value.lowercase(Locale.getDefault()))) {
                        val captions = wordCaptionsMap.get(word.value)
                        val links = mutableListOf<String>()
                        // 用于预览
                        val previewCaptionsList = mutableListOf<Caption>()
                        // 字幕最多3条，这个 counter 是剩余的数量
                        var counter = 3 - word.links.size
                        if (counter in 1..3) {
                            captions?.forEachIndexed { index, caption ->
                                val link =
                                    "(${it.absolutePath})[${selectedVocabulary.relateVideoPath}][${selectedVocabulary.subtitlesTrackId}][$index]"
                                if (counter != 0) {
                                    if (!word.links.contains(link)) {
                                        links.add(link)
                                        previewCaptionsList.add(caption)
                                        counter--
                                    } else {
                                        linkedCounter++
                                    }
                                }
                            }
                        } else {

                            // 字幕已经有3条了，查询是否有一样的
                            captions?.forEachIndexed { index, _ ->
                                val link =
                                    "(${it.absolutePath})[${selectedVocabulary.relateVideoPath}][${selectedVocabulary.subtitlesTrackId}][$index]"
                                if (word.links.contains(link)) {
                                    linkedCounter++
                                }
                            }
                        }
                        if (links.isNotEmpty()) {
                            prepareLinks.put(word.value, links)
                            previewWords.add(Pair(word.value, previewCaptionsList))
                            linkCounter += previewCaptionsList.size
                        }

                    }
                }
                // previewWords isEmpty 有两种情况：
                // 1. 已经导入了一次。
                // 2. 没有匹配的字幕
                if (previewWords.isEmpty()) {
                    extractCaptionResultInfo = if (linkedCounter == 0) {
                        "没有匹配的字幕，请重新选择"
                    } else {
                        "${selectedVocabulary.name} 有${linkedCounter}条相同的字幕已经导入，请重新选择"
                    }
                }

            } else {
                vocabularyTypeWrong = true
            }
        }).start()
    }

    Dialog(
        title = "导入字幕",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = {
            clear()
            close()
        },
        undecorated = !MaterialTheme.colors.isLight,
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, (if (MaterialTheme.colors.isLight) 640 else 600).dp)
        ),
    ) {

        WindowDraggableArea {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
            ) {
                CompositionLocalProvider(
                    LocalMediaPlayerComponent provides rememberMediaPlayerComponent(),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (previewWords.isEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize().align(Alignment.Center)
                            ) {
                                val captionPattern: Pattern =
                                    Pattern.compile("\\((.*?)\\)\\[(.*?)\\]\\[([0-9]*?)\\]\\[([0-9]*?)\\]")
                                val subtitlesMap = mutableMapOf<String, Int>()
                                state.vocabulary.wordList.forEach { word ->
                                    word.links.forEach { link ->
                                        val matcher = captionPattern.matcher(link)
                                        if (matcher.find()) {
                                            val subtitlesPath = matcher.group(1)
                                            var counter = subtitlesMap[subtitlesPath]
                                            if (counter == null) {
                                                subtitlesMap[subtitlesPath] = 1
                                            } else {
                                                counter++
                                                subtitlesMap[subtitlesPath] = counter
                                            }
                                        }
                                    }
                                }
                                if (subtitlesMap.isNotEmpty()) {
                                    Column(Modifier.width(IntrinsicSize.Max)) {

                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        ) { Text("已导入列表") }
                                        Divider()
                                        Column {
                                            var showConfirmationDialog by remember { mutableStateOf(false) }
                                            subtitlesMap.forEach { (path, count) ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val name = File(path).nameWithoutExtension
                                                    Text(
                                                        text = name,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(250.dp).padding(end = 10.dp)
                                                    )
                                                    Text("$count", modifier = Modifier.width(60.dp))
                                                    IconButton(onClick = { showConfirmationDialog = true }) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "",
                                                            tint = MaterialTheme.colors.onBackground
                                                        )
                                                    }
                                                    if (showConfirmationDialog) {
                                                        ConfirmationDelete(
                                                            message = "确定要删除 ${name} 的所有字幕吗?",
                                                            confirm = {
                                                                state.vocabulary.wordList.forEach { word ->
                                                                    val tempLinks = mutableListOf<String>()
                                                                    word.links.forEach { link ->
                                                                        val matcher = captionPattern.matcher(link)
                                                                        if (matcher.find()) {
                                                                            val subtitlesPath = matcher.group(1)
                                                                            if (subtitlesPath == path) {
                                                                                tempLinks.add(link)
                                                                            }
                                                                        }
                                                                    }
                                                                    word.links.removeAll(tempLinks)
                                                                }
                                                                showConfirmationDialog = false

                                                                saveVocabulary(
                                                                    state.vocabulary.vocabulary,
                                                                    state.typing.vocabularyPath
                                                                )
                                                            },
                                                            close = { showConfirmationDialog = false }
                                                        )
                                                    }

                                                }
                                            }
                                        }

                                        Divider()
                                    }
                                }
                                if (vocabularyTypeWrong) {
                                    Text(
                                        "词库的类型错误，请选择从 SRT 或 MKV 生成的词库文件",
                                        color = Color.Red,
                                        modifier = Modifier.width(360.dp).padding(top = 20.dp, bottom = 20.dp)
                                    )
                                }
                                if (extractCaptionResultInfo.isNotEmpty()) {
                                    Text(
                                        text = extractCaptionResultInfo,
                                        color = Color.Red,
                                        modifier = Modifier.width(360.dp).padding(top = 20.dp, bottom = 20.dp)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(onClick = {
                                        state.loadingFileChooserVisible = true
                                        Thread(Runnable {
                                            val fileChooser =  state.futureFileChooser.get()
                                            fileChooser.dialogTitle = "选择有字幕的词库"
                                            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                            fileChooser.currentDirectory = getResourcesFile("vocabulary")
                                            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                            fileChooser.selectedFile = null
                                            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                                                val file = fileChooser.selectedFile
                                                extractCaption(file)
                                                state.loadingFileChooserVisible = false
                                            } else {
                                                state.loadingFileChooserVisible = false
                                            }
                                        }).start()
                                    }) {
                                        Text("选择文件")
                                    }
                                    Spacer(Modifier.width(20.dp))
                                    OutlinedButton(onClick = {
                                        clear()
                                        close()
                                    }) {
                                        Text("取消")
                                    }
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize().align(Alignment.Center)) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)
                                ) {
                                    Text("总共${previewWords.size}个单词,${linkCounter}条字幕")
                                }
                                Divider()
                                Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                                    val scrollState = rememberLazyListState()
                                    LazyColumn(Modifier.fillMaxSize(), scrollState) {

                                        items(previewWords) { (word, captions) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start,
                                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                                                    .padding(start = 10.dp, end = 10.dp)
                                            ) {

                                                Text(text = word, modifier = Modifier.width(150.dp))
                                                Divider(Modifier.width(1.dp).fillMaxHeight())
                                                val mediaPlayerComponent = LocalMediaPlayerComponent.current
                                                Column(verticalArrangement = Arrangement.Center) {
                                                    captions.forEachIndexed { index, caption ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = "${index + 1}. ${caption.content}",
                                                                modifier = Modifier.padding(5.dp)
                                                            )
                                                            val playTriple =
                                                                Triple(caption, relateVideoPath, subtitlesTrackId)
                                                            val playerBounds by remember {
                                                                mutableStateOf(
                                                                    Rectangle(
                                                                        0,
                                                                        0,
                                                                        540,
                                                                        303
                                                                    )
                                                                )
                                                            }
                                                            if (vocabularyType != VocabularyType.DOCUMENT) {
                                                                IconButton(
                                                                    onClick = {},
                                                                    modifier = Modifier
                                                                        .onPointerEvent(PointerEventType.Press) {
                                                                            val location =
                                                                                it.awtEventOrNull?.locationOnScreen
                                                                            if (location != null) {
                                                                                playerBounds.x = location.x - 270 + 24
                                                                                playerBounds.y = location.y - 320

                                                                                val file = File(relateVideoPath)
                                                                                if (file.exists()) {
                                                                                    Thread(Runnable {
                                                                                        play(
                                                                                            window = state.videoPlayerWindow,
                                                                                            setIsPlaying = {},
                                                                                            volume = state.typing.audioVolume,
                                                                                            playTriple = playTriple,
                                                                                            mediaPlayerComponent= mediaPlayerComponent,
                                                                                            bounds =playerBounds
                                                                                        )
                                                                                    }).start()
                                                                                }
                                                                            }
                                                                        }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.PlayArrow,
                                                                        contentDescription = "Localized description",
                                                                        tint = MaterialTheme.colors.primary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                }
                                            }
                                            Divider()
                                        }
                                    }
                                    VerticalScrollbar(
                                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                        style = LocalScrollbarStyle.current.copy(shape = RectangleShape)
                                    )
                                }

                                Divider()
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp)
                                ) {
                                    OutlinedButton(onClick = {
                                        import()
                                        clear()
                                    }) {
                                        Text("导入")
                                    }
                                    Spacer(Modifier.width(20.dp))
                                    OutlinedButton(onClick = { clear() }) {
                                        Text("取消")
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }
    }
}

/**
 * 是否有字幕词库
 */
fun subtitleDirectoryIsNotEmpty(): Boolean {
    val file = getResourcesFile("vocabulary/字幕")
    return if (file != null) {
        file.listFiles().isNotEmpty()
    } else false

}