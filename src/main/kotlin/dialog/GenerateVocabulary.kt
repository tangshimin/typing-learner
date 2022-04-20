package dialog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.FlatLaf
import com.matthewn4444.ebml.EBMLReader
import data.*
import data.Dictionary
import data.VocabularyType.*
import kotlinx.serialization.ExperimentalSerializationApi
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import player.mediaPlayer
import state.AppState
import state.composeAppResource
import state.getResourcesFile
import subtitleFile.FormatASS
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import theme.DarkColorScheme
import theme.LightColorScheme
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Point
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.text.Collator
import java.util.*
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * 生成词库
 * @param state 应用程序状态
 * @param title 标题
 * @param type 词库类型
 */
@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun GenerateVocabulary(
    state: AppState,
    title: String,
    type: VocabularyType
) {
    Dialog(
        title = title,
        onCloseRequest = {
            onCloseRequest(state, type)
        },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1360.dp, 785.dp)
        ),
    ) {
        val fileFilter = when (type) {
            DOCUMENT -> FileNameExtensionFilter(
                "支持的文件扩展(*.pdf、*.txt)",
                "pdf",
                "txt",
            )
            SUBTITLES -> FileNameExtensionFilter(
                "SRT 和 ASS 格式的字幕文件",
                "srt","ass",
            )
            MKV -> FileNameExtensionFilter(
                "mkv 格式的视频文件",
                "mkv",
            )
        }

        Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            Divider()
            Row {
                /**
                 * 摘要词库
                 */
                val summaryVocabulary = loadSummaryVocabulary()

                /**
                 * 分析之后得到的单词
                 */
                val documentWords = remember { mutableStateListOf<Word>() }

                /**
                 * 预览的单词
                 */
                val previewList = remember { mutableStateListOf<Word>() }

                /**
                 * 用于显示过滤列表
                 */
                val selectedNameList = remember { mutableStateListOf<String>() }

                /**
                 * 用于过滤单词
                 */
                val selectedPathList = remember { mutableStateListOf<String>() }

                /**
                 * 选择的文件名
                 */
                var selectedFileName by remember { mutableStateOf("") }

                /**
                 * 是否过滤 BNC 词频为0的单词
                 */
                var notBncFilter by remember { mutableStateOf(false) }

                /**
                 * 是否过滤 FRQ 词频为0的单词
                 */
                var notFrqFilter by remember { mutableStateOf(false) }

                /**
                 * 是否替换索引派生词
                 */
                var replaceToLemma by remember { mutableStateOf(false) }

                /**
                 * 从字幕生成单词 -> 相关视频的地址
                 */
                var relateVideoPath by remember { mutableStateOf("") }

                /**
                 * 字幕的轨道 ID
                 */
                var selectedTrackId by remember { mutableStateOf(0) }

                val left = ComposePanel()
                left.setContent {
                    MaterialTheme(colors = if (state.typing.isDarkTheme) DarkColorScheme else LightColorScheme) {
                        Column {
                            FilterVocabulary(
                                notBncFilter = notBncFilter,
                                setNotBncFilter = {
                                    notBncFilter = it
                                },
                                notFrqFilter = notFrqFilter,
                                setNotFrqFilter = {
                                    notFrqFilter = it
                                },
                                replaceToLemma = replaceToLemma,
                                setReplaceToLemma = {
                                    replaceToLemma = it
                                },
                            )
                            SelectTree(
                                selectedList = selectedNameList,
                                selectedListAdd = { selectedNameList.add(it) },
                                selectedListRemove = { selectedNameList.remove(it) },
                                selectedPathListAdd = { pathName ->
                                    selectedPathList.add(pathName)
                                },
                                selectedPathListRemove = {
                                    selectedPathList.remove(it)
                                }
                            )
                        }
                    }
                }
                val right = ComposePanel()
                right.setContent {
                    MaterialTheme(colors = if (state.typing.isDarkTheme) DarkColorScheme else LightColorScheme) {
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colors.background)
                        ) {
                            /**
                             * 这个 flag 有三个状态：""、"start"、"end"
                             */
                            var flag by remember { mutableStateOf("") }
                            var progress by remember { mutableStateOf(0.1f) }
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                            )
                            var progressText by remember { mutableStateOf("") }
                            val trackMap  = remember { mutableStateMapOf<Int,String>() }
                            SelectFile(
                                state = state,
                                type = type,
                                fileFilter = fileFilter,
                                setSelectFileName = {selectedFileName = it },
                                relateVideoPath = relateVideoPath,
                                setRelateVideoPath = {relateVideoPath = it},
                                trackMap = trackMap,
                                setTrackMap = {
                                    trackMap.clear()
                                    trackMap.putAll(it)
                                },
                                selectedTrackId = selectedTrackId,
                                setSelectedTrackId = {selectedTrackId = it},
                                analysis = { pathName,trackId ->
                                    flag = "start"
                                    selectedNameList.clear()
                                    documentWords.clear()
                                    progress = 0.15F
                                    Thread(Runnable() {

                                        val words = when (type) {
                                            DOCUMENT -> {
                                                readPDF(pathName = pathName,
                                                    addProgress = { progress += it },
                                                    setProgressText = { progressText = it })
                                            }
                                            SUBTITLES -> {
                                                readSRT(
                                                    pathName = pathName,
                                                    addProgress = { progress += it },
                                                    setProgressText = { progressText = it })
                                            }
                                            MKV -> {
                                                readMKV(
                                                    pathName = pathName,
                                                    trackId = trackId,
                                                    addProgress = { progress += it },
                                                    setProgressText = { progressText = it })
                                            }
                                        }


                                        words.forEach { word -> documentWords.add(word) }


                                        progress = 1F
                                        flag = "end"
                                        progress = 0F
                                    }).start()

                                })

                            Box(Modifier.fillMaxSize()) {
                                if (flag == "start") {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = animatedProgress,
                                        )
                                        Text(text = progressText, color = MaterialTheme.colors.onBackground)
                                    }
                                } else if (flag == "end") {
                                    val filteredDocumentList = filterDocumentWords(
                                        documentWords,
                                        notBncFilter,
                                        notFrqFilter,
                                        replaceToLemma
                                    )
                                    previewList.clear()
                                    val filteredList = filterSelectVocabulary(
                                        pathList = selectedPathList,
                                        filteredDocumentList = filteredDocumentList
                                    )
                                    previewList.addAll(filteredList)
                                    PreviewWords(previewList, summaryVocabulary,
                                        removeWord = {
                                            previewList.remove(it)
                                        })
                                }
                            }

                        }
                    }

                }
                val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
                splitPane.leftComponent = left
                splitPane.rightComponent = right
                splitPane.dividerLocation = 425
                val bottomPanel = ComposePanel()
                bottomPanel.setSize(Int.MAX_VALUE, 54)
                bottomPanel.setContent {
                    MaterialTheme(colors = if (state.typing.isDarkTheme) DarkColorScheme else LightColorScheme) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.background(MaterialTheme.colors.background)
                        ) {
                            Divider()
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(54.dp).fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    enabled = previewList.size > 0,
                                    onClick = {
                                        val fileChooser = state.futureFileChooser.get()
                                        fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                        fileChooser.dialogTitle = "保存词库"
                                        val directory = getResourcesFile("vocabulary")
                                        fileChooser.selectedFile = File("${directory?.absolutePath}/$selectedFileName.json")
                                        val userSelection = fileChooser.showSaveDialog(splitPane)
                                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                                            val fileToSave = fileChooser.selectedFile
                                            println(fileToSave.absolutePath)
                                            val vocabulary = Vocabulary(
                                                name = fileToSave.nameWithoutExtension,
                                                type = type,
                                                language = "english",
                                                size = previewList.size,
                                                relateVideoPath = relateVideoPath,
                                                subtitlesTrackId = selectedTrackId,
                                                wordList = previewList
                                            )
                                            saveVocabulary(vocabulary, fileToSave.absolutePath)
                                            onCloseRequest(state, type)
                                        }
                                    }) {
                                    Text("保存")
                                }
                                Spacer(Modifier.width(10.dp))
                                OutlinedButton(onClick = {
                                    onCloseRequest(state, type)
                                }) {
                                    Text("取消")
                                }
                                Spacer(Modifier.width(10.dp))
                            }
                        }

                    }
                }
                SwingPanel(
                    background = Color(MaterialTheme.colors.background.toArgb()),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    factory = {
                        JPanel().apply {
                            layout = BorderLayout()
                            add(bottomPanel, BorderLayout.SOUTH)
                            add(splitPane, BorderLayout.CENTER)
                        }
                    }
                )
            }
        }

    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun onCloseRequest(state: AppState, type: VocabularyType) {
    when (type) {
        DOCUMENT -> state.generateVocabularyFromDocument = false
        SUBTITLES -> state.generateVocabularyFromSubtitles = false
        MKV -> state.generateVocabularyFromMKV = false
    }

}

private fun getWordLemma(word: Word): String? {
    word.exchange.split("/").forEach { exchange ->
        val pair = exchange.split(":")
        if (pair[0] == "0") return pair[1]
    }
    return null
}

@Composable
fun Summary(
    list: List<Word>,
    summaryVocabulary: Map<String, List<String>>
) {

    Column(Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp,top = 5.dp, bottom = 5.dp)
        ) {
            val summary = computeSummary(list, summaryVocabulary)
            Text(
                "共 ${list.size} 词  牛津5000核心词：${summary[0]} 词  四级：${summary[1]} 词  六级：${summary[2]} 词  GRE：${summary[3]} 词",
                color = MaterialTheme.colors.onBackground
            )
        }
        Divider()
    }


}

/**
 * 计算摘要
 */
private fun computeSummary(
    list: List<Word>,
    vocabularySummary: Map<String, List<String>>
): List<Int> {
    var oxfordCount = 0
    var cet4Count = 0
    var cet6Count = 0
    var greCount = 0
    list.forEach { word ->
        if (vocabularySummary["oxford"]?.contains(word.value) == true) {
            oxfordCount++
        }
        if (vocabularySummary["cet4"]?.contains(word.value) == true) {
            cet4Count++
        }
        if (vocabularySummary["cet6"]?.contains(word.value) == true) {
            cet6Count++
        }
        if (vocabularySummary["gre"]?.contains(word.value) == true) {
            greCount++
        }
    }

    return listOf(oxfordCount, cet4Count, cet6Count, greCount)
}

/**
 * 载入摘要词库
 */
private fun loadSummaryVocabulary(): Map<String, List<String>> {

    val oxford = loadVocabulary("vocabulary/牛津核心词/The_Oxford_5000.json").wordList
    val cet4 = loadVocabulary("vocabulary/大学英语/四级.json").wordList
    val cet6 = loadVocabulary("vocabulary/大学英语/六级.json").wordList
    val gre = loadVocabulary("vocabulary/大学英语/GRE.json").wordList

    val oxfordList = oxford.map { word -> word.value }
    val cet4List = cet4.map { word -> word.value }
    val cet6List = cet6.map { word -> word.value }
    val greList = gre.map { word -> word.value }

    val map = HashMap<String, List<String>>()
    map["oxford"] = oxfordList
    map["cet4"] = cet4List
    map["cet6"] = cet6List
    map["gre"] = greList

    return map
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun FilterVocabulary(
    notBncFilter: Boolean,
    setNotBncFilter: (Boolean) -> Unit,
    notFrqFilter: Boolean,
    setNotFrqFilter: (Boolean) -> Unit,
    replaceToLemma: Boolean,
    setReplaceToLemma: (Boolean) -> Unit,
) {
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("过滤词库", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default)
        }
        Divider()
        val textWidth = 320.dp
        val textColor = MaterialTheme.colors.onBackground
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp)
        ) {


            Row {
                Text("过滤所有 ", color = MaterialTheme.colors.onBackground)

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "英国国家语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {

                    Text("BNC", color = blueColor,
                        modifier = Modifier
                            .clickable {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                        .isSupported(Desktop.Action.BROWSE)
                                ) {
                                    Desktop.getDesktop().browse(URI("https://www.natcorp.ox.ac.uk/"))
                                }
                            }
                            .pointerHoverIcon(PointerIconDefaults.Hand)
                            .padding(end = 3.dp))
                }

                Text("   语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = notBncFilter,
                onCheckedChange = { setNotBncFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp)
        ) {

            Row {
                Text("过滤所有 ", color = MaterialTheme.colors.onBackground)
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "美国当代英语语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {
                    Text("COCA", color = blueColor,
                        modifier = Modifier.clickable {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.BROWSE)
                            ) {
                                Desktop.getDesktop().browse(URI("https://www.english-corpora.org/coca/"))
                            }
                        }
                            .pointerHoverIcon(PointerIconDefaults.Hand))
                }

                Text(" 语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = notFrqFilter,
                onCheckedChange = { setNotFrqFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp)
        ) {
            Text(
                "词形还原，例如：\ndid、done、doing、does 全部替换为 do",
                fontFamily = FontFamily.Default,
                color = textColor,
                modifier = Modifier.width(textWidth)
            )
            Checkbox(
                checked = replaceToLemma,
                onCheckedChange = { setReplaceToLemma(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
    }

}

@Composable
fun SelectTree(
    selectedList: List<String>,
    selectedListAdd: (String) -> Unit,
    selectedListRemove: (String) -> Unit,
    selectedPathListAdd: (String) -> Unit,
    selectedPathListRemove: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Column {
            var selectedPath: TreePath? = null
            val vocabulary = composeAppResource("vocabulary")
            val pathNameMap = searchPaths(vocabulary)
            val tree = JTree(addNodes(null, vocabulary))

            val treeSelectionListener: TreeSelectionListener = object : TreeSelectionListener {
                override fun valueChanged(event: TreeSelectionEvent?) {
                    if (event != null) {
                        val path = event.path
                        val node = path.lastPathComponent as DefaultMutableTreeNode
                        val name = node.userObject.toString()
                        if (node.isLeaf && !selectedList.contains(name)) {
                            selectedListAdd(name)
                            // 过滤词库，要存储被过滤的单词，用于取消过滤
                            val filePath = pathNameMap[name]
                            if (filePath != null) selectedPathListAdd(filePath)
                        }
                        selectedPath = path
                    }

                }

            }

            tree.addTreeSelectionListener(treeSelectionListener)

            val scrollPane = JScrollPane(
                tree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            )
            scrollPane.background = java.awt.Color(18, 18, 18)
            scrollPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
            val composePanel = ComposePanel()
            composePanel.setContent {
                SelectedList(selectedList,
                    removeItem = {
                        if (selectedPath != null) {
                            tree.removeSelectionPath(selectedPath)
                            selectedPath = null
                        }
                        selectedListRemove(it)
                        // 取消过滤
                        val path = pathNameMap[it]
                        if (path != null) selectedPathListRemove(path)
                    })
            }
            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
            splitPane.leftComponent = scrollPane
            splitPane.rightComponent = composePanel
            scrollPane.background = java.awt.Color(18, 18, 18)
            splitPane.leftComponent.background = java.awt.Color(18, 18, 18)
            splitPane.rightComponent.background = java.awt.Color(18, 18, 18)
            splitPane.dividerLocation = 235
            SwingPanel(
                background = Color(MaterialTheme.colors.background.toArgb()),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                factory = {
                    splitPane
                }
            )
        }

    }
}

@Composable
fun SelectedList(list: List<String>, removeItem: (String) -> Unit) {
    MaterialTheme(colors = if (FlatLaf.isLafDark()) DarkColorScheme else LightColorScheme) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            LazyColumn {
                items(list) { fileName ->

                    Box(
                        modifier = Modifier.clickable {}
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = fileName,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .clickable { removeItem(fileName) }
                                .align(Alignment.CenterEnd)
                                .size(30.dp, 30.dp)

                        )
                    }
                }
            }
        }
    }


}

fun searchPaths(dir: File): Map<String, String> {
    val pathNameMap: MutableMap<String, String> = hashMapOf()
    dir.listFiles().forEach { file ->
        if (file.isDirectory) {
            pathNameMap.putAll(searchPaths(file))
        }
        if (!file.isDirectory) {
            pathNameMap.put(file.nameWithoutExtension, file.absolutePath)
        }
    }
    return pathNameMap
}

fun addNodes(curTop: DefaultMutableTreeNode?, dir: File): DefaultMutableTreeNode {
    val curDir = DefaultMutableTreeNode(dir.nameWithoutExtension)
    curTop?.add(curDir)
    val ol = Vector<File>()
    dir.listFiles().forEach { ol.addElement(it) }
    ol.sort()
    var files = Vector<String>()

    ol.forEach { file ->
        if (file.isDirectory)
            addNodes(curDir, file);
        else
            files.addElement(file.nameWithoutExtension);
    }

    val cmp = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
    Collections.sort(files, cmp)
    files.forEach {
        curDir.add(DefaultMutableTreeNode(it))
    }
    return curDir
}

@OptIn(ExperimentalSerializationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SelectFile(
    state: AppState,
    type: VocabularyType,
    relateVideoPath:String,
    setRelateVideoPath:(String) ->Unit,
    trackMap:Map<Int,String>,
    setTrackMap:(Map<Int,String>) ->Unit,
    selectedTrackId:Int,
    setSelectedTrackId:(Int) -> Unit,
    fileFilter: FileNameExtensionFilter,
    setSelectFileName: (String) -> Unit,
    analysis: (String,Int) -> Unit
) {

    Column(Modifier.height(IntrinsicSize.Max)){
        var filePath by remember { mutableStateOf("") }
        var selectedSubtitle by remember { mutableStateOf("    ") }
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ) {
            val chooseText = when (type) {
                DOCUMENT -> "选择文档"
                SUBTITLES -> "选择字幕"
                MKV -> "选择 MKV 文件"
            }
            Text(chooseText, color = MaterialTheme.colors.onBackground)
            if(type == SUBTITLES) Spacer(Modifier.width(95.dp))
            BasicTextField(
                value = filePath,
                onValueChange = {
                    filePath = it
                    setSelectFileName(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                textStyle = TextStyle(
                    lineHeight = 26.sp,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onBackground
                ),
                modifier = Modifier
                    .width(300.dp)
                    .padding(start = 8.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Text(text = "打开文件", modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
//                state.loadingFileChooserVisible = true
                    Thread(Runnable {
                        val fileChooser = state.futureFileChooser.get()
                        fileChooser.dialogTitle = chooseText
                        fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                        fileChooser.isAcceptAllFileFilterUsed = false
                        fileChooser.addChoosableFileFilter(fileFilter)
                        fileChooser.selectedFile = null
                        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val file = fileChooser.selectedFile
                            filePath = file.absolutePath
                            if(type == MKV) {
                                setRelateVideoPath(file.absolutePath)
                                val window = state.videoPlayerWindow
                                val  mediaPlayerComponent= state.videoPlayerComponent
                                mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                                        val map = HashMap<Int, String>()
                                        mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
                                            if(index != 0){
                                                map[index-1] = trackDescription.description()
                                            }
                                        }
                                        mediaPlayer.controls().pause()
                                        window.isAlwaysOnTop = true
                                        window.title = "视频播放窗口"
                                        window.isVisible = false
                                        mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
                                        setTrackMap(map)
                                    }
                                })
                                window.title = "正在读取字幕"
                                window.isAlwaysOnTop = false
                                window.toBack()
                                window.size = Dimension(1,1)
                                window.location = Point(0,0)
                                window.layout = null
                                window.contentPane.add(mediaPlayerComponent)
                                window.isVisible = true
                                mediaPlayerComponent.mediaPlayer().media().play(filePath)
                            }
                            setSelectFileName(file.nameWithoutExtension)
                            fileChooser.selectedFile = File("")
                        }
                        fileChooser.removeChoosableFileFilter(fileFilter)
//                    state.loadingFileChooserVisible = false
                    }).start()

                }) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground,
                    )
                }
            }

            if (filePath.isNotEmpty() && type == MKV && trackMap.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                ) {
                    Text("选择字幕 ", color = MaterialTheme.colors.onBackground)
                    var expanded by remember { mutableStateOf(false) }

                    Box(Modifier.width(IntrinsicSize.Max)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
//                                .widthIn(50.dp,180.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(
                                text = selectedSubtitle, fontSize = 12.sp,
//                                modifier = Modifier.widthIn(50.dp,180.dp)
                            )
                            Icon(
                                Icons.Default.ExpandMore, contentDescription = "Localized description",
                                modifier = Modifier.size(20.dp, 20.dp)
                            )
                        }
                        val dropdownMenuHeight = (trackMap.size * 40 + 20).dp
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(160.dp)
                                .height(dropdownMenuHeight)
                        ) {
                            println("trackMap size:${trackMap.size}")
                            trackMap.forEach { name ->
                                DropdownMenuItem(
                                    onClick = {
                                        expanded = false
                                        selectedSubtitle = name.value
                                        setSelectedTrackId(name.key)
                                        println("选择了第${name.key}条字幕")
                                    },
                                    modifier = Modifier.width(160.dp).height(40.dp)
                                ) {
                                    Text(
                                        text = "${name.value} ", fontSize = 12.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }


                        }

                    }

                }
            }else if (filePath.isNotEmpty() && type == MKV && trackMap.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                ) {
                    Text("选择的视频没有字幕", color =Color.Red)
                }
            }
            if ((type != MKV && filePath.isNotEmpty()) ||
                (type == MKV && selectedSubtitle != "    " && trackMap.isNotEmpty())
            ) {
                OutlinedButton(onClick = {
                    analysis(filePath,selectedTrackId)
                }) {
                    Text("分析", fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
            }


        }
        if(type==SUBTITLES && filePath.isNotEmpty()){
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ){
                Text("选择对应的视频(可选)",color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = relateVideoPath,
                    onValueChange = {
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = 26.sp,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(start = 8.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
            IconButton(onClick = {
                val fileChooser = state.futureFileChooser.get()
                fileChooser.dialogTitle = "选择视频"
                fileChooser.isAcceptAllFileFilterUsed = true
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile
                    setRelateVideoPath(file.absolutePath)
                    fileChooser.selectedFile = File("")
                }
                fileChooser.removeChoosableFileFilter(fileFilter)
            }) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = "Localized description",
                    tint = MaterialTheme.colors.onBackground,
                )
            }
            }
        }
        Divider()
    }
}

fun getSubtitleTrackMap(pathName: String): Map<Int, String> {
    val map = HashMap<Int, String>()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        // Check to see if this is a valid MKV file
        // The header contains information for where all the segments are located
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return mapOf()
        }

        // Read the tracks. This contains the details of video, audio and subtitles
        // in this file
        reader.readTracks()

        // Check if there are any subtitles in this file
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            println("There are no subtitles in this file!")
            return mapOf()
        }

        reader.subtitles.forEachIndexed { index, subtitles ->
            if (subtitles.name == "English") {
                map[index] = "English"
            } else if (!subtitles.name.isNullOrEmpty() && subtitles.name.contains(Regex("Forced"))) {
                if (subtitles.language == "en") {
                    map[index] = "English Forced"
                }
                when (subtitles.languageIetf) {
                    "en" -> {
                        map[index] = "English Forced"
                    }
                    "en-US" -> {
                        map[index] = "English(United States) Forced"
                    }
                    "en-GB" -> {
                        map[index] = "English(Great Britain) Forced"
                    }
                }
            } else if (!subtitles.name.isNullOrEmpty() && subtitles.name.contains(Regex("SDH"))) {
                if (subtitles.language == "en") {
                    map[index] = "English SDH"
                }
                when (subtitles.languageIetf) {
                    "en" -> {
                        map[index] = "English SDH"
                    }
                    "en-US" -> {
                        map[index] = "English(United States) SDH"
                    }
                    "en-GB" -> {
                        map[index] = "English(Great Britain) SDH"
                    }
                }

            } else if (subtitles.languageIetf == "en") {
                map[index] = "English"
            } else if (subtitles.languageIetf == "en-US") {
                map[index] = "English(United States)"
            } else if (subtitles.languageIetf == "en-GB") {
                map[index] = "English(Great Britain)"
            }
        }

    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return map
}
//fun getSubtitleTrackMapFromVLC(pathName: String, mediaPlayerComponent: Component):Map<Int,String>{
//    val map = HashMap<Int, String>()
//    mediaPlayerComponent.mediaPlayer().media().play(pathName)
//    mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
//        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
//               println("subtitle track count :${ mediaPlayer.subpictures().trackCount()}")
//                mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
//                    println("index:$index,$trackDescription")
//                    if(index != 0){
//                        map[index-1] = trackDescription.description()
//                    }
//                }
//
//            mediaPlayer.controls().pause()
//            mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
//            println("remove event event adapter")
//        }
//    })
//
//    return map
//}
fun filterDocumentWords(
    documentWords: List<Word>,
    notBncFilter: Boolean,
    notFrqFilter: Boolean,
    replaceToLemma: Boolean
): List<Word> {

    val previewList = ArrayList(documentWords)
    val notBncList = ArrayList<Word>()
    val notFrqList = ArrayList<Word>()

    /**
     * 派生词列表，需要转换为原型的单词
     */
    val toLemmaList = ArrayList<Word>()

    /**
     * 原型词列表
     */
    val lemmaList = ArrayList<Word>()

    /**
     * 准备批量查询的原型词词典
     */
    val queryMap = HashMap<String,MutableList<Caption>>()
    documentWords.forEach { word ->
        if (notBncFilter && word.bnc == 0) notBncList.add(word)
        if (notFrqFilter && word.frq == 0) notFrqList.add(word)
        val lemma = getWordLemma(word)
        if (replaceToLemma && !lemma.isNullOrEmpty()) {
            toLemmaList.add(word)
            if(queryMap[lemma].isNullOrEmpty()){
                queryMap[lemma] = word.captions
            }else{
                // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                val list = mutableListOf<Caption>()
                list.addAll(queryMap[lemma]!!)
                list.addAll(word.captions)
                queryMap[lemma] = list
            }
        }
    }
    // 查询单词原型
    val result = Dictionary.querySet(queryMap.keys)
    result.forEach { word ->
        val captions = queryMap[word.value]!!
        word.captions = captions
        lemmaList.add(word)
    }
    previewList.removeAll(notBncList)
    previewList.removeAll(notFrqList)
    previewList.removeAll(toLemmaList)
    previewList.addAll(lemmaList)
    return previewList
}

fun filterSelectVocabulary(pathList: List<String>, filteredDocumentList: List<Word>): List<Word> {
    println("Filter Select Vocabulary")
    var list = ArrayList(filteredDocumentList)
    pathList.forEach { pathName ->
        val vocabulary = loadVocabulary(pathName)
        list.removeAll(vocabulary.wordList)
    }
    return list
}

/**
 * 预览单词
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PreviewWords(
    previewList: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    removeWord: (Word) -> Unit
) {
    Column {
        Summary(previewList, summaryVocabulary)
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(130.dp),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 60.dp),
                state = listState
            ) {
                items(previewList) { word ->

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape,
                            ) {
                                Column(Modifier.padding(5.dp).width(200.dp)) {
                                    Text(
                                        text = "${word.value}",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val lemma = getWordLemma(word)
                                    if (lemma != null) {
                                        Text(text = "原型:$lemma", fontSize = 12.sp)
                                    }
                                    Row {
                                        Text(text = "BNC  ", fontSize = 12.sp, modifier = Modifier.padding(end = 2.dp))
                                        Text(text = ":${word.bnc}", fontSize = 12.sp)
                                    }

                                    Text(text = "COCA:${word.frq}", fontSize = 12.sp)
                                    Divider()
                                    Text(
                                        text = "${word.translation}",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                    )

                                    if (word.captions.isNotEmpty()) {
                                        Divider()
                                        word.captions.forEachIndexed { index, caption ->
                                            val top = if (index == 0) 5.dp else 0.dp
                                            Text(
                                                text = caption.content,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = top)
                                            )
                                        }

                                    }
                                }
                            }
                        },
                        delayMillis = 50,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.BottomCenter,
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(7.5.dp),
                            elevation = 3.dp
                        ) {
                            var closeVisible by remember { mutableStateOf(false) }
                            Box(Modifier.size(width = 130.dp, height = 65.dp)
                                .onPointerEvent(PointerEventType.Enter) {
                                    closeVisible = true
                                }
                                .onPointerEvent(PointerEventType.Exit) {
                                    closeVisible = false
                                }) {
                                Text(
                                    text = word.value,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colors.onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                if (closeVisible) {
                                    Icon(
                                        Icons.Filled.Close, contentDescription = "",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.clickable { removeWord(word) }.align(Alignment.TopEnd)
                                    )
                                }




                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                style = LocalScrollbarStyle.current.copy(shape = RectangleShape),
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = listState
                )
            )
        }
    }


}


@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun readPDF(
    pathName: String,
    addProgress: (Float) -> Unit,
    setProgressText: (String) -> Unit
): Set<Word> {
    val file = File(pathName)
    var text = ""
    val extension = file.extension
    val otherExtensions = listOf("txt", "java", "cs", "cpp", "c", "kt", "js", "py", "ts")
    if (extension == "pdf") {
        setProgressText("正在加载文档")
        val document: PDDocument = PDDocument.load(file)
        addProgress(0.1F)
        //Instantiate PDFTextStripper class
        val pdfStripper = PDFTextStripper()
        text = pdfStripper.getText(document)
        document.close()
    } else if (otherExtensions.contains(extension)) {
        text = file.readText()
    }

    val set: MutableSet<String> = HashSet()
//    val modelFile = getFile("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin")
    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        val model = TokenizerModel(inputStream)
        addProgress(0.35F)
        setProgressText("正在分词")
        val tokenizer: Tokenizer = TokenizerME(model)
        val tokenize = tokenizer.tokenize(text)
        addProgress(0.1F)
        setProgressText("正在处理特殊分隔符")
        tokenize.forEach { word ->
            val lowercase = word.lowercase(Locale.getDefault())
            // 在代码片段里的关键字之间用.符号分隔
            if (lowercase.contains(".")) {
                val split = lowercase.split("\\.").toTypedArray()
                set.addAll(split.toList())
            }
            // 还有一些关键字之间用 _ 符号分隔
            if (lowercase.matches(Regex("_"))) {
                val split = lowercase.split("_").toTypedArray()
                set.addAll(split.toList())
            }
            set.add(lowercase)
        }
//        FileInputStream("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { modelIn ->
//        }
    }

    addProgress(0.15F)
    setProgressText("从文档提取出 ${set.size} 个单词，正在批量查询单词，如果词典里没有的就丢弃")
    println(" extra PDF word size: " + set.size)
    val start = System.currentTimeMillis()
    val validSet = Dictionary.querySet(set)
    println("查询单词共耗时：${System.currentTimeMillis() - start} millis")
    setProgressText("${validSet.size} 个有效单词")
    addProgress(0.1F)
    setProgressText("")
    return validSet
}


// 提取 srt 字幕 ffmpeg -i input.mkv -map "0:2" output.eng.srt
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
private fun readSRT(
    pathName: String,
    addProgress: (Float) -> Unit,
    setProgressText: (String) -> Unit
): Set<Word> {
    val map: MutableMap<String, MutableList<Caption>> = HashMap()

    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { input ->
        val model = TokenizerModel(input)
        val tokenizer: Tokenizer = TokenizerME(model)
        val formatSRT = FormatSRT()
        val formatASS = FormatASS()
        val file = File(pathName)
        val inputStream: InputStream = FileInputStream(file)
        addProgress(0.1F)
        setProgressText("正在解析字幕文件")
        val timedTextObject: TimedTextObject =  if(file.extension == "srt"){
            formatSRT.parseFile(file.name, inputStream)
        }else {
            formatASS.parseFile(file.name, inputStream)
        }

        val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
        val captionList: Collection<subtitleFile.Caption> = captions.values
        addProgress(0.2F)
        setProgressText("正在分词")
        for (caption in captionList) {
            var content = replaceSpecialCharacter(caption.content)
            val tokenize = tokenizer.tokenize(content)
            for (word in tokenize) {

                var text = caption.content.replace("<br />", "\n")
                if(text.endsWith("\n")){
                    text = text.substringBefore("\n")
                }
                val captionString = Caption(
                    // getTime(format) 返回的时间不能播放
                    start = caption.start.getTime("hh:mm:ss.ms"),
                    end = caption.end.getTime("hh:mm:ss.ms"),
                    content = text
                )
                val lowercase = word.lowercase(Locale.getDefault())
                if (!map.containsKey(lowercase)) {
                    val list = mutableListOf(captionString)
                    map[lowercase] = list
                } else {
                    if (map[lowercase]!!.size < 3) {
                        map[lowercase]?.add(captionString)
                    }
                }
            }
        }
    }
    addProgress(0.2F)
    setProgressText("从字幕文件中提取出 ${map.size} 个单词，正在批量查询单词，如果词典里没有就丢弃")
    val validSet = Dictionary.querySet(map.keys)
    setProgressText("${validSet.size} 个有效单词")
    validSet.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    addProgress(0.35F)
    setProgressText("")
    return validSet
}


@OptIn(ExperimentalComposeUiApi::class)
private fun readMKV(
    pathName: String,
    trackId :Int,
    addProgress: (Float) -> Unit,
    setProgressText: (String) -> Unit
): Set<Word> {
    val map: MutableMap<String, ArrayList<Caption>> = HashMap()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        val start = System.currentTimeMillis()
        setProgressText("正在解析 MKV 文件")
        addProgress(0.2F)
        // Check to see if this is a valid MKV file
        // The header contains information for where all the segments are located
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return setOf()
        }
        println("读取头文件共消耗：${System.currentTimeMillis() - start}毫秒")
        // Read the tracks. This contains the details of video, audio and subtitles
        // in this file
        reader.readTracks()

        // Check if there are any subtitles in this file
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            println("There are no subtitles in this file!")
            return setOf()
        }
        println("There are $numSubtitles subtitles in this file")

        // You need this to find the clusters scattered across the file to find
        // video, audio and subtitle data
        reader.readCues()

        // OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
//            for (int i = 0; i < reader.getSubtitles().size(); i++) {
//                if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
//                    SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
//                    System.out.println(subs.getHeader());
//                }
//            }

        // Read all the subtitles from the file each from cue index.
        // Once a cue is parsed, it is cached, so if you read the same cue again,
        // it will not waste time.
        // Performance-wise, this will take some time because it needs to read
        // most of the file.
        println("CuesCount: " + reader.cuesCount)
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }
        println("读取 Cue 共消耗：${System.currentTimeMillis() - start}毫秒")
        addProgress(0.45F)
        setProgressText("正在分词")
        // If you had to seek the video while the subtitles are still extracting,
        // you can use read a different cue index. Use getCueIndexFromAddress()
        // to find the nearest floor cue from address you seek to.
        val subtitles = reader.subtitles[trackId].readUnreadSubtitles()
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            val model = TokenizerModel(inputStream)
            val tokenizer: Tokenizer = TokenizerME(model)
            for (subtitle in subtitles) {
                var content = replaceSpecialCharacter(subtitle.stringData)
                content = content.lowercase(Locale.getDefault())
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {

                    val stringData =removeLocationInfo(subtitle.stringData)
                    if (!map.containsKey(word)) {
                        val list = ArrayList<Caption>()
                        list.add(
                            Caption(
                                start = subtitle.startTime.format().toString(),
                                end = subtitle.endTime.format(),
                                content = stringData
                            )
                        )
                        map[word] = list
                    } else {
                        if (map[word]!!.size < 3) {
                            map[word]!!
                                .add(
                                    Caption(
                                        start = subtitle.startTime.format().toString(),
                                        end = subtitle.endTime.format(),
                                        content =stringData
                                    )
                                )
                        }
                    }
                }
            }
            println(" map size: " + map.size)
        }
        println("读取分词共消耗：${System.currentTimeMillis() - start}毫秒")
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    addProgress(0.1F)
    setProgressText("从视频中提取出${map.keys.size}个单词，正在批量查询单词，如果词典里没有就丢弃")
    val validSet = Dictionary.querySet(map.keys)
    setProgressText("${validSet.size}个有效单词")
    validSet.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    addProgress(0.1F)
    setProgressText("")
    return validSet
}

/**
 * 替换一些特殊字符
 */
private fun replaceSpecialCharacter(captionContent: String): String {
    var content = captionContent
    if (content.startsWith("-")) content = content.substring(1)
    if (content.contains("[")) {
        content = content.replace(Regex("\\["), "")
    }
    if (content.contains("]")) {
        content = content.replace(Regex("]"), "")
    }
    if (content.contains("<i>")) {
        content = content.replace("<i>", "")
    }
    if (content.contains("</i>")) {
        content = content.replace("</i>", "")
    }
    if (content.contains("<br />")) {
        content = content.replace("<br />", "")
    }
    content = removeLocationInfo(content)
    return content
}

/**
 * 有一些字幕并不是在一个的固定位置，而是标注在人物旁边，这个函数删除位置信息
 */
private fun removeLocationInfo(content:String):String{
    var pattern = Pattern.compile("\\{.*\\}")
    var matcher = pattern.matcher(content)
    return matcher.replaceAll("")
}