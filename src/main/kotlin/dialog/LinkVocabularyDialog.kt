package dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import components.ConfirmationDelete
import components.play
import data.Caption
import data.VocabularyType
import data.loadVocabulary
import data.saveVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import player.LocalMediaPlayerComponent
import player.rememberMediaPlayerComponent
import state.AppState
import state.getResourcesFile
import java.awt.Rectangle
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

/**
 * 链接字幕词库窗口
 * 把字幕词库链接到文档词库
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
fun LinkVocabularyDialog(
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