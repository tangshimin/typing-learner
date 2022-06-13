import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import components.TypingSubtitles
import components.TypingWord
import components.computeVideoBounds
import components.flatlaf.UpdateFlatLaf
import data.VocabularyType
import dialog.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import state.TypingType.*
import state.computeFontSize
import state.getResourcesFile
import state.rememberAppState
import theme.createColors
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalSerializationApi::class
)
fun main() = application {
    var isOpen by remember { mutableStateOf(true) }
    val state = rememberAppState()
    UpdateFlatLaf(state.global.isDarkTheme, state)
    CompositionLocalProvider(
        LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
        LocalCtrl provides rememberCtrl(),
        LocalTextSelectionColors provides textSelectionColors()
    ) {
        val audioPlayerComponent = LocalAudioPlayerComponent.current
        val close: () -> Unit = {
            isOpen = false
            audioPlayerComponent.mediaPlayer().release()
            state.videoPlayerComponent.mediaPlayer().release()
        }
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Maximized,
            size = DpSize(1030.dp, 862.dp),
        )

        val title = computeTitle(state)
        if (isOpen) {
            Window(
                title = title,
                icon = painterResource("logo/logo.png"),
                state = windowState,
                onCloseRequest = {close() },
            ) {
                MaterialTheme(colors = state.colors) {
                    state.global.fontSize = computeFontSize(state.global.textStyle)
                    WindowMenuBar(
                        state = state,
                        close = {close()}
                    )
                    MenuDialogs(state)

                    when (state.global.type) {
                        WORD -> {
                            // 显示器缩放
                            val density = LocalDensity.current.density
                            // 视频播放器的位置，大小
                            val videoBounds = computeVideoBounds(windowState, state.openSettings,density)
                            TypingWord(
                                window = window,
                                title = title,
                                state = state,
                                audioPlayer = audioPlayerComponent,
                                videoBounds = videoBounds
                            )
                        }
                        SUBTITLES -> {
                            val scope = rememberCoroutineScope()
                            var typingSubtitles = state.typingSubtitles
                            TypingSubtitles(
                                typingSubtitles = typingSubtitles,
                                globalState = state.global,
                                saveSubtitlesState = {
                                    scope.launch {
                                        state.saveTypingSubtitlesState()
                                    }
                                },
                                saveGlobalState = {
                                    scope.launch {
                                        state.saveGlobalState()
                                    }
                                },
                                saveIsDarkTheme = {
                                    scope.launch {
                                        state.global.isDarkTheme = it
                                        state.colors = createColors(state.global.isDarkTheme, state.global.primaryColor)
                                        state.saveGlobalState()
                                    }
                                },
                                toTypingWord = {
                                    scope.launch {
                                        state.global.type = WORD
                                        state.saveGlobalState()
                                    }
                                },
                                isOpenSettings = state.openSettings,
                                setIsOpenSettings = { state.openSettings = it },
                                window = window,
                                title = title,
                                playerWindow = state.videoPlayerWindow,
                                videoVolume = state.global.videoVolume,
                                mediaPlayerComponent = state.videoPlayerComponent,
                                futureFileChooser = state.futureFileChooser,
                                closeLoadingDialog = { state.loadingFileChooserVisible = false },
                            )
                        }

                    }

                }
            }

        }
    }

}


@OptIn(ExperimentalSerializationApi::class)
private fun computeTitle(state: AppState): String {
    when (state.global.type) {
        WORD -> {
            return if (state.vocabulary.wordList.isNotEmpty()) {
                val suffix = if (state.isDictation) {
                    if (state.isReviewWrongList) {
                        "复习错误单词 - ${state.dictationIndex + 1}"
                    } else "默写模式 - ${state.dictationIndex + 1}"
                } else {
                    "${state.typingWord.index + 1}"
                }
                "${state.typingWord.vocabularyName} - $suffix"
            } else {
                "请选择词库"
            }
        }
        SUBTITLES -> {
            val fileName = File(state.typingSubtitles.mediaPath).nameWithoutExtension
            return fileName + " - " + state.typingSubtitles.trackDescription
        }
        else -> {
            return "Anki"
        }
    }

}

/**
 * 菜单栏
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
private fun FrameWindowScope.WindowMenuBar(
    state: AppState,
    close: () -> Unit,
) = MenuBar {
    Menu("词库(V)", mnemonic = 'V') {
        Item("打开词库(O)", mnemonic = 'O', onClick = {
            if(isWindows()) {
                state.loadingFileChooserVisible = true
            }
            Thread(Runnable {
                val fileChooser = state.futureFileChooser.get()
                fileChooser.dialogTitle = "选择词库"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = getResourcesFile("vocabulary")
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile
                    val index = state.findVocabularyIndex(file)
                    state.changeVocabulary(file,index)
                    state.global.type = WORD
                    state.saveGlobalState()
                    state.loadingFileChooserVisible = false
                } else {
                    state.loadingFileChooserVisible = false
                }
            }).start()

        })

        Menu("打开最近词库(R)",enabled = state.recentList.isNotEmpty(), mnemonic = 'R') {
            for (i in 0 until state.recentList.size){
                val recentItem = state.recentList.getOrNull(i)
                if(recentItem!= null){
                    val recentFile = File(recentItem.path)
                    if (recentFile.exists()) {
                        Item(text = recentItem.name, onClick = {
                            val file = File(recentItem.path)
                            state.changeVocabulary(file,recentItem.index)
                            state.global.type = WORD
                            state.saveGlobalState()
                            state.loadingFileChooserVisible = false
                        })
                    } else {
                        state.removeInvalidRecentItem(recentItem)
                    }
                }
            }
        }

        Separator()
        Item("合并词库(M)", mnemonic = 'M', onClick = {
            state.mergeVocabulary = true
        })
        Item("过滤词库(F)", mnemonic = 'F', onClick = {
            state.filterVocabulary = true
        })
        Separator()
        Item("从文档生成词库(D)", mnemonic = 'D', onClick = {
            state.generateVocabularyFromDocument = true
        })
        Item("从字幕生成词库(C)", mnemonic = 'C', onClick = {
            state.generateVocabularyFromSubtitles = true
        })
        Item("从 MKV 视频生成词库(V)", mnemonic = 'V', onClick = {
            state.generateVocabularyFromMKV = true
        })
        Separator()
        var showSettingsDialog by remember { mutableStateOf(false) }
        Item("设置(S)", mnemonic = 'S', onClick = { showSettingsDialog = true })
        if(showSettingsDialog){
            SettingsDialog(
                close = {showSettingsDialog = false},
                state = state
            )
        }
        Separator()
        Item("退出(X)", mnemonic = 'X', onClick = { close() })
    }
    Menu("字幕(S)", mnemonic = 'S') {
        val enableTypingSubtitles = (state.global.type == WORD)
        Item(
            "抄写字幕(T)", mnemonic = 'T',
            enabled = enableTypingSubtitles,
            onClick = {
                state.global.type = SUBTITLES
            },
        )
        var showLinkVocabulary by remember { mutableStateOf(false) }
        if (showLinkVocabulary) {
            LinkVocabularyDialog(
                state = state,
                close = {
                    showLinkVocabulary = false
                }
            )
        }
        //如果当前词库类型为文档就启用
        val enableLinkVocabulary = (state.vocabulary.type == VocabularyType.DOCUMENT && state.global.type == WORD)
        Item(
            "链接字幕词库(L)", mnemonic = 'L',
            enabled = enableLinkVocabulary,
            onClick = { showLinkVocabulary = true },
        )
    }
    Menu("章节(C)", mnemonic = 'C') {
        val enable = state.global.type == WORD
        Item(
            "选择章节(C)", mnemonic = 'C',
            enabled = enable,
            onClick = {
                state.openSelectChapter = true
            },
        )
    }
    var aboutDialogVisible by remember { mutableStateOf(false) }
    var donateDialogVisible by remember { mutableStateOf(false) }
    var helpDialogVisible by remember { mutableStateOf(false) }
    Menu("帮助(H)", mnemonic = 'H') {
//        Item("检查更新(U)", mnemonic = 'U', onClick = { println("点击 检查更新") })
        Item("帮助文档(H)", mnemonic = 'H', onClick = { helpDialogVisible = true})
        if(helpDialogVisible){
            HelpDialog(
                close = {helpDialogVisible = false}
            )
        }
        Item("捐赠", onClick = { donateDialogVisible = true })
        if(donateDialogVisible){
            DonateDialog (
                close = {donateDialogVisible = false}
            )
        }
        Item("关于(A)", mnemonic = 'A', onClick = { aboutDialogVisible = true })
        if (aboutDialogVisible) {
            AboutDialog(
                close = { aboutDialogVisible = false }
            )
        }

    }
}

/**
 * 设置
 */
@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun Settings(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 30.dp else 0.dp).background(MaterialTheme.colors.background))
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(if (isOpen) 217.dp else 48.dp)
                    .shadow(
                        elevation = 0.dp,
                        shape = if (isOpen) RectangleShape else RoundedCornerShape(50)
                    )
                    .background(MaterialTheme.colors.background)
                    .clickable { setIsOpen(!isOpen) }) {

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val ctrl = LocalCtrl.current
                            Text(text = "侧边栏 $ctrl+1", modifier = Modifier.padding(10.dp))
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
                        if (isOpen) Icons.Filled.ArrowBack else Icons.Filled.Tune,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable { setIsOpen(!isOpen) }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )

                }

                if (isOpen) {
                    Divider(Modifier.height(48.dp).width(1.dp))
                }
            }
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
        }
    }
}

val LocalCtrl = staticCompositionLocalOf<String> {
    error("LocalCtrl isn't provided")
}

/** 本地的 Ctrl 键 */
@Composable
fun rememberCtrl(): String = remember {
    if (isMacOS()) "⌃" else "Ctrl"
}

/** 选择字符时的背景颜色 */
fun textSelectionColors(): TextSelectionColors {
    val defaultSelectionColor = Color(0xFF4286F4)
    val backgroundColor = defaultSelectionColor.copy(alpha = 0.4f)
    return TextSelectionColors(handleColor = defaultSelectionColor, backgroundColor = backgroundColor)
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

    if (state.loadingFileChooserVisible) {
        LoadingDialog()
    }
    if (state.mergeVocabulary) {
        MergeVocabularyDialog(
            futureFileChooser = state.futureFileChooser,
            saveToRecentList = { name, path ->
                state.saveToRecentList(name, path,0)
            },
            close = { state.mergeVocabulary = false })
    }
    if (state.filterVocabulary) {
        GenerateVocabularyDialog(
            state = state,
            title = "过滤词库",
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.generateVocabularyFromDocument) {
        GenerateVocabularyDialog(
            state = state,
            title = "从文档生成词库",
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.generateVocabularyFromSubtitles) {
        GenerateVocabularyDialog(
            state = state,
            title = "从字幕生成词库",
            type = VocabularyType.SUBTITLES
        )
    }

    if (state.generateVocabularyFromMKV) {
        GenerateVocabularyDialog(
            state = state,
            title = "从 MKV 视频生成词库",
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
        icon = painterResource("logo/logo.png"),
        onCloseRequest = {},
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(300.dp, 300.dp)
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
