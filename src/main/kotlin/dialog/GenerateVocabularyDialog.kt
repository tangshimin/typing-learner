package dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.subtitles.SSASubtitles
import components.createTransferHandler
import components.parseTrackList
import data.*
import data.Dictionary
import data.VocabularyType.*
import dialog.FilterState.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import player.isWindows
import state.AppState
import state.composeAppResource
import state.getResourcesFile
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Paths
import java.text.Collator
import java.util.*
import java.util.concurrent.FutureTask
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import kotlin.collections.HashMap

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
fun GenerateVocabularyDialog(
    state: AppState,
    title: String,
    type: VocabularyType
) {
    Dialog(
        title = title,
        onCloseRequest = {
            onCloseRequest(state, title)
        },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1210.dp, 785.dp)
        ),
    ) {
        val scope = rememberCoroutineScope()

        val fileFilter = when (title) {
            "过滤词库" -> FileNameExtensionFilter(
                "词库",
                "json",
            )
            "从文档生成词库" -> FileNameExtensionFilter(
                "支持的文件扩展(*.pdf、*.txt)",
                "pdf",
                "txt",
            )
            "从字幕生成词库" -> FileNameExtensionFilter(
                "SRT 格式的字幕文件",
                "srt",
            )

            "从 MKV 视频生成词库" -> FileNameExtensionFilter(
                "mkv 格式的视频文件",
                "mkv",
            )
            else -> null
        }

        /**
         * 选择的文件的绝对路径
         */
        var selectedFilePath by remember { mutableStateOf("") }

        /**
         * 选择的字幕名称
         */
        var selectedSubtitlesName by remember { mutableStateOf("    ") }

        /**
         * 预览的单词
         */
        val previewList = remember { mutableStateListOf<Word>() }

        /**
         * 从字幕生成单词 -> 相关视频的地址
         */
        var relateVideoPath by remember { mutableStateOf("") }

        /**
         * 字幕的轨道 ID
         */
        var selectedTrackId by remember { mutableStateOf(0) }

        /**
         * 需要过滤的词库的类型
         */
        var filteringType by remember { mutableStateOf(DOCUMENT) }

        /**
         * 字幕轨道列表
         */
        val trackList = remember { mutableStateListOf<Pair<Int, String>>() }

        /**
         * 这个 filterState 有四个状态：Idle、"Parse"、"Filtering"、"End"
         */
        var filterState by remember { mutableStateOf(Idle) }

        /**
         * 摘要词库
         */
        val summaryVocabulary = loadSummaryVocabulary()

        /**
         * 分析之后得到的单词
         */
        val documentWords = remember { mutableStateListOf<Word>() }

        /**
         * 用于过滤的词库列表
         */
        val selectedFileList = remember { mutableStateListOf<File>() }

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

        var loading by remember { mutableStateOf(false) }

        /**  处理拖放文件的函数 */
        val transferHandler = createTransferHandler(
            singleFile = false,
            showWrongMessage = { message ->
                JOptionPane.showMessageDialog(window, message)
            },
            parseImportFile = { files ->

                scope.launch {
                    Thread(Runnable {
                        if(files.size == 1){
                            val file = files.first()
                            when (file.extension) {
                                "pdf", "txt" -> {
                                    if (type == DOCUMENT) {
                                        selectedFilePath = file.absolutePath
                                        selectedSubtitlesName = "    "
                                    } else {
                                        JOptionPane.showMessageDialog(
                                            window,
                                            "如果你想从 ${file.nameWithoutExtension} 文档生成词库，\n请重新选择：词库 -> 从文档生成词库，再拖放文件到这里。"
                                        )
                                    }
                                }
                                "srt" -> {
                                    if (type == SUBTITLES) {
                                        selectedFilePath = file.absolutePath
                                        selectedSubtitlesName = "    "
                                    } else {
                                        JOptionPane.showMessageDialog(
                                            window,
                                            "如果你想从 ${file.nameWithoutExtension} 字幕生成词库，\n请重新选择：词库 -> 从字幕生成词库，再拖放文件到这里。"
                                        )
                                    }
                                }
                                "mp4" -> {
                                    if(type == SUBTITLES){
                                        relateVideoPath = file.absolutePath
                                    }else{
                                        JOptionPane.showMessageDialog(window, "格式错误")
                                    }
                                }
                                "mkv" -> {
                                    when (type) {
                                        MKV -> {
                                            loading = true
                                            selectedFilePath = file.absolutePath
                                            relateVideoPath = file.absolutePath
                                            selectedSubtitlesName = "    "
                                            parseTrackList(
                                                state.videoPlayerComponent,
                                                window,
                                                state.videoPlayerWindow,
                                                file.absolutePath,
                                                setTrackList = {
                                                    trackList.clear()
                                                    trackList.addAll(it)
                                                }
                                            )
                                            loading = false
                                        }
                                        SUBTITLES -> {
                                            relateVideoPath = file.absolutePath
                                        }
                                        else -> {
                                            JOptionPane.showMessageDialog(
                                                window,
                                                "如果你想从 ${file.nameWithoutExtension} 视频生成词库，\n请重新选择：词库 -> 从 MKV 视频生成词库，再拖放文件到这里。"
                                            )
                                        }
                                    }
                                }
                                "json" -> {
                                    if (title == "过滤词库") {
                                        selectedFilePath = file.absolutePath
                                    }
                                }
                                else -> {
                                    JOptionPane.showMessageDialog(window, "格式不支持")
                                }
                            }


                        }else if(files.size == 2){
                            if(type == SUBTITLES){
                                val first = files.first()
                                val last = files.last()
                                if(first.extension == "srt" && (last.extension == "mp4" || last.extension == "mkv")){
                                    selectedFilePath = first.absolutePath
                                    selectedSubtitlesName = "    "
                                    relateVideoPath = last.absolutePath
                                    selectedTrackId = -1
                                }else if(last.extension == "srt" && (first.extension == "mp4" || first.extension == "mkv")){
                                    selectedFilePath = last.absolutePath
                                    selectedSubtitlesName = "    "
                                    relateVideoPath = first.absolutePath
                                    selectedTrackId = -1
                                }else if(first.extension == "srt" && last.extension == "srt"){
                                    JOptionPane.showMessageDialog(window, "不能接收两个 srt 字幕文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件")
                                }else if(first.extension == "mp4" && last.extension == "mp4"){
                                    JOptionPane.showMessageDialog(window, "不能接收两个 mp4 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件")
                                }else if(first.extension == "mkv" && last.extension == "mkv"){
                                    JOptionPane.showMessageDialog(window, "不能接收两个 mkv 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件")
                                }else if(first.extension == "mkv" && last.extension == "mp4"){
                                    JOptionPane.showMessageDialog(window, "不能接收两个视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件")
                                }else{
                                    JOptionPane.showMessageDialog(window, "格式错误，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件")
                                }

                            }else{
                                JOptionPane.showMessageDialog(window, "只要从字幕生成词库才能接收两个文件")
                            }
                        }else{
                            JOptionPane.showMessageDialog(window, "文件不能超过两个")
                        }
                    }).start()
                }


            }
        )
        window.transferHandler = transferHandler

        val contentPanel = ComposePanel()
        contentPanel.setContent {
            MaterialTheme(colors = state.colors) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                    Divider()
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.width(540.dp).fillMaxHeight()) {
                            BasicFilter(
                                notBncFilter = notBncFilter,
                                setNotBncFilter = {
                                    notBncFilter = it
                                    filterState = Filtering
                                },
                                notFrqFilter = notFrqFilter,
                                setNotFrqFilter = {
                                    notFrqFilter = it
                                    filterState = Filtering
                                },
                                replaceToLemma = replaceToLemma,
                                setReplaceToLemma = {
                                    replaceToLemma = it
                                    filterState = Filtering
                                },
                            )
                            VocabularyFilter(
                                futureFileChooser = state.futureFileChooser,
                                selectedFileList = selectedFileList,
                                selectedFileListAdd = {
                                    if (!selectedFileList.contains(it)) {
                                        selectedFileList.add(it)
                                        filterState = Filtering
                                    }
                                },
                                selectedFileListRemove = {
                                    selectedFileList.remove(it)
                                    filterState = Filtering
                                },
                                recentList = state.recentList,
                                removeInvalidRecentItem = {
                                    state.removeInvalidRecentItem(it)
                                }
                            )
                        }
                        Divider(Modifier.width(1.dp).fillMaxHeight())
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colors.background)
                        ) {

                            var progressText by remember { mutableStateOf("") }

                            /** 暂时不读取 MKV 里的 ASS 字幕 */
                            var isSelectedASS by remember { mutableStateOf(false) }

                            SelectFile(
                                state = state,
                                parentWindow = window,
                                type = type,
                                title = title,
                                selectedFilePath = selectedFilePath,
                                setSelectedFilePath = { selectedFilePath = it },
                                selectedSubtitle = selectedSubtitlesName,
                                setSelectedSubtitle = { selectedSubtitlesName = it },
                                isSelectedASS = isSelectedASS,
                                setIsSelectedASS = { isSelectedASS = it },
                                fileFilter = fileFilter,
                                relateVideoPath = relateVideoPath,
                                setRelateVideoPath = { relateVideoPath = it },
                                trackList = trackList,
                                setTrackList = {
                                    trackList.clear()
                                    trackList.addAll(it)
                                },
                                selectedTrackId = selectedTrackId,
                                setSelectedTrackId = { selectedTrackId = it },
                                setIsLoading = {loading = it},
                                analysis = { pathName, trackId ->
                                    filterState = Parse
                                    selectedFileList.clear()
                                    documentWords.clear()
                                    Thread(Runnable() {

                                        val words = when (type) {
                                            DOCUMENT -> {
                                                if (title == "过滤词库") {
                                                    val vocabulary = loadVocabulary(pathName)
                                                    filteringType = vocabulary.type
                                                    relateVideoPath = vocabulary.relateVideoPath
                                                    selectedTrackId = vocabulary.subtitlesTrackId
                                                    vocabulary.wordList
                                                } else {
                                                    readDocument(
                                                        pathName = pathName,
                                                        setProgressText = { progressText = it })
                                                }

                                            }
                                            SUBTITLES -> {
                                                readSRT(pathName = pathName, setProgressText = { progressText = it })
                                            }
                                            MKV -> {
                                                readMKV(
                                                    pathName = pathName,
                                                    trackId = trackId,
                                                    setProgressText = { progressText = it },
                                                    setIsSelectedASS = { isSelectedASS = it })
                                            }
                                        }


                                        words.forEach { word -> documentWords.add(word) }
                                        filterState =
                                            if (notBncFilter || notFrqFilter || replaceToLemma || selectedFileList.isNotEmpty()) {
                                                Filtering
                                            } else {
                                                End
                                            }
                                        if (filterState == End) {
                                            previewList.clear()
                                            previewList.addAll(documentWords)
                                        }
                                    }).start()

                                })

                            Box(Modifier.fillMaxSize()) {
                                when (filterState) {
                                    Parse -> {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.width(60.dp).padding(bottom = 60.dp)
                                            )
                                            Text(text = progressText, color = MaterialTheme.colors.onBackground)
                                        }
                                    }
                                    Filtering -> {
                                        CircularProgressIndicator(
                                            Modifier.width(60.dp).align(Alignment.Center)
                                        )
                                        Thread(Runnable {
                                            val filteredDocumentList = filterDocumentWords(
                                                documentWords,
                                                notBncFilter,
                                                notFrqFilter,
                                                replaceToLemma
                                            )
                                            previewList.clear()
                                            val filteredList = filterSelectVocabulary(
                                                selectedFileList = selectedFileList,
                                                filteredDocumentList = filteredDocumentList
                                            )
                                            previewList.addAll(filteredList)
                                            filterState = End
                                        }).start()


                                    }
                                    End -> {
                                        PreviewWords(
                                            type = type,
                                            previewList = previewList,
                                            summaryVocabulary = summaryVocabulary,
                                            removeWord = {
                                                previewList.remove(it)
                                            })
                                    }
                                }
                                if(loading){
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                    ) {
                                        CircularProgressIndicator(
                                            Modifier.width(60.dp).padding(bottom = 60.dp)
                                        )
                                        Text(text = "正在读取字幕轨道列表", color = MaterialTheme.colors.onBackground)
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        val bottomPanel = ComposePanel()
        bottomPanel.setSize(Int.MAX_VALUE, 54)
        bottomPanel.setContent {
            MaterialTheme(colors = state.colors) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(54.dp).background(MaterialTheme.colors.background)
                ) {
                    Divider()
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        OutlinedButton(
                            enabled = previewList.size > 0,
                            onClick = {
                                scope.launch {
                                    val fileChooser = state.futureFileChooser.get()
                                    fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                    fileChooser.dialogTitle = "保存词库"
                                    val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                                    val fileName = File(selectedFilePath).nameWithoutExtension
                                    fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                                    val userSelection = fileChooser.showSaveDialog(window)
                                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                                        val selectedFile = fileChooser.selectedFile
                                       val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                                       val savePath = Paths.get(selectedFile.absolutePath)
                                        if(savePath.startsWith(vocabularyDirPath)){
                                            JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，生成的词库会被删除")
                                        }else{
                                            val vocabulary = Vocabulary(
                                                name = selectedFile.nameWithoutExtension,
                                                type = if (title == "过滤词库") filteringType else type,
                                                language = "english",
                                                size = previewList.size,
                                                relateVideoPath = relateVideoPath,
                                                subtitlesTrackId = selectedTrackId,
                                                wordList = previewList
                                            )
                                            state.saveToRecentList(vocabulary.name, selectedFile.absolutePath)
                                            saveVocabulary(vocabulary, selectedFile.absolutePath)

                                            // 清理状态
                                            selectedFilePath = ""
                                            selectedSubtitlesName = ""
                                            previewList.clear()
                                            relateVideoPath = ""
                                            selectedTrackId = 0
                                            filteringType = DOCUMENT
                                            trackList.clear()
                                            filterState = Idle
                                            documentWords.clear()
                                            selectedFileList.clear()
                                            notBncFilter = false
                                            notFrqFilter = false
                                            replaceToLemma = false
                                        }


                                    }
                                }

                            }) {
                            Text("保存")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = {
                            onCloseRequest(state, title)
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
                    add(contentPanel, BorderLayout.CENTER)
                }
            }
        )

    }
}

@Serializable
data class RecentItem(val time: String, val name: String, val path: String) {
    override fun equals(other: Any?): Boolean {
        val otherItem = other as RecentItem
        return this.name == otherItem.name && this.path == otherItem.path
    }

    override fun hashCode(): Int {
        return name.hashCode() + path.hashCode()
    }
}

enum class FilterState {
    Idle, Parse, Filtering, End
}

@OptIn(ExperimentalSerializationApi::class)
private fun onCloseRequest(state: AppState, title: String) {
    when (title) {
        "过滤词库" -> state.filterVocabulary = false
        "从文档生成词库" -> state.generateVocabularyFromDocument = false
        "从字幕生成词库" -> state.generateVocabularyFromSubtitles = false
        "从 MKV 视频生成词库" -> state.generateVocabularyFromMKV = false
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
    type: VocabularyType,
    list: List<Word>,
    summaryVocabulary: Map<String, List<String>>
) {

    Column(Modifier.fillMaxWidth()) {
        val height = if (type == DOCUMENT) 61.dp else 40.dp
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(height).padding(start = 10.dp)
        ) {
            val summary = computeSummary(list, summaryVocabulary)
            Text(text = "共 ${list.size} 词  ", color = MaterialTheme.colors.onBackground)
            Text(text = "牛津5000核心词：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["oxford"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[0]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "四级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet4"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[1]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "六级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet6"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[2]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "GRE: ", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["gre"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[3]} 词", color = MaterialTheme.colors.onBackground)
            }

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
fun BasicFilter(
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {


            Row(Modifier.width(textWidth)) {
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(Modifier.width(textWidth)) {
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VocabularyFilter(
    futureFileChooser: FutureTask<JFileChooser>,
    selectedFileList: List<File>,
    selectedFileListAdd: (File) -> Unit,
    selectedFileListRemove: (File) -> Unit,
    recentList: List<RecentItem>,
    removeInvalidRecentItem: (RecentItem) -> Unit,
) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
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
                    if (node.isLeaf) {
                        val filePath = pathNameMap[name]
                        if (filePath != null) {
                            val file = File(filePath)
                            if (!selectedFileList.contains(file)) {
                                selectedFileListAdd(file)
                            }
                        }

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
        if (!MaterialTheme.colors.isLight) {
            tree.background = java.awt.Color(32, 33, 34)
            scrollPane.background = java.awt.Color(32, 33, 34)
        }
        scrollPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        Column(Modifier.width(270.dp).fillMaxHeight().background(MaterialTheme.colors.background)) {

            if (recentList.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box(Modifier.width(270.dp).height(IntrinsicSize.Max).padding(top = 10.dp)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        Text(text = "最近生成的词库")
                    }
                    val dropdownMenuHeight = if (recentList.size <= 10) (recentList.size * 40 + 20).dp else 420.dp

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(20.dp, 0.dp),
                        modifier = Modifier.width(IntrinsicSize.Max).height(dropdownMenuHeight)
                    ) {

                        Box(Modifier.fillMaxWidth().height(dropdownMenuHeight)) {
                            val stateVertical = rememberScrollState(0)
                            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                                Column {
                                    recentList.forEach { recentItem ->
                                        val recentFile = File(recentItem.path)
                                        if (recentFile.exists()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().height(40.dp)
                                                    .clickable {
                                                        expanded = false
                                                        selectedFileListAdd(recentFile)
                                                    }
                                            ) {
                                                Text(
                                                    text = recentItem.name,
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colors.onBackground,
                                                    modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                                                )

                                            }

                                        } else {
                                            // 文件可能被删除了
                                            removeInvalidRecentItem(recentItem)

                                        }

                                    }

                                }
                            }

                            VerticalScrollbar(
                                style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = stateVertical
                                )
                            )
                        }

                    }
                }
            }
            Box(Modifier.width(270.dp).height(IntrinsicSize.Max).background(MaterialTheme.colors.background)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(139.dp)
                        .align(Alignment.Center)
                ) {
                    Text(text = "内置词库")
                }
                DropdownMenu(
                    expanded = expanded,
                    offset = DpOffset(20.dp, 0.dp),
                    onDismissRequest = { expanded = false },
                ) {
                    SwingPanel(
                        modifier = Modifier.width(400.dp).height(400.dp),
                        factory = {
                            scrollPane
                        }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        Thread(Runnable {
                            val fileChooser = futureFileChooser.get()
                            fileChooser.dialogTitle = "选择词库"
                            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                            fileChooser.isAcceptAllFileFilterUsed = false
                            val fileFilter = FileNameExtensionFilter("词库", "json")
                            fileChooser.addChoosableFileFilter(fileFilter)
                            fileChooser.selectedFile = null
                            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val file = fileChooser.selectedFile
                                selectedFileListAdd(File(file.absolutePath))
                            }
                            fileChooser.selectedFile = null
                            fileChooser.removeChoosableFileFilter(fileFilter)
                        }).start()

                    },
                    modifier = Modifier
                        .width(139.dp)
                ) {
                    Text(text = "选择词库")
                }
            }

        }
        Divider(Modifier.width(1.dp).fillMaxHeight())
        Column(
            Modifier.width(270.dp).fillMaxHeight()
                .background(MaterialTheme.colors.background)
        ) {
            SelectedList(
                selectedFileList,
                removeFile = {
                    if (selectedPath != null) {
                        tree.removeSelectionPath(selectedPath)
                        selectedPath = null
                    }
                    selectedFileListRemove(it)
                })
        }
    }
}

@Composable
fun SelectedList(
    list: List<File>,
    removeFile: (File) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LazyColumn {
            items(list) { file ->

                Box(
                    modifier = Modifier.clickable {}
                        .fillMaxWidth()
                ) {
                    Text(
                        text = file.nameWithoutExtension,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).width(225.dp).padding(10.dp)
                    )

                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .clickable { removeFile(file) }
                            .align(Alignment.CenterEnd)
                            .size(30.dp, 30.dp)

                    )
                }
            }
        }
    }


}

/**
 * Map<String,String> 的类型参数，第一个代表文件名，第二个代表文件的绝对路径
 */
fun searchPaths(dir: File): MutableMap<String, String> {
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

@OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun SelectFile(
    parentWindow: Component,
    state: AppState,
    type: VocabularyType,
    title: String,
    selectedFilePath: String,
    setSelectedFilePath: (String) -> Unit,
    selectedSubtitle: String,
    setSelectedSubtitle: (String) -> Unit,
    isSelectedASS: Boolean,
    setIsSelectedASS: (Boolean) -> Unit,
    relateVideoPath: String,
    setRelateVideoPath: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    selectedTrackId: Int,
    setSelectedTrackId: (Int) -> Unit,
    fileFilter: FileNameExtensionFilter?,
    setIsLoading:(Boolean) -> Unit,
    analysis: (String, Int) -> Unit
) {

    Column(Modifier.height(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ) {
            val chooseText = when (title) {
                "过滤词库" -> "选择词库"
                "从文档生成词库" -> "选择文档"
                "从字幕生成词库" -> "选择字幕"
                "从 MKV 视频生成词库" -> "选择 MKV 文件"
                else -> ""
            }


            Text(chooseText, color = MaterialTheme.colors.onBackground)
            if (type == SUBTITLES || type == DOCUMENT) {
                Spacer(Modifier.width(95.dp))
            } else if (type == MKV) {
                Spacer(Modifier.width(44.dp))
            }
            BasicTextField(
                value = selectedFilePath,
                onValueChange = {
                    setSelectedFilePath(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                textStyle = TextStyle(
                    lineHeight = 29.sp,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onBackground
                ),
                modifier = Modifier
                    .width(300.dp)
                    .padding(start = 8.dp, end = 10.dp)
                    .height(35.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            val scope = rememberCoroutineScope()
            OutlinedButton(onClick = {
                Thread(Runnable {
                    val fileChooser = state.futureFileChooser.get()
                    fileChooser.dialogTitle = chooseText
                    fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                    fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    fileChooser.isAcceptAllFileFilterUsed = false
                    fileChooser.addChoosableFileFilter(fileFilter)
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        scope.launch {
                            Thread(Runnable {
                                val file = fileChooser.selectedFile
                                setSelectedFilePath(file.absolutePath)
                                setSelectedSubtitle("    ")
                                if (type == MKV) {
                                    setIsLoading(true)
                                    setRelateVideoPath(file.absolutePath)
                                    parseTrackList(
                                        state.videoPlayerComponent,
                                        parentWindow,
                                        state.videoPlayerWindow,
                                        file.absolutePath,
                                        setTrackList = { setTrackList(it) },
                                    )
                                    setIsLoading(false)
                                }
                                fileChooser.selectedFile = File("")
                            }).start()
                        }

                    }
                    fileChooser.removeChoosableFileFilter(fileFilter)
                }).start()

            }) {
                Text("打开", fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))
            if (type != MKV && selectedFilePath.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        analysis(selectedFilePath, selectedTrackId)
                    }) {
                    Text("分析", fontSize = 12.sp)
                }
            }
        }

        if (selectedFilePath.isNotEmpty() && type == MKV) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                if (trackList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 95.dp)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.width(IntrinsicSize.Max)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(282.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(
                                    text = selectedSubtitle, fontSize = 12.sp,
                                )
                                Icon(
                                    Icons.Default.ExpandMore, contentDescription = "Localized description",
                                    modifier = Modifier.size(20.dp, 20.dp)
                                )
                            }
                            val dropdownMenuHeight = (trackList.size * 40 + 20).dp
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(282.dp)
                                    .height(dropdownMenuHeight)
                            ) {
                                trackList.forEach { (index, description) ->
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            setIsSelectedASS(false)
//                                            selectedSubtitle = description
                                            setSelectedSubtitle(description)
                                            setSelectedTrackId(index)
                                        },
                                        modifier = Modifier.width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = "$description ", fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }


                            }

                        }

                    }
                }
                if (selectedSubtitle != "    " && trackList.isNotEmpty()) {
                    if (isSelectedASS) {
                        Text("暂时不支持 ASS 字幕,请重新选择", color = Color.Red)
                    } else {
                        OutlinedButton(onClick = {
                            analysis(selectedFilePath, selectedTrackId)
                        }) {
                            Text("分析", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (type == SUBTITLES && selectedFilePath.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                Text("选择对应的视频(可选)", color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = relateVideoPath,
                    onValueChange = {
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = 29.sp,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(start = 8.dp, end = 10.dp)
                        .height(35.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
                OutlinedButton(onClick = {
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
                    Text("打开")
                }
            }
        }
        Divider()
    }
}


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
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word,String>()

    /**
     * 原型词 > 字幕列表  映射
     */
    val captionsMap = HashMap<String, MutableList<Caption>>()
    documentWords.forEach { word ->
        if (notBncFilter && word.bnc == 0) notBncList.add(word)
        if (notFrqFilter && word.frq == 0) notFrqList.add(word)
        val lemma = getWordLemma(word)
        if (replaceToLemma && !lemma.isNullOrEmpty()) {
            lemmaMap[word] = lemma
            if (captionsMap[lemma].isNullOrEmpty()) {
                captionsMap[lemma] = word.captions
            } else {
                // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                val list = mutableListOf<Caption>()
                list.addAll(captionsMap[lemma]!!)
                for (caption in word.captions) {
                    if(list.size<3){
                        list.add(caption)
                    }
                }
                captionsMap[lemma] = list
            }
        }
    }
    // 查询单词原型
    val queryList = lemmaMap.values.toList()
    val result = Dictionary.queryList(queryList)
    val validLemmaMap = HashMap<String,Word>()
    result.forEach { word ->
        val captions = captionsMap[word.value]!!
        word.captions = captions
        validLemmaMap[word.value] = word
    }
    previewList.removeAll(notBncList)
    previewList.removeAll(notFrqList)
    val toLemmaList = lemmaMap.keys
    for (word in toLemmaList) {
        val index = previewList.indexOf(word)
        // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
        if(index != -1){
            val lemmaStr = lemmaMap[word]
            val validLemma = validLemmaMap[lemmaStr]
            if(validLemma != null){
                previewList.remove(word)
                if (!previewList.contains(validLemma)){
                    previewList.add(index,validLemma)
                }
            }

        }

    }
    return previewList
}

fun filterSelectVocabulary(
    selectedFileList: List<File>,
    filteredDocumentList: List<Word>
): List<Word> {

    var list = ArrayList(filteredDocumentList)
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)
            list.removeAll(vocabulary.wordList)
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }

    }
    return list
}

/**
 * 预览单词
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PreviewWords(
    type: VocabularyType,
    previewList: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    removeWord: (Word) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Summary(type, previewList, summaryVocabulary)
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
                style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
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
fun readDocument(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val file = File(pathName)
    var text = ""
    val extension = file.extension
    val otherExtensions = listOf("txt", "java", "cs", "cpp", "c", "kt", "js", "py", "ts")

    try{
        if (extension == "pdf") {
            setProgressText("正在加载文档")
            val document: PDDocument = PDDocument.load(file)
            //Instantiate PDFTextStripper class
            val pdfStripper = PDFTextStripper()
            text = pdfStripper.getText(document)
            document.close()
        } else if (otherExtensions.contains(extension)) {
            text = file.readText()
        }
    }catch (exception: InvalidPasswordException){
        JOptionPane.showMessageDialog(null,exception.message)
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }


    val set: MutableSet<String> = HashSet()
    val list = mutableListOf<String>()
    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        val model = TokenizerModel(inputStream)
        setProgressText("正在分词")
        val tokenizer: Tokenizer = TokenizerME(model)
        val tokenize = tokenizer.tokenize(text)
        setProgressText("正在处理特殊分隔符")
        tokenize.forEach { word ->
            val lowercase = word.lowercase(Locale.getDefault())
            // 在代码片段里的关键字之间用.符号分隔
            if (lowercase.contains(".")) {
                val split = lowercase.split("\\.").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            // 还有一些关键字之间用 _ 符号分隔
            if (lowercase.matches(Regex("_"))) {
                val split = lowercase.split("_").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            if (!set.contains(lowercase)) {
                list.add(lowercase)
                set.add(lowercase)
            }
            set.add(lowercase)
        }

    }

    setProgressText("从文档提取出 ${set.size} 个单词，正在批量查询单词，如果词典里没有的就丢弃")
    val validList = Dictionary.queryList(list)
    setProgressText("${validList.size} 个有效单词")
    setProgressText("")
    return validList
}


// 提取 srt 字幕 ffmpeg -i input.mkv -map "0:2" output.eng.srt
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
private fun readSRT(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val map: MutableMap<String, MutableList<Caption>> = HashMap()
    // 保存顺序
    val orderList = mutableListOf<String>()
    try{
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { input ->
            val model = TokenizerModel(input)
            val tokenizer: Tokenizer = TokenizerME(model)
            val formatSRT = FormatSRT()
            val file = File(pathName)
            val inputStream: InputStream = FileInputStream(file)

            setProgressText("正在解析字幕文件")
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream)

            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList: Collection<subtitleFile.Caption> = captions.values
            setProgressText("正在分词")
            for (caption in captionList) {
                var content = replaceSpecialCharacter(caption.content)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    // getTime(format) 返回的时间不能播放
                    start = caption.start.getTime("hh:mm:ss.ms"),
                    end = caption.end.getTime("hh:mm:ss.ms"),
                    content = content
                )
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    val lowercase = word.lowercase(Locale.getDefault())
                    if (!map.containsKey(lowercase)) {
                        val list = mutableListOf(dataCaption)
                        map[lowercase] = list
                        orderList.add(lowercase)
                    } else {
                        if (map[lowercase]!!.size < 3) {
                            map[lowercase]?.add(dataCaption)
                        }
                    }
                }
            }
        }
        setProgressText("从字幕文件中提取出 ${orderList.size} 个单词，正在批量查询单词，如果词典里没有就丢弃")
        val validList = Dictionary.queryList(orderList)
        setProgressText("${validList.size} 个有效单词")
        validList.forEach { word ->
            if (map[word.value] != null) {
                word.captions = map[word.value]!!
            }
        }
        setProgressText("")
        return validList
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }
    return listOf()
}


@OptIn(ExperimentalComposeUiApi::class)
private fun readMKV(
    pathName: String,
    trackId: Int,
    setProgressText: (String) -> Unit,
    setIsSelectedASS: (Boolean) -> Unit
): List<Word> {
    val map: MutableMap<String, ArrayList<Caption>> = HashMap()
    val orderList = mutableListOf<String>()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        setProgressText("正在解析 MKV 文件")

        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return listOf()
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            return listOf()
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()


        /**
         *   OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
         *       for (int i = 0; i < reader.getSubtitles().size(); i++) {
         *         if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
         *           SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
         *           System.out.println(subs.getHeader());
         *         }
         *       }
         *
         *
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }
        setProgressText("正在分词")
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            val model = TokenizerModel(inputStream)
            val tokenizer: Tokenizer = TokenizerME(model)
            val subtitle = reader.subtitles[trackId]
            if (subtitle is SSASubtitles) {
                setIsSelectedASS(true)
            } else {
                setIsSelectedASS(false)
                val captionList = subtitle.readUnreadSubtitles()
                for (caption in captionList) {
                    var content = replaceSpecialCharacter(caption.stringData)
                    content = removeLocationInfo(content)
                    val dataCaption = Caption(
                        start = caption.startTime.format().toString(),
                        end = caption.endTime.format(),
                        content = content
                    )

                    content = content.lowercase(Locale.getDefault())
                    val tokenize = tokenizer.tokenize(content)
                    for (word in tokenize) {
                        if (!map.containsKey(word)) {
                            val list = ArrayList<Caption>()
                            list.add(dataCaption)
                            map[word] = list
                            orderList.add(word)
                        } else {

                            if (map[word]!!.size < 3) {
                                map[word]!!.add(dataCaption)
                            }
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        JOptionPane.showMessageDialog(null,e.message)
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    setProgressText("从视频中提取出${orderList.size}个单词，正在批量查询单词，如果词典里没有就丢弃")
//    val validSet = Dictionary.querySet(map.keys)
    val validList = Dictionary.queryList(orderList)
    setProgressText("${validList.size}个有效单词")
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    setProgressText("")
    return validList
}

/**
 * 替换一些特殊字符
 */
fun replaceSpecialCharacter(captionContent: String): String {
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

/** 有一些字幕并不是在一个的固定位置，而是标注在人物旁边，这个函数删除位置信息 */
fun removeLocationInfo(content: String): String {
    val pattern = Pattern.compile("\\{.*\\}")
    val matcher = pattern.matcher(content)
    return matcher.replaceAll("")
}

fun removeItalicSymbol(content: String): String {
    var string = content
    if (string.contains("<i>")) {
        string = string.replace("<i>", "")
    }
    if (string.contains("</i>")) {
        string = string.replace("</i>", "")
    }
    return string
}

fun replaceNewLine(content: String): String {
    var string = content
    if (string.contains("\r\n")) {
        string = string.replace("\r\n", " ")
    }
    if (string.contains("\n")) {
        string = string.replace("\n", " ")
    }
    if (string.contains("<br />")) {
        string = string.replace("<br />", " ")
    }
    if (string.endsWith(" ")){
        string = string.substring(0,string.length-1)
    }
    return string
}