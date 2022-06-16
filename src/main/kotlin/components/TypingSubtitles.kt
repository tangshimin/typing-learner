package components

import LocalCtrl
import Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.subtitles.SRTSubtitles
import com.matthewn4444.ebml.subtitles.SSASubtitles
import data.Caption
import dialog.removeItalicSymbol
import dialog.removeLocationInfo
import dialog.replaceNewLine
import kotlinx.coroutines.launch
import player.*
import state.GlobalState
import state.SubtitlesState
import state.getSettingsDirectory
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.TransferHandler
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TypingSubtitles(
    subtitlesState: SubtitlesState,
    globalState: GlobalState,
    saveSubtitlesState: () -> Unit,
    saveGlobalState: () -> Unit,
    setIsDarkTheme: (Boolean) -> Unit,
    backToHome: () -> Unit,
    isOpenSettings: Boolean,
    setIsOpenSettings: (Boolean) -> Unit,
    window: ComposeWindow,
    title: String,
    playerWindow: JFrame,
    videoVolume: Float,
    mediaPlayerComponent: Component,
    futureFileChooser: FutureTask<JFileChooser>,
    openLoadingDialog: () -> Unit,
    closeLoadingDialog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val captionList = remember { mutableStateListOf<Caption>() }
    var isPlaying by remember { mutableStateOf(false) }
    var showOpenFile by remember { mutableStateOf(false) }
    var selectedPath by remember { mutableStateOf("") }
    var showSelectTrack by remember { mutableStateOf(false) }
    val trackList = remember { mutableStateListOf<Pair<Int, String>>() }
    var textRect by remember{ mutableStateOf(Rect(0.0F,0.0F,0.0F,0.0F))}
    val videoPlayerBounds by remember { mutableStateOf(Rectangle(0, 0, 540, 303)) }
    val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }
    var loading by remember { mutableStateOf(false) }
    var mediaType by remember { mutableStateOf(computeMediaType(subtitlesState.mediaPath)) }
    var pgUp by remember { mutableStateOf(false) }
    val audioPlayerComponent = LocalAudioPlayerComponent.current

    /** 读取字幕文件*/
    if (subtitlesState.subtitlesPath.isNotEmpty() && captionList.isEmpty()) {
        parseSubtitles(
            subtitlesPath = subtitlesState.subtitlesPath,
            setMaxLength = {
                scope.launch {
                    subtitlesState.sentenceMaxLength = it
                    saveSubtitlesState()
                }
            },
            setCaptionList = {
                captionList.clear()
                captionList.addAll(it)
            },
            resetSubtitlesState = {
                subtitlesState.mediaPath = ""
                subtitlesState.subtitlesPath = ""
                subtitlesState.trackID = 0
                subtitlesState.trackDescription = ""
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.sentenceMaxLength = 0
            }
        )
    }

    /** 播放按键音效 */
    val playKeySound = {
        if (globalState.isPlayKeystrokeSound) {
            playSound("audio/keystroke.wav", globalState.keystrokeVolume)
        }
    }

    /** 设置字幕列表的被回调函数 */
    val setTrackList: (List<Pair<Int, String>>) -> Unit = {
        trackList.clear()
        trackList.addAll(it)
    }
    /** 支持的媒体类型 */
    val formatList = listOf("wav","mp3","aac","mp4","mkv")
    /** 支持的音频类型*/
    val audioFormatList = listOf("wav","mp3","aac")
    /** 支持的视频类型 */
    val videoFormatList = listOf("mp4","mkv")

    /** 解析打开的文件 */
    val parseImportFile: (List<File>,OpenMode) -> Unit = {files,openMode ->
        if(files.size == 1){
            val file = files.first()
            loading = true
            scope.launch {
                Thread(Runnable{
                    if (file.extension == "mkv") {
                        if (subtitlesState.mediaPath != file.absolutePath) {
                            selectedPath = file.absolutePath
                            parseTrackList(
                                mediaPlayerComponent,
                                window,
                                playerWindow,
                                file.absolutePath,
                                setTrackList = { setTrackList(it) },
                            )

                        } else {
                            JOptionPane.showMessageDialog(window, "文件已打开")
                        }

                    }else if (formatList.contains(file.extension)) {
                        JOptionPane.showMessageDialog(window, "需要同时选择 ${file.extension} 视频 + srt 字幕")
                    }else if (file.extension == "srt") {
                        JOptionPane.showMessageDialog(window, "需要同时选择1个视频(mp4、mkv) + 1个srt 字幕")
                    }else if (file.extension == "json") {
                        JOptionPane.showMessageDialog(window, "想要打开词库文件，需要先切换到记忆单词界面")
                    } else {
                        JOptionPane.showMessageDialog(window, "格式不支持")
                    }
                    loading = false
                }).start()
            }
        }else if(files.size == 2){
            val first = files.first()
            val last = files.last()
            val modeString = if(openMode== OpenMode.Open) "打开" else "拖拽"


            if(first.extension == "srt" && formatList.contains(last.extension)){
                subtitlesState.trackID = -1
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.subtitlesPath = first.absolutePath
                subtitlesState.mediaPath = last.absolutePath
                subtitlesState.trackDescription = first.nameWithoutExtension
                captionList.clear()
                mediaType = computeMediaType(subtitlesState.mediaPath)
                if(openMode == OpenMode.Open) showOpenFile = false
            }else if(formatList.contains(first.extension) && last.extension == "srt"){
                subtitlesState.trackID = -1
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.mediaPath = first.absolutePath
                subtitlesState.subtitlesPath = last.absolutePath
                subtitlesState.trackDescription = last.nameWithoutExtension
                captionList.clear()
                mediaType = computeMediaType(subtitlesState.mediaPath)
                if(openMode == OpenMode.Open) showOpenFile = false
            }else if(first.extension == "mp4" && last.extension == "mp4"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个 MP4 格式的视频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(first.extension == "mkv" && last.extension == "mkv"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个 MKV 格式的视频，\n"
                        +"可以选择一个有字幕的 mkv 格式的视频，\n或者一个 MKV 格式的视频和1个 srt 字幕")
            }else if(first.extension == "srt" && last.extension == "srt"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个字幕，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(videoFormatList.contains(first.extension) && videoFormatList.contains(last.extension)){
                JOptionPane.showMessageDialog(window, "${modeString}了2个视频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(audioFormatList.contains(first.extension) &&  audioFormatList.contains(last.extension)){
                JOptionPane.showMessageDialog(window, "${modeString}了2个音频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else {
                JOptionPane.showMessageDialog(window, "文件格式不支持")
            }
        }else{
            JOptionPane.showMessageDialog(window, "不能超过两个文件")
        }

    }

    /** 打开文件对话框 */
    val openFileChooser: () -> Unit = {

        // 打开 windows 的文件选择器很慢，有时候会等待超过2秒
        openLoadingDialog()

        Thread(Runnable{
            val fileChooser = futureFileChooser.get()
            fileChooser.dialogTitle = "打开"
            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
            fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.isAcceptAllFileFilterUsed = false
            fileChooser.isMultiSelectionEnabled = true
            val fileFilter = FileNameExtensionFilter("1个 mkv 视频，或 1个媒体(mp3、wav、aac、mp4、mkv) + 1个字幕(srt)","mp3","wav","aac","mkv","srt","mp4")
            fileChooser.addChoosableFileFilter(fileFilter)
            fileChooser.selectedFile = null
            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                val files = fileChooser.selectedFiles.toList()
                parseImportFile(files,OpenMode.Open)
                closeLoadingDialog()
            } else {
                closeLoadingDialog()
            }
            fileChooser.selectedFile = null
            fileChooser.isMultiSelectionEnabled = false
            fileChooser.removeChoosableFileFilter(fileFilter)
        }).start()

    }

    /**  使用按钮播放视频时调用的回调函数   */
    val playCurrentCaption: (Caption) -> Unit = { caption ->
        val file = File(subtitlesState.mediaPath)
        if (file.exists() ) {
            if (!isPlaying) {
                scope.launch {
                    isPlaying = true
                    val playTriple = Triple(caption, subtitlesState.mediaPath, subtitlesState.trackID)
                    // 使用内部字幕轨道
                    if(subtitlesState.trackID != -1){
                        play(
                            window = playerWindow,
                            setIsPlaying = { isPlaying = it },
                            volume = videoVolume,
                            playTriple = playTriple,
                            videoPlayerComponent = mediaPlayerComponent,
                            bounds = videoPlayerBounds
                        )
                        // 使用外部字幕
                    }else{
                        if(file.extension == "wav" || file.extension == "mp3"|| file.extension == "aac"){
                            play(
                                setIsPlaying = {isPlaying = it},
                                audioPlayerComponent = audioPlayerComponent,
                                volume = videoVolume,
                                caption = caption,
                                videoPath = subtitlesState.mediaPath,
                                subtitlePath = subtitlesState.subtitlesPath
                            )
                        }else{
                            play(
                                window= playerWindow,
                                setIsPlaying = { isPlaying = it },
                                videoPlayerComponent= mediaPlayerComponent,
                                volume= videoVolume,
                                caption=caption,
                                videoPath=subtitlesState.mediaPath,
                                subtitlePath=subtitlesState.subtitlesPath,
                                bounds= videoPlayerBounds
                            )
                        }
                    }
                }
            }

        } else {
            JOptionPane.showMessageDialog(window, "视频地址错误")
        }

    }

    /** 保存轨道 ID 时被调用的回调函数 */
    val saveTrackID: (Int) -> Unit = {
        scope.launch {
            subtitlesState.trackID = it
            saveSubtitlesState()
        }
    }

    /** 保存轨道名称时被调用的回调函数 */
    val saveTrackDescription: (String) -> Unit = {
        scope.launch {
            subtitlesState.trackDescription = it
            saveSubtitlesState()
        }
    }

    /** 保存轨道数量时被调用的回调函数 */
    val saveTrackSize: (Int) -> Unit = {
        scope.launch {
            subtitlesState.trackSize = it
            saveSubtitlesState()
        }
    }

    /** 保存视频路径时被调用的回调函数 */
    val saveVideoPath: (String) -> Unit = {
        subtitlesState.mediaPath = it
        mediaType = "video"
        saveSubtitlesState()
    }

    /** 保存一个新的字幕时被调用的回调函数 */
    val saveSubtitlesPath: (String) -> Unit = {
        scope.launch {
            subtitlesState.subtitlesPath = it
            subtitlesState.firstVisibleItemIndex = 0
            subtitlesState.currentIndex = 0
            focusManager.clearFocus()
            /** 把之前的字幕列表清除才能触发解析字幕的函数重新运行 */
            captionList.clear()
            saveSubtitlesState()
        }
    }

    /** 设置是否启用击键音效时被调用的回调函数 */
    val setIsPlayKeystrokeSound: (Boolean) -> Unit = {
        scope.launch {
            globalState.isPlayKeystrokeSound = it
            saveGlobalState()
        }
    }

    /** 选择字幕轨道 */
    val selectTypingSubTitles:() -> Unit = {
        if (trackList.isEmpty()) {
            loading = true
            scope.launch {
                showSelectTrack = true
                Thread(Runnable{
                    parseTrackList(
                        mediaPlayerComponent,
                        window,
                        playerWindow,
                        subtitlesState.mediaPath,
                        setTrackList = {
                            setTrackList(it)
                        },
                    )
                    loading = false

                }).start()

            }

        }
    }

    /** 设置当前字幕的可见性 */
    val setCurrentCaptionVisible: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.currentCaptionVisible = !subtitlesState.currentCaptionVisible
            saveSubtitlesState()
        }
    }

    /** 设置未抄写字幕的可见性 */
    val setNotWroteCaptionVisible: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.notWroteCaptionVisible = !subtitlesState.notWroteCaptionVisible
            saveSubtitlesState()
        }
    }
    /** 当前界面的快捷键 */
    val boxKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            (keyEvent.isCtrlPressed && keyEvent.key == Key.W && keyEvent.type == KeyEventType.KeyUp) -> {
                backToHome()
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.O && keyEvent.type == KeyEventType.KeyUp) -> {
                openFileChooser()
                showOpenFile = true
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.S && keyEvent.type == KeyEventType.KeyUp) -> {
                if(subtitlesState.trackSize > 1){
                    selectTypingSubTitles()
                }
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.H && keyEvent.type == KeyEventType.KeyUp) -> {
                setCurrentCaptionVisible(!subtitlesState.currentCaptionVisible)
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.G && keyEvent.type == KeyEventType.KeyUp) -> {
                setNotWroteCaptionVisible(!subtitlesState.notWroteCaptionVisible)
                true
            }

            (keyEvent.isCtrlPressed && keyEvent.key == Key.D && keyEvent.type == KeyEventType.KeyUp) -> {
                setIsDarkTheme(!globalState.isDarkTheme)
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.M && keyEvent.type == KeyEventType.KeyUp) -> {
                setIsPlayKeystrokeSound(!globalState.isPlayKeystrokeSound)
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.One && keyEvent.type == KeyEventType.KeyUp) -> {
                setIsOpenSettings(!isOpenSettings)
                true
            }
            ((keyEvent.key == Key.Tab) && keyEvent.type == KeyEventType.KeyUp) -> {
                val caption = captionList[subtitlesState.currentIndex]
                playCurrentCaption(caption)
                true
            }
            else -> false
        }
    }


    /**  处理拖放文件的函数 */
    val transferHandler = createTransferHandler(
        singleFile = false,
        showWrongMessage = { message ->
            JOptionPane.showMessageDialog(window, message)
        },
        parseImportFile = { parseImportFile(it,OpenMode.Drag) }
    )

    window.transferHandler = transferHandler
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
            .focusRequester(focusRequester)
            .onKeyEvent(boxKeyEvent)
            .focusable()
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Row(Modifier.fillMaxSize()) {
            SubtitlesSidebar(
                isOpen = isOpenSettings,
                currentCaptionVisible = subtitlesState.currentCaptionVisible,
                setCurrentCaptionVisible = {setCurrentCaptionVisible(it)},
                notWroteCaptionVisible = subtitlesState.notWroteCaptionVisible,
                setNotWroteCaptionVisible = {setNotWroteCaptionVisible(it)},
                backToHome = { backToHome() },
                trackSize = subtitlesState.trackSize,
                openFile = { showOpenFile = true },
                openFileChooser = { openFileChooser() },
                selectTrack = { selectTypingSubTitles() },
                isDarkTheme = globalState.isDarkTheme,
                setIsDarkTheme = { setIsDarkTheme(it) },
                isPlayKeystrokeSound = globalState.isPlayKeystrokeSound,
                setIsPlayKeystrokeSound = { setIsPlayKeystrokeSound(it) },
            )
            val topPadding = if (isMacOS()) 30.dp else 0.dp
            if (isOpenSettings) {
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(Modifier.fillMaxSize().padding(top = topPadding)) {

                if (captionList.isNotEmpty()) {

                    val listState = rememberLazyListState(subtitlesState.firstVisibleItemIndex)
                    val stateHorizontal = rememberScrollState(0)
                    val isAtTop by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                        }
                    }
                    val startTimeWidth = 50.dp
                    val endPadding = 10.dp
                    val maxWidth = startTimeWidth + endPadding + (subtitlesState.sentenceMaxLength * 13).dp
                    val indexWidth = (captionList.size.toString().length * 14).dp
                    LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1050.dp)
                            .fillMaxHeight()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .horizontalScroll(stateHorizontal),
                    ) {
                        itemsIndexed(captionList) { index, caption ->
                            val captionContent = caption.content
                            val typingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }
                            var textFieldValue by remember { mutableStateOf("") }
                            var selectable by remember { mutableStateOf(false) }
                            val selectRequester = remember { FocusRequester() }
                            val textFieldRequester = remember { FocusRequester() }
                            val next :() -> Unit = {
                                scope.launch {
                                    val end =
                                        listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size - 2
                                    if (index >= end) {
                                        listState.scrollToItem(index)
                                    }
                                   if(index+1 != captionList.size){
                                       focusManager.moveFocus(FocusDirection.Next)
                                       focusManager.moveFocus(FocusDirection.Next)
                                       focusManager.moveFocus(FocusDirection.Next)
                                   }
                                }
                            }
                            val previous :() -> Unit = {
                                scope.launch {
                                    if(index == listState.firstVisibleItemIndex+1){
                                        var top = index - listState.layoutInfo.visibleItemsInfo.size
                                        if(top < 0) top = 0
                                        listState.scrollToItem(top)
                                        subtitlesState.currentIndex = index-1
                                        pgUp = true
                                    }else if(subtitlesState.currentIndex > 0){
                                        focusManager.moveFocus(FocusDirection.Previous)
                                        focusManager.moveFocus(FocusDirection.Previous)
                                    }

                                }
                            }
                            /** 检查输入的回调函数 */
                            val checkTyping: (String) -> Unit = { input ->
                                scope.launch {
                                    if (textFieldValue.length > captionContent.length) {
                                        typingResult.clear()
                                        textFieldValue = ""

                                    } else if (input.length <= captionContent.length) {
                                        textFieldValue = input
                                        typingResult.clear()
                                        val inputChars = input.toMutableList()
                                        for (i in inputChars.indices) {
                                            val inputChar = inputChars[i]
                                            val char = captionContent[i]
                                            if (inputChar == char) {
                                                typingResult.add(Pair(inputChar, true))
                                                // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                                            } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                                typingResult.add(Pair(char, true))
                                                // 音乐符号不好输入，所以可以使用空格替换
                                            }else if (inputChar == ' ' && (char == '♪')) {
                                                typingResult.add(Pair(char, true))
//                                              // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                                                inputChars.add(i,'♪')
                                                inputChars.removeAt(i+1)
                                                textFieldValue = String(inputChars.toCharArray())
                                            } else {
                                                typingResult.add(Pair(inputChar, false))
                                            }
                                        }
                                        if(input.length >= captionContent.length){
                                            next()
                                        }

                                    }else{
                                        next()
                                    }
                                }
                            }

                            val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                when {
                                    ((it.key != Key.ShiftLeft && it.key != Key.ShiftRight) && it.type == KeyEventType.KeyDown) -> {
                                        playKeySound()
                                        true
                                    }
                                    ((it.key == Key.Enter ||it.key == Key.NumPadEnter || it.key == Key.DirectionDown) && it.type == KeyEventType.KeyUp) -> {
                                        next()
                                        true
                                    }

                                    ((it.key == Key.DirectionUp) && it.type == KeyEventType.KeyUp) -> {
                                        previous()
                                        true
                                    }
                                    ((it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current-20)
                                        }
                                        true
                                    }
                                    ((it.key == Key.DirectionRight) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current+20)
                                        }
                                        true
                                    }
                                    (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch { selectable = !selectable }
                                        true
                                    }
                                    else -> false
                                }

                            }
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(maxWidth)
                                    .padding(start = 150.dp)
                            ) {
                                val alpha = if(subtitlesState.currentIndex == index) ContentAlpha.high else ContentAlpha.medium
                                val lineColor =  if(index <  subtitlesState.currentIndex){
                                    MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium)
                                }else if(subtitlesState.currentIndex == index){
                                    if(subtitlesState.currentCaptionVisible){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }else{
                                    if(subtitlesState.notWroteCaptionVisible){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }
                                val indexColor =  if(index <=  subtitlesState.currentIndex){
                                    MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium)
                                }else{
                                    if(subtitlesState.notWroteCaptionVisible){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }

                                Row(modifier = Modifier.width(indexWidth)){
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = indexColor,
                                                    fontSize = MaterialTheme.typography.h5.fontSize,
                                                    letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                    fontFamily = MaterialTheme.typography.h5.fontFamily,
                                                )
                                            ) {
                                                append("${index+1}")
                                            }
                                        },
                                    )
                                }

                                Spacer(Modifier.width(20.dp))
                                Box(Modifier.width(IntrinsicSize.Max)) {
                                    if (subtitlesState.currentIndex == index) {
                                        Divider(
                                            Modifier.align(Alignment.BottomCenter)
                                                .background(MaterialTheme.colors.primary)
                                        )
                                    }

                                    CompositionLocalProvider(
                                        LocalTextInputService provides null
                                    ) {
                                        BasicTextField(
                                            value = textFieldValue,
                                            onValueChange = { checkTyping(it) },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            textStyle = MaterialTheme.typography.h5.copy(
                                                color = Color.Transparent,
                                                fontFamily = monospace
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 5.dp)
                                                .align(Alignment.CenterStart)
                                                .focusable()
                                                .onKeyEvent { textFieldKeyEvent(it) }
                                                .focusRequester(textFieldRequester)
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        scope.launch {
                                                            subtitlesState.currentIndex = index
                                                            subtitlesState.firstVisibleItemIndex =
                                                                listState.firstVisibleItemIndex
                                                            saveSubtitlesState()
                                                        }
                                                    } else if (textFieldValue.isNotEmpty()) {
                                                        typingResult.clear()
                                                        textFieldValue = ""
                                                    }
                                                }
                                        )
                                        if(pgUp){
                                            SideEffect {
                                                if(subtitlesState.currentIndex == index){
                                                    textFieldRequester.requestFocus()
                                                    pgUp = false
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = buildAnnotatedString {

                                            typingResult.forEach { (char, correct) ->
                                                if (correct) {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = MaterialTheme.colors.primary.copy(alpha = alpha),
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = monospace,
                                                        )
                                                    ) {
                                                        append(char)
                                                    }
                                                } else {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = Color.Red,
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = monospace,
                                                        )
                                                    ) {
                                                        if (char == ' ') {
                                                            append("_")
                                                        } else {
                                                            append(char)
                                                        }

                                                    }
                                                }
                                            }
                                            var remainChars = captionContent.substring(typingResult.size)


                                            withStyle(
                                                style = SpanStyle(
                                                    color = lineColor,
                                                    fontSize = MaterialTheme.typography.h5.fontSize,
                                                    letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                    fontFamily = monospace,
                                                )
                                            ) {
                                                append(remainChars)
                                            }
                                        },
                                        textAlign = TextAlign.Start,
                                        color = MaterialTheme.colors.onBackground,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(bottom = 5.dp)
                                            .onGloballyPositioned { coordinates ->
                                            if (subtitlesState.currentIndex == index) {
                                                // 如果视频播放按钮被遮挡，就使用这个位置计算出视频播放器的位置
                                                textRect = coordinates.boundsInWindow()
                                            }

                                        }
                                    )


                                    DropdownMenu(
                                        expanded = selectable,
                                        focusable = true,
                                        onDismissRequest = {
                                            selectable = false
                                        },
                                        offset = DpOffset(0.dp, (-50).dp)
                                    ) {
                                        BasicTextField(
                                            value = captionContent,
                                            onValueChange = {},
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            textStyle = MaterialTheme.typography.h5.copy(
                                                fontFamily = monospace,
                                                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                                            ),
                                            modifier = Modifier.focusable()
                                                .height(32.dp)
                                                .focusRequester(selectRequester)
                                                .onKeyEvent {
                                                    if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                                        scope.launch { selectable = !selectable }
                                                        true
                                                    } else false
                                                }
                                        )
                                        LaunchedEffect(Unit) {
                                            selectRequester.requestFocus()
                                        }

                                    }

                                }

                                Row(Modifier.width(48.dp).height(IntrinsicSize.Max)) {
                                    if (subtitlesState.currentIndex == index) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    elevation = 4.dp,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                    ),
                                                    shape = RectangleShape
                                                ) {
                                                    Row(modifier = Modifier.padding(10.dp)){
                                                        Text(text = "播放" )
                                                        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                                            Text(text = " Tab")
                                                        }
                                                    }

                                                }
                                            },
                                            delayMillis = 300,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.TopCenter,
                                                alignment = Alignment.TopCenter,
                                                offset = DpOffset.Zero
                                            )
                                        ) {
                                            val density = LocalDensity.current.density
                                            IconButton(onClick = {
                                                playCurrentCaption(caption)
                                            },
                                                modifier = Modifier
                                                    .onKeyEvent {
                                                        if (it.key == Key.Spacebar && it.type == KeyEventType.KeyUp) {
                                                            playCurrentCaption(caption)
                                                            true
                                                        } else false
                                                    }
                                                    .onGloballyPositioned { coordinates ->
                                                        val rect = coordinates.boundsInWindow()
                                                        if(!rect.isEmpty){
                                                            // 视频播放按钮没有被遮挡
                                                            videoPlayerBounds.x = window.x + rect.left.toInt() + (48 * density).toInt()
                                                            videoPlayerBounds.y = window.y + rect.top.toInt() - (100 * density).toInt()
                                                        }else{
                                                            // 视频播放按钮被遮挡
                                                            videoPlayerBounds.x = window.x + textRect.right.toInt()
                                                            videoPlayerBounds.y = window.y + textRect.top.toInt() - (100 * density).toInt()
                                                        }

                                                        val graphicsDevice =
                                                            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
                                                        // 只要一个显示器时，才判断屏幕边界
                                                        if(GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size == 1){
                                                            val width = graphicsDevice.displayMode.width
                                                            val height = graphicsDevice.displayMode.height
                                                            val actualWidth = (540 * density).toInt()
                                                            if (videoPlayerBounds.x + actualWidth > width) {
                                                                videoPlayerBounds.x = width - actualWidth
                                                            }
                                                            val actualHeight = (330 * density).toInt()
                                                            if (videoPlayerBounds.y < 0) videoPlayerBounds.y = 0
                                                            if (videoPlayerBounds.y + actualHeight > height) {
                                                                videoPlayerBounds.y = height - actualHeight
                                                            }
                                                        }


                                                        // 显示器缩放
                                                        if(density != 1f){
                                                            videoPlayerBounds.x = videoPlayerBounds.x.div(density).toInt()
                                                            videoPlayerBounds.y =  videoPlayerBounds.y.div(density).toInt()
                                                        }
                                                    }
                                            ) {
                                                val icon = if(mediaType=="audio" && !isPlaying) {
                                                    Icons.Filled.VolumeDown
                                                } else if(mediaType=="audio" && isPlaying){
                                                    Icons.Filled.VolumeUp
                                                }else Icons.Filled.PlayArrow

                                                Icon(
                                                    icon,
                                                    contentDescription = "Localized description",
                                                    tint = if(isPlaying)MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                                                )
                                            }

                                        }

                                    }
                                }

                            }

                        }
                    }

                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                    HorizontalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        adapter = rememberScrollbarAdapter(stateHorizontal)
                    )
                    if (!isAtTop) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(0)
                                    subtitlesState.currentIndex = 0
                                    subtitlesState.firstVisibleItemIndex = 0
                                    focusManager.clearFocus()
                                    saveSubtitlesState()
                                }
                            },
                            backgroundColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 100.dp, bottom = 100.dp)
                        ) {
                            Icon(
                                Icons.Filled.North,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }

                if (showOpenFile || selectedPath.isNotEmpty() || captionList.isEmpty()) {
                    OpenFileComponent(
                        parentComponent = window,
                        cancel = { showOpenFile = false },
                        openFileChooser = { openFileChooser() },
                        showCancel = captionList.isNotEmpty(),
                        setTrackId = { saveTrackID(it) },
                        setTrackDescription = { saveTrackDescription(it) },
                        trackList = trackList,
                        setTrackList = { setTrackList(it) },
                        setVideoPath = { saveVideoPath(it) },
                        selectedPath = selectedPath,
                        setSelectedPath = { selectedPath = it },
                        setSubtitlesPath = { saveSubtitlesPath(it) },
                        setTrackSize = { saveTrackSize(it) },
                    )
                }
                if (showSelectTrack) {
                    Box(
                        Modifier.fillMaxSize()
                            .align(Alignment.Center)
                            .background(MaterialTheme.colors.background)
                    ) {
                        Row(Modifier.align(Alignment.Center)) {
                            SelectTrack(
                                close = { showSelectTrack = false },
                                parentComponent = window,
                                setTrackId = { saveTrackID(it) },
                                setTrackDescription = { saveTrackDescription(it) },
                                trackList = trackList,
                                setTrackList = { setTrackList(it) },
                                setVideoPath = { saveVideoPath(it) },
                                selectedPath = subtitlesState.mediaPath,
                                setSelectedPath = { selectedPath = it },
                                setSubtitlesPath = { saveSubtitlesPath(it) },
                                setTrackSize = { saveTrackSize(it) },
                                setIsLoading = { loading = it }
                            )
                            OutlinedButton(onClick = {
                                showSelectTrack = false
                                setTrackList(listOf())
                            }) {
                                Text("取消")
                            }
                        }

                    }
                }
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 200.dp)
                    )
                }
            }

        }

        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            )
        }
        Settings(
            isOpen = isOpenSettings,
            setIsOpen = { setIsOpenSettings(it) },
            modifier = Modifier.align(Alignment.TopStart)
        )

    }

}

enum class OpenMode {
    Open, Drag,
}
@Composable
fun OpenFileComponent(
    parentComponent: Component,
    cancel: () -> Unit,
    openFileChooser: () -> Unit,
    showCancel: Boolean,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
) {

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        var loading by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(IntrinsicSize.Max).align(Alignment.Center)
        ) {

            Text(
                text = "可以拖放一个有字幕的 MKV 视频或\n"+
                        "字幕(SRT) + 媒体(MP3、WAV、AAC、MP4、MKV)到这里\n",
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(top = 14.dp,end = 20.dp)
            )
            OutlinedButton(
                modifier = Modifier.padding(end = 20.dp),
                onClick = { openFileChooser() }) {
                Text("打开")
            }

            SelectTrack(
                close = { cancel() },
                parentComponent = parentComponent,
                setTrackId = { setTrackId(it) },
                setTrackDescription = { setTrackDescription(it) },
                trackList = trackList,
                setTrackList = { setTrackList(it) },
                setVideoPath = { setVideoPath(it) },
                selectedPath = selectedPath,
                setSelectedPath = { setSelectedPath(it) },
                setSubtitlesPath = { setSubtitlesPath(it) },
                setTrackSize = { setTrackSize(it) },
                setIsLoading = { loading = it }
            )
            if (showCancel) {
                OutlinedButton(onClick = {
                    setTrackList(listOf())
                    setSelectedPath("")
                    cancel()
                }) {
                    Text("取消")
                }
            }
        }
        if (loading) {
            CircularProgressIndicator(Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 200.dp))
        }
    }

}

@Composable
fun SelectTrack(
    close: () -> Unit,
    parentComponent: Component,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
    setIsLoading: (Boolean) -> Unit,
) {
    if (trackList.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        var selectedSubtitle by remember { mutableStateOf("    ") }
        Box(Modifier.width(IntrinsicSize.Max).padding(end = 20.dp)) {
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
                trackList.forEach { (trackId, description) ->
                    DropdownMenuItem(
                        onClick = {
                            setIsLoading(true)
                            Thread(Runnable {
                                expanded = false
                                val subtitles = writeToFile(selectedPath, trackId,parentComponent)
                                if (subtitles != null) {
                                    setSubtitlesPath(subtitles.absolutePath)
                                    setTrackId(trackId)
                                    setTrackDescription(description)
                                    setVideoPath(selectedPath)

                                    setTrackSize(trackList.size)
                                    setTrackList(listOf())
                                    setSelectedPath("")
                                    close()
                                }
                                setIsLoading(false)

                            }).start()
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

@Composable
fun Settings(
    modifier: Modifier
) {
    Icon(
        Icons.Filled.ArrowBack,
        contentDescription = "Localized description",
        tint = MaterialTheme.colors.primary,
        modifier = modifier,
    )
}

@Composable
fun SubtitlesSidebar(
    isOpen: Boolean,
    currentCaptionVisible: Boolean,
    setCurrentCaptionVisible:(Boolean) -> Unit,
    notWroteCaptionVisible: Boolean,
    setNotWroteCaptionVisible:(Boolean) -> Unit,
    isDarkTheme: Boolean,
    setIsDarkTheme: (Boolean) -> Unit,
    isPlayKeystrokeSound: Boolean,
    setIsPlayKeystrokeSound: (Boolean) -> Unit,
    trackSize: Int,
    backToHome: () -> Unit,
    openFile: () -> Unit,
    openFileChooser: () -> Unit,
    selectTrack: () -> Unit,
) {
    if (isOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 78.dp else 48.dp))
            Divider()
            val ctrl = LocalCtrl.current
            val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(48.dp).clickable { backToHome() }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("记忆单词", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+W",
                        color = MaterialTheme.colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        openFileChooser()
                        openFile()
                    }
                    .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("打开文件", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+O",
                        color = MaterialTheme.colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

            if (trackSize > 1) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { selectTrack() }
                        .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("选择字幕", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+S",
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("当前字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+H",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = currentCaptionVisible,
                    onCheckedChange = { setCurrentCaptionVisible(!currentCaptionVisible) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("未写字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+G",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = notWroteCaptionVisible,
                    onCheckedChange = {setNotWroteCaptionVisible(!notWroteCaptionVisible) },
                )
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("深色模式", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+D",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isDarkTheme,
                    onCheckedChange = { setIsDarkTheme(it) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("击键音效", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+M",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isPlayKeystrokeSound,
                    onCheckedChange = { setIsPlayKeystrokeSound(it) },
                )
            }

        }
    }
}

/** 创建拖放处理器
 * @param singleFile 是否只接收单个文件
 * @param parseImportFile 处理导入的文件的函数
 * @param showWrongMessage 显示提示信息的函数
 */
fun createTransferHandler(
    singleFile: Boolean = true,
    parseImportFile: (List<File>) -> Unit,
    showWrongMessage: (String) -> Unit,
): TransferHandler {
    return object : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val transferable = support.transferable
            try {
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                if (singleFile) {
                    if (files.size == 1) {
                        parseImportFile(files)
                    } else {
                        showWrongMessage("一次只能读取一个文件")
                    }
                } else {
                    parseImportFile(files)
                }


            } catch (exception: UnsupportedFlavorException) {
                return false
            } catch (exception: IOException) {
                return false
            }
            return true
        }
    }
}

/**
 * 解析选择的文件，返回字幕名称列表，用于用户选择具体的字幕。
 * @param mediaPlayerComponent VLC 组件
 * @param playerWindow 播放视频的窗口
 * @param videoPath 视频路径
 * @param setTrackList 解析完成后，用来设置字幕列表的回调。
 */
fun parseTrackList(
    mediaPlayerComponent: Component,
    parentComponent: Component,
    playerWindow: JFrame,
    videoPath: String,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
) {
    val result = checkSubtitles(videoPath,parentComponent)
    if(result){
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                val list = mutableListOf<Pair<Int, String>>()
                mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
                    if (index != 0) {
                        list.add(Pair(index - 1, trackDescription.description()))
                    }
                }
                mediaPlayer.controls().pause()
                playerWindow.isAlwaysOnTop = true
                playerWindow.title = "视频播放窗口"
                playerWindow.isVisible = false
                setTrackList(list)
                mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
            }
        })
        playerWindow.title = "正在读取字幕列表"
        playerWindow.isAlwaysOnTop = false
        playerWindow.toBack()
        playerWindow.size = Dimension(10, 10)
        playerWindow.location = Point(0, 0)
        playerWindow.layout = null
        playerWindow.contentPane.add(mediaPlayerComponent)
        playerWindow.isVisible = true
        mediaPlayerComponent.mediaPlayer().media().play(videoPath,":no-sub-autodetect-file")
    }
}

/**
 * 有些文件，可能文件扩展是mkv,但实际内容并不是 mkv
 */
fun checkSubtitles(
    videoPath: String,
    parentComponent: Component):Boolean{
    var reader: EBMLReader? = null

    try {
        reader = EBMLReader(videoPath)
        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            JOptionPane.showMessageDialog(parentComponent, "这不是一个 mkv 格式的视频")
            return false
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
            JOptionPane.showMessageDialog(parentComponent, "这个视频没有字幕")
            return false
        }
    } catch (exception: IOException) {
        exception.printStackTrace()
    } finally {
        try {
            reader?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return true
}

/**
 * 解析字幕，返回最大字符数和字幕列表，用于显示。
 * @param subtitlesPath 字幕的路径
 * @param setMaxLength 用于设置字幕的最大字符数的回调函数
 * @param setCaptionList 用于设置字幕列表的回调函数
 * @param resetSubtitlesState 字幕文件删除，或者被修改，导致不能解析，就重置
 */
fun parseSubtitles(
    subtitlesPath: String,
    setMaxLength: (Int) -> Unit,
    setCaptionList: (List<Caption>) -> Unit,
    resetSubtitlesState:() -> Unit,
) {
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    if(file.exists()){
        try {
            val inputStream: InputStream = FileInputStream(file)
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream)
            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList = mutableListOf<Caption>()
            var maxLength = 0
            for (caption in captions.values) {
                var content = removeLocationInfo(caption.content)
                content = removeItalicSymbol(content)
                content = replaceNewLine(content)

                val newCaption = Caption(
                    start = caption.start.getTime("hh:mm:ss.ms"),
                    end = caption.end.getTime("hh:mm:ss.ms"),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

            setMaxLength(maxLength)
            setCaptionList(captionList)
        }catch (exception:IOException){
            exception.printStackTrace()
            resetSubtitlesState()
        }

    }else{
        println("找不到正在抄写的字幕")
        resetSubtitlesState()
    }

}

/**
 * 提取选择的字幕到用户目录
 * */
private fun writeToFile(
    videoPath: String,
    trackId: Int,
    parentComponent: Component,
): File? {
    var reader: EBMLReader? = null
    val settingsDir = getSettingsDirectory()
    var subtitlesFile: File? = null

    try {
        reader = EBMLReader(videoPath)
        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return subtitlesFile
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
            return subtitlesFile
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()

        /**
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }

        val subtitles = reader.subtitles[trackId]
        if(subtitles is SSASubtitles){
            JOptionPane.showMessageDialog(parentComponent, "暂时不支持 ASS 格式的字幕")
        }else if(subtitles is SRTSubtitles){
            subtitlesFile = File(settingsDir, "subtitles.srt")
            subtitles.writeFile(subtitlesFile.absolutePath)
        }

    } catch (exception: Exception) {
        exception.printStackTrace()
    } finally {
        try {
            reader?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return subtitlesFile
}

/** 计算文件的媒体类型，
 * 如果文件不存在返回默认的媒体类型 video
 */
fun computeMediaType(mediaPath:String):String{
    val file = File(mediaPath)
    if(file.exists()){
        val extension = file.extension
        //  mp3、aac、wav、mp4、mkv，
        return if(extension =="mp3"||extension =="aac"||extension =="wav"){
            "audio"
        }else{
            "video"
        }
    }
    return "video"
}