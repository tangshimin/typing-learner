package components

import LocalCtrl
import Settings
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.window.WindowState
import data.*
import state.MemoryStrategy.*
import dialog.ChapterFinishedDialog
import dialog.ConfirmDialog
import dialog.EditWordDialog
import dialog.SelectChapterDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import state.MutableSpeedState
import state.TypingType
import theme.createColors
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.awt.*
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.*
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule

/**
 * 应用程序的核心组件
 * @param state 应用程序的状态
 * @param videoBounds 视频播放窗口的位置和大小
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun TypingWord(
    window: ComposeWindow,
    title: String,
    audioPlayer: AudioPlayerComponent,
    currentWord: Word?,
    state: AppState,
    videoBounds: Rectangle,
    openSearch: () -> Unit
) {

    /** 协程构建器 */
    val scope = rememberCoroutineScope()

    /** 解析拖放的文件 */
    val parseImportFile: (List<File>) -> Unit = { files ->
        scope.launch {
            val file = files.first()
            if (file.extension == "json") {
                if (state.typingWord.vocabularyPath != file.absolutePath) {
                    val index = state.findVocabularyIndex(file)
                    state.changeVocabulary(file,index)
                } else {
                    JOptionPane.showMessageDialog(window, "词库已打开")
                }

            } else if (file.extension == "mkv") {
                JOptionPane.showMessageDialog(window, "如果想打开 MKV 视频文件抄写字幕，\n需要先切换到抄写字幕界面，\n如果想生成词库需要先打开生成词库界面。")
            } else {
                JOptionPane.showMessageDialog(window, "只能读取 json 格式的词库")
            }
        }
    }

    /**  处理拖放文件的函数 */
    val transferHandler = createTransferHandler(
        showWrongMessage = { message ->
            JOptionPane.showMessageDialog(window, message)
        },
        parseImportFile = { parseImportFile(it) }
    )
    window.transferHandler = transferHandler

    Box(Modifier.background(MaterialTheme.colors.background)) {
        Row {
            TypingWordSidebar(state)
            if (state.openSettings) {
                val topPadding = if (isMacOS()) 30.dp else 0.dp
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(Modifier.fillMaxSize()) {
                val endPadding = 0.dp
                Column(Modifier.align(Alignment.TopCenter)){
                    if (isMacOS()) {
                        Spacer(Modifier.height(10.dp))
                        MacOSTitle(
                            title = title,
                            window = window,
                        )
                    }

                    val text = when(state.memoryStrategy){
                        Normal -> { "${state.typingWord.index + 1}/${state.vocabulary.size}"}
                        Dictation -> { "听写测试   ${state.dictationIndex + 1}/${state.dictationWords.size}"}
                        Review -> {"听写复习   ${state.dictationIndex + 1}/${state.reviewWords.size}"}
                        NormalReviewWrong -> { "复习错误单词   ${state.dictationIndex + 1}/${state.wrongWords.size}"}
                        DictationReviewWrong -> { "听写复习 - 复习错误单词   ${state.dictationIndex + 1}/${state.wrongWords.size}"}
                    }

                    Row(verticalAlignment = Alignment.CenterVertically){
                        val top = if(state.memoryStrategy == Review || state.memoryStrategy == DictationReviewWrong) 0.dp else 10.dp
                        Text(text = text,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(top = top )
                        )
                        if(state.memoryStrategy == Review || state.memoryStrategy == DictationReviewWrong){
                            Spacer(Modifier.width(20.dp))
                            ExitButton(onClick = {
                                state.showInfo()
                                state.memoryStrategy = Normal
                                if( state.wrongWords.isNotEmpty()){
                                    state.wrongWords.clear()
                                }
                                if(state.reviewWords.isNotEmpty()){
                                    state.reviewWords.clear()
                                }

                            })
                        }
                    }
                }




                /** 速度组件的状态 */
                val speed = remember { MutableSpeedState() }

                if (currentWord != null) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .padding(end = endPadding,bottom = 58.dp)
                    ) {

                        val typingWord = state.typingWord

                        /** 是否正在播放视频 */
                        var isPlaying by remember { mutableStateOf(false) }

                        var plyingIndex by remember { mutableStateOf(0) }

                        /** 显示填充后的书签图标 */
                        var showBookmark by remember { mutableStateOf(false) }

                        /** 显示删除对话框 */
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        /** 显示把当前单词加入到熟悉词库的确认对话框 */
                        var showFamiliarDialog by remember { mutableStateOf(false) }

                        /** 单词输入框的焦点请求器*/
                        val wordFocusRequester = remember { FocusRequester() }

                        /** 字幕输入框焦点请求器*/
                        val (focusRequester1,focusRequester2,focusRequester3) = remember { FocusRequester.createRefs() }

                        /** 等宽字体*/
                        val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }

                        /** 单词发音的本地路径 */
                        val audioPath by remember(currentWord){
                            derivedStateOf {
                                getAudioPath(
                                    word = currentWord.value,
                                    audioSet = state.audioSet,
                                    addToAudioSet = {state.audioSet.add(it)},
                                    pronunciation = state.typingWord.pronunciation
                                )
                            }
                        }

                        /** 是否正在播放单词发音 */
                        var isPlayingAudio by remember { mutableStateOf(false) }

                        /**
                         * 用快捷键播放视频时被调用的回调函数
                         * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
                         */
                        @OptIn(ExperimentalSerializationApi::class)
                        val shortcutPlay: (playTriple: Triple<Caption, String, Int>?) -> Unit = { playTriple ->
                            if (playTriple != null && !isPlaying) {
                                scope.launch {
                                    val file = File(playTriple.second)
                                    if (file.exists()) {
                                        isPlaying = true
                                        play(
                                            window = state.videoPlayerWindow,
                                            setIsPlaying = { isPlaying = it },
                                            state.global.videoVolume,
                                            playTriple,
                                            state.videoPlayerComponent,
                                            videoBounds,
                                            typingWord.externalSubtitlesVisible
                                        )
                                    }
                                }
                            }
                        }
                        /** 删除当前单词 */
                        val delete:() -> Unit = {
                            val index = state.typingWord.index
                            state.vocabulary.wordList.removeAt(index)
                            state.vocabulary.size = state.vocabulary.wordList.size
                            if(state.vocabulary.name == "HardVocabulary"){
                                state.hardVocabulary.wordList.remove(currentWord)
                                state.hardVocabulary.size = state.hardVocabulary.wordList.size
                            }

                            state.saveCurrentVocabulary()
                        }
                        /** 把当前单词加入到熟悉词库 */
                        val addToFamiliar:() -> Unit = {
                            val file = getFamiliarVocabularyFile()
                            val familiar =  loadVocabulary(file.absolutePath)
                            if (state.vocabulary.type == VocabularyType.MKV ||
                                state.vocabulary.type == VocabularyType.SUBTITLES
                            ) {
                                currentWord.captions.forEach{caption ->
                                    val externalCaption = ExternalCaption(
                                        relateVideoPath = state.vocabulary.relateVideoPath,
                                        subtitlesTrackId = state.vocabulary.subtitlesTrackId,
                                        subtitlesName = state.vocabulary.name,
                                        start = caption.start,
                                        end = caption.end,
                                        content = caption.content
                                    )
                                    currentWord.externalCaptions.add(externalCaption)
                                }
                                currentWord.captions.clear()

                            }
                            if(!familiar.wordList.contains(currentWord)){
                                familiar.wordList.add(currentWord)
                                familiar.size = familiar.wordList.size
                            }
                            saveVocabulary(familiar,file.absolutePath)
                            delete()
                            showFamiliarDialog = false
                        }

                        /** 处理加入到困难词库的函数 */
                        val bookmarkClick :() -> Unit = {
                            val contains = state.hardVocabulary.wordList.contains(currentWord)
                            if(contains){
                                state.hardVocabulary.wordList.remove(currentWord)
                                if(state.vocabulary.name == "HardVocabulary"){
                                    state.vocabulary.wordList.remove(currentWord)
                                    state.vocabulary.size = state.vocabulary.wordList.size
                                    state.saveCurrentVocabulary()
                                }
                            }else{
                                val relateVideoPath = state.vocabulary.relateVideoPath
                                val subtitlesTrackId = state.vocabulary.subtitlesTrackId
                                val subtitlesName =
                                    if (state.vocabulary.type == VocabularyType.SUBTITLES) state.vocabulary.name else ""
                                val hardWord = currentWord.deepCopy()

                                currentWord.captions.forEach { caption ->
                                    val externalCaption = ExternalCaption(
                                        relateVideoPath,
                                        subtitlesTrackId,
                                        subtitlesName,
                                        caption.start,
                                        caption.end,
                                        caption.content
                                    )
                                    hardWord.externalCaptions.add(externalCaption)
                                }
                                hardWord.captions.clear()
                                state.hardVocabulary.wordList.add(hardWord)
                            }
                            state.hardVocabulary.size = state.hardVocabulary.wordList.size
                            state.saveHardVocabulary()
                    }

                        /** 处理全局快捷键的回调函数 */
                        val globalKeyEvent: (KeyEvent) -> Boolean = {
                            when {
                                (it.isCtrlPressed && it.key == Key.U && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.global.type = TypingType.SUBTITLES
                                        state.saveGlobalState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.T && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.global.type = TypingType.TEXT
                                        state.saveGlobalState()
                                    }
                                    true
                                }

                                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        wordFocusRequester.requestFocus()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.D && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.global.isDarkTheme = !state.global.isDarkTheme
                                        state.colors = createColors(state.global.isDarkTheme, state.global.primaryColor)
                                        state.saveGlobalState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        openSearch()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.phoneticVisible = !state.typingWord.phoneticVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.morphologyVisible = !state.typingWord.morphologyVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.E && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.definitionVisible = !state.typingWord.definitionVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.translationVisible = !state.typingWord.translationVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.wordVisible = !state.typingWord.wordVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }

                                (it.isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
                                    if (!isPlayingAudio) {
                                        playAudio(
                                            audioPath = audioPath,
                                            volume = state.global.audioVolume,
                                            audioPlayerComponent = audioPlayer,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                                            setIsAutoPlay = {}
                                        )
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.Z && it.type == KeyEventType.KeyUp) -> {
                                    if(state.memoryStrategy != Dictation && state.memoryStrategy != Review ){
                                        val playTriple = if (state.vocabulary.type == VocabularyType.DOCUMENT) {
                                            getPayTriple(currentWord, 0)
                                        } else {
                                            val caption = state.getCurrentWord().captions[0]
                                            Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                                        }
                                        plyingIndex = 0
                                        if (playTriple != null && typingWord.subtitlesVisible) focusRequester1.requestFocus()
                                        shortcutPlay(playTriple)
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.X && it.type == KeyEventType.KeyUp) -> {
                                    if(state.memoryStrategy != Dictation && state.memoryStrategy != Review){
                                        val playTriple = if (state.getCurrentWord().externalCaptions.size >= 2) {
                                            getPayTriple(currentWord, 1)
                                        } else if (state.getCurrentWord().captions.size >= 2) {
                                            val caption = state.getCurrentWord().captions[1]
                                            Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                                        }else null
                                        plyingIndex = 1
                                        if (playTriple != null && typingWord.subtitlesVisible) focusRequester2.requestFocus()
                                        shortcutPlay(playTriple)
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                                    if(state.memoryStrategy != Dictation && state.memoryStrategy != Review){
                                        val playTriple = if (state.getCurrentWord().externalCaptions.size >= 3) {
                                            getPayTriple(currentWord, 2)
                                        } else if (state.getCurrentWord().captions.size >= 3) {
                                            val caption = state.getCurrentWord().captions[2]
                                            Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                                        }else null
                                        plyingIndex = 2
                                        if (playTriple != null && typingWord.subtitlesVisible) focusRequester3.requestFocus()
                                        shortcutPlay(playTriple)
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.subtitlesVisible = !state.typingWord.subtitlesVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }

                                (it.isCtrlPressed && it.key == Key.M && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.global.isPlayKeystrokeSound = !state.global.isPlayKeystrokeSound
                                        state.saveGlobalState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.Q && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.isPlaySoundTips = !state.typingWord.isPlaySoundTips
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.openSettings = !state.openSettings
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        state.typingWord.speedVisible = !state.typingWord.speedVisible
                                        state.saveTypingWordState()
                                    }
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        bookmarkClick()
                                    }
                                    showBookmark = true
                                    true
                                }
                                (it.isCtrlPressed && it.key == Key.Y && it.type == KeyEventType.KeyUp) -> {
                                    showFamiliarDialog = true
                                    true
                                }
                                (it.key == Key.Delete && it.type == KeyEventType.KeyUp) -> {
                                    scope.launch {
                                        showDeleteDialog = true
                                    }
                                    true
                                }
                                else -> false
                            }

                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .onKeyEvent { globalKeyEvent(it) }
                                .width(intrinsicSize = IntrinsicSize.Max)
                                .background(MaterialTheme.colors.background)
                                .focusable(true)
                        ) {
                            /** 当前单词的正确次数 */
                            var wordCorrectTime by remember {mutableStateOf(0)}

                            /** 当前单词的错误次数 */
                            var wordWrongTime by remember {mutableStateOf(0)}

                            /** 当前章节的正确数，主要用于听写模式计算正确率 */
                            var chapterCorrectTime by remember { mutableStateOf(0F)}

                            /** 当前章节的错误数，主要用于听写模式计算正确率 */
                            var chapterWrongTime by remember { mutableStateOf(0F)}

                            /** 听写模式的错误单词，主要用于听写模式计算正确率*/
                            val dictationWrongWords = remember { mutableStateMapOf<Word, Int>()}

                            /** 单词输入框里的字符串*/
                            var wordTextFieldValue by remember { mutableStateOf("") }

                            /** 第一条字幕的输入字符串*/
                            var captionsTextFieldValue1 by remember { mutableStateOf("") }

                            /** 第二条字幕的输入字符串*/
                            var captionsTextFieldValue2 by remember { mutableStateOf("") }

                            /** 第三条字幕的输入字符串*/
                            var captionsTextFieldValue3 by remember { mutableStateOf("") }

                            /** 单词输入框输入的结果*/
                            val wordTypingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }

                            /** 字幕输入框的结果 */
                            val captionsTypingResultMap =
                                remember { mutableStateMapOf<Int, MutableList<Pair<Char, Boolean>>>() }

                            /** 显示本章节已经完成对话框 */
                            var showChapterFinishedDialog by remember { mutableStateOf(false) }

                            /** 显示整个词库已经学习完成对话框 */
                            var isVocabularyFinished by remember { mutableStateOf(false) }

                            /** 显示编辑单词对话框 */
                            var showEditWordDialog by remember { mutableStateOf(false) }



                            /** 重置章节计数器,清空听写模式存储的错误单词 */
                            val resetChapterTime: () -> Unit = {
                                chapterCorrectTime = 0F
                                chapterWrongTime = 0F
                                dictationWrongWords.clear()
                            }



                            /** 播放错误音效 */
                            val playBeepSound = {
                                if (state.typingWord.isPlaySoundTips) {
                                    playSound("audio/beep.wav", state.typingWord.soundTipsVolume)
                                }
                            }

                            /** 播放成功音效 */
                            val playSuccessSound = {
                                if (state.typingWord.isPlaySoundTips) {
                                    playSound("audio/hint.wav", state.typingWord.soundTipsVolume)
                                }
                            }

                            /** 播放整个章节完成时音效 */
                            val playChapterFinished = {
                                if (state.typingWord.isPlaySoundTips) {
                                    playSound("audio/Success!!.wav", state.typingWord.soundTipsVolume)
                                }
                            }

                            /** 播放按键音效 */
                            val playKeySound = {
                                if (state.global.isPlayKeystrokeSound) {
                                    playSound("audio/keystroke.wav", state.global.keystrokeVolume)
                                }
                            }

                            /**
                             * 当用户在听写测试按 enter 调用的函数，
                             * 在听写测试跳过单词也算一次错误
                             */
                            val dictationSkipCurrentWord: () -> Unit = {
                                if (wordCorrectTime == 0) {
                                    chapterWrongTime++
                                    val dictationWrongTime = dictationWrongWords[currentWord]
                                    if (dictationWrongTime == null) {
                                        dictationWrongWords[currentWord] = 1
                                    }
                                }
                            }

                            /** 焦点切换到单词输入框 */
                            val jumpToWord:() -> Unit = {
                                wordFocusRequester.requestFocus()
                            }

                            /** 焦点切换到抄写字幕 */
                            val jumpToCaptions:() -> Unit = {
                                if((state.memoryStrategy != Dictation && state.memoryStrategy != Review) &&
                                    typingWord.subtitlesVisible && currentWord.captions.isNotEmpty()
                                ){
                                    focusRequester1.requestFocus()
                                }
                            }

                            /** 清除当前单词的状态 */
                            val clear:() -> Unit = {
                                wordTypingResult.clear()
                                wordTextFieldValue = ""
                                captionsTypingResultMap.clear()
                                captionsTextFieldValue1 = ""
                                captionsTextFieldValue2 = ""
                                captionsTextFieldValue3 = ""
                                wordCorrectTime = 0
                                wordWrongTime = 0
                            }

                            /**
                             * 在听写模式，闭着眼睛听写单词时，刚拼写完单词，就播放这个声音感觉不好，
                             * 在非听写模式下按Enter键就不会有这种感觉，因为按Enter键，
                             * 自己已经输入完成了，有一种期待，预测到了将会播放提示音。
                             */
                            val delayPlaySound:() -> Unit = {
                                Timer("playChapterFinishedSound", false).schedule(1000) {
                                    playChapterFinished()
                                }
                                showChapterFinishedDialog = true
                            }

                            /** 增加复习错误单词时的索引 */
                            val increaseWrongIndex:() -> Unit = {
                                if (state.dictationIndex + 1 == state.wrongWords.size) {
                                    delayPlaySound()
                                } else state.dictationIndex++
                            }

                            /** 切换到下一个单词 */
                            val toNext: () -> Unit = {
                                scope.launch {
                                    clear()

                                    when (state.memoryStrategy) {
                                        Normal -> {
                                            when {
                                                (state.typingWord.index == state.vocabulary.size - 1) -> {
                                                    isVocabularyFinished = true
                                                    playChapterFinished()
                                                    showChapterFinishedDialog = true
                                                }
                                                ((state.typingWord.index + 1) % 20 == 0) -> {
                                                    playChapterFinished()
                                                    showChapterFinishedDialog = true
                                                }
                                                else -> state.typingWord.index += 1
                                            }
                                            state.saveTypingWordState()
                                        }
                                        Dictation -> {
                                            if (state.dictationIndex + 1 == state.dictationWords.size) {
                                                delayPlaySound()
                                            } else state.dictationIndex++
                                        }
                                        Review -> {
                                            if (state.dictationIndex + 1 == state.reviewWords.size) {
                                                delayPlaySound()
                                            } else state.dictationIndex++
                                        }
                                        NormalReviewWrong -> { increaseWrongIndex() }
                                        DictationReviewWrong -> { increaseWrongIndex() }
                                    }

                                    wordFocusRequester.requestFocus()
                                }
                            }

                            /** 切换到上一个单词,听写时不允许切换到上一个单词 */
                            val previous :() -> Unit = {
                                scope.launch {
                                    // 正常记忆单词
                                    if(state.memoryStrategy == Normal){
                                        clear()
                                        if((state.typingWord.index) % 20 != 0 ){
                                            state.typingWord.index -= 1
                                            state.saveTypingWordState()
                                        }
                                    // 复习错误单词
                                    }else if (state.memoryStrategy == NormalReviewWrong || state.memoryStrategy == DictationReviewWrong ){
                                        clear()
                                        if(state.dictationIndex > 0 ){
                                            state.dictationIndex -= 1
                                        }
                                    }
                                }
                            }

                            /** 检查输入的单词 */
                            val checkWordInput: (String) -> Unit = { input ->
                                wordTextFieldValue = input
                                wordTypingResult.clear()
                                var done = true
                                /**
                                 *  防止用户粘贴内容过长，如果粘贴的内容超过 word.value 的长度，
                                 * 会改变 BasicTextField 宽度，和 Text 的宽度不匹配
                                 */
                                if (wordTextFieldValue.length > currentWord.value.length) {
                                    wordTypingResult.clear()
                                    wordTextFieldValue = ""
                                } else if (input.length <= currentWord.value.length) {
                                    val chars = input.toList()
                                    for (i in chars.indices) {
                                        val inputChar = chars[i]
                                        val wordChar = currentWord.value[i]
                                        if (inputChar == wordChar) {
                                            wordTypingResult.add(Pair(inputChar, true))
                                        } else {
                                            // 字母输入错误
                                            done = false
                                            wordTypingResult.add(Pair(wordChar, false))
                                            speed.wrongCount = speed.wrongCount + 1
                                            playBeepSound()
                                            wordWrongTime++
                                            // 如果是听写测试，或听写复习，需要汇总错误单词
                                            if (state.memoryStrategy == Dictation || state.memoryStrategy == Review) {
                                                chapterWrongTime++
                                                val dictationWrongTime = dictationWrongWords[currentWord]
                                                if (dictationWrongTime != null) {
                                                    dictationWrongWords[currentWord] = dictationWrongTime + 1
                                                } else {
                                                    dictationWrongWords[currentWord] = 1
                                                }
                                            }
                                            Timer("cleanInputChar", false).schedule(50) {
                                                wordTextFieldValue = ""
                                                wordTypingResult.clear()
                                            }
                                        }
                                    }
                                    // 用户输入的单词完全正确
                                    if (wordTypingResult.size == currentWord.value.length && done) {
                                        // 输入完全正确
                                        speed.correctCount = speed.correctCount + 1
                                        playSuccessSound()
                                        if (state.memoryStrategy == Dictation || state.memoryStrategy == Review) chapterCorrectTime++
                                        if (state.typingWord.isAuto) {
                                            Timer("cleanInputChar", false).schedule(50) {
                                                toNext()
                                                wordTextFieldValue = ""
                                                wordTypingResult.clear()
                                            }
                                        } else {
                                            wordCorrectTime++
                                            Timer("cleanInputChar", false).schedule(50) {
                                                wordTypingResult.clear()
                                                wordTextFieldValue = ""
                                            }
                                        }
                                    }
                                }
                            }


                            /** 检查输入的字幕 */
                            val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                                if (input.length <= captionContent.length) {
                                    when(index){
                                        0 -> captionsTextFieldValue1 = input
                                        1 -> captionsTextFieldValue2 = input
                                        2 -> captionsTextFieldValue3 = input
                                    }
                                    val typingResult = captionsTypingResultMap[index]
                                    typingResult!!.clear()
                                    val inputChars = input.toMutableList()
                                    for (i in inputChars.indices) {
                                        val inputChar = inputChars[i]
                                        val char = captionContent[i]
                                        if (inputChar == char) {
                                            typingResult.add(Pair(char, true))
                                        }else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                            typingResult.add(Pair(char, true))
                                            // 音乐符号不好输入，所以可以使用空格替换
                                        }else if (inputChar == ' ' && (char == '♪')) {
                                            typingResult.add(Pair(char, true))
                                            // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                                            inputChars.add(i,'♪')
                                            inputChars.removeAt(i+1)
                                            val textFieldValue = String(inputChars.toCharArray())
                                            when(index){
                                                0 -> captionsTextFieldValue1 = textFieldValue
                                                1 -> captionsTextFieldValue2 = textFieldValue
                                                2 -> captionsTextFieldValue3 = textFieldValue
                                            }
                                        } else {
                                            typingResult.add(Pair(inputChar, false))
                                        }
                                    }
                                }

                            }

                            /** 索引递减 */
                            val decreaseIndex = {
                                if(state.typingWord.index == state.vocabulary.size - 1){
                                    val mod = state.vocabulary.size % 20
                                    state.typingWord.index -= (mod-1)
                                }else if (state.vocabulary.size > 19) state.typingWord.index -= 19
                                else state.typingWord.index = 0
                            }

                            /** 计算正确率 */
                            val correctRate: () -> Float = {
                                if (chapterCorrectTime == 0F) {
                                    0F
                                } else {
                                    val rateDouble = chapterCorrectTime.div(chapterCorrectTime + chapterWrongTime).toDouble()
                                    val rateD = BigDecimal(rateDouble).setScale(3, RoundingMode.HALF_EVEN)

                                    rateD.times(BigDecimal(100)).toFloat()
                                }

                            }

                            /** 重复学习本章 */
                            val learnAgain: () -> Unit = {
                                decreaseIndex()
                                resetChapterTime()
                                state.saveTypingWordState()
                                showChapterFinishedDialog = false
                                isVocabularyFinished = false
                            }


                            /** 复习错误单词 */
                            val reviewWrongWords: () -> Unit = {
                                val reviewList = dictationWrongWords.keys.toList()
                                if (reviewList.isNotEmpty()) {
                                    state.showInfo(clear = false)
                                    if (state.memoryStrategy == Review ||
                                        state.memoryStrategy == DictationReviewWrong
                                    ) {
                                        state.memoryStrategy = DictationReviewWrong
                                    }else{
                                        state.memoryStrategy = NormalReviewWrong
                                    }

                                    state.wrongWords.addAll(reviewList)
                                    state.dictationIndex = 0
                                    resetChapterTime()
                                    showChapterFinishedDialog = false
                                }
                            }

                            /** 下一章 */
                            val nextChapter: () -> Unit = {

                                if (state.memoryStrategy == NormalReviewWrong ||
                                    state.memoryStrategy == DictationReviewWrong
                                ) {
                                    state.wrongWords.clear()
                                }

                                if( state.memoryStrategy == Dictation){
                                    state.showInfo()
                                }

                                state.typingWord.index += 1
                                state.typingWord.chapter++
                                resetChapterTime()
                                state.memoryStrategy = Normal
                                state.saveTypingWordState()
                                showChapterFinishedDialog = false
                            }


                            /** 正常记忆单词，进入到听写测试，需要的单词 */
                            val shuffleNormal:() -> Unit = {
                                val wordValue = state.getCurrentWord().value
                                val shuffledList = state.generateDictationWords(wordValue)
                                state.dictationWords.clear()
                                state.dictationWords.addAll(shuffledList)
                            }
                            /** 从听写复习再次进入到听写测试时，需要的单词 */
                            val shuffleDictationReview:() -> Unit = {
                                var shuffledList = state.reviewWords.shuffled()
                                // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
                                while(shuffledList.first() == currentWord){
                                    shuffledList = state.reviewWords.shuffled()
                                }
                                state.reviewWords.clear()
                                state.reviewWords.addAll(shuffledList)
                            }
                            /** 进入听写模式 */
                            val enterDictation: () -> Unit = {
                                println("Learn Status:${state.memoryStrategy}")
                                scope.launch {
                                    state.saveTypingWordState()
                                    when(state.memoryStrategy){
                                        // 从正常记忆单词第一次进入到听写测试
                                        Normal -> {
                                            shuffleNormal()
                                            state.memoryStrategy = Dictation
                                            state.dictationIndex = 0
                                            state.hiddenInfo()
                                        }
                                        // 正常记忆单词时选择再次听写
                                        Dictation ->{
                                            shuffleNormal()
                                            state.dictationIndex = 0
                                        }
                                        // 从复习错误单词进入到听写测试，这里有两种情况：
                                        // 一种是从正常记忆单词进入到复习错误单词，复习完毕后，再次听写
                                        NormalReviewWrong ->{
                                            state.memoryStrategy = Dictation
                                            state.wrongWords.clear()
                                            shuffleNormal()
                                            state.dictationIndex = 0
                                            state.hiddenInfo()
                                        }
                                        // 一种是从听写复习进入到复习错误单词，复习完毕后，再次听写
                                        DictationReviewWrong ->{
                                            state.memoryStrategy = Review
                                            state.wrongWords.clear()
                                            shuffleDictationReview()
                                            state.dictationIndex = 0
                                            state.hiddenInfo()
                                        }
                                        // 在听写复习时选择再次听写
                                        Review ->{
                                            shuffleDictationReview()
                                            state.dictationIndex = 0
                                        }
                                    }
                                    wordFocusRequester.requestFocus()
                                    resetChapterTime()
                                    showChapterFinishedDialog = false
                                    isVocabularyFinished = false
                                }
                            }


                            /**
                             * 重置索引
                             * @param isShuffle 是否打乱词库
                             */
                            val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->
                                state.typingWord.index = 0
                                state.typingWord.chapter = 1
                                if (isShuffle) {
                                    state.vocabulary.wordList.shuffle()
                                    state.saveCurrentVocabulary()
                                }
                                state.saveTypingWordState()
                                resetChapterTime()
                                showChapterFinishedDialog = false
                                isVocabularyFinished = false
                            }

                            var lastInputTime by remember { mutableStateOf(0L) }
                            val wordKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                when {
                                    ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown)
                                            && it.type == KeyEventType.KeyUp) -> {
                                        toNext()
                                        if (state.memoryStrategy == Dictation || state.memoryStrategy == Review) {
                                            dictationSkipCurrentWord()
                                        }
                                        true
                                    }
                                    (it.key == Key.PageUp && it.type == KeyEventType.KeyUp) -> {
                                        previous()
                                        true
                                    }
                                    (it.isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                        // 消耗快捷键，消耗之后，就不会触发 Ctrl + I 了
                                        true
                                    }
                                    (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                        jumpToCaptions()
                                        true
                                    }

                                    (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                                        jumpToCaptions()
                                        true
                                    }
                                    (it.type == KeyEventType.KeyDown
                                            && !it.isCtrlPressed
                                            && !it.isAltPressed
                                            && it.key != Key.Escape
                                            && it.key != Key.Enter
                                            && it.key != Key.NumPadEnter
                                            ) -> {
                                        playKeySound()
                                        lastInputTime = System.currentTimeMillis()
                                        // 如果计时器没有启动，就启动一个新的计时器
                                        if (!speed.isStart) {
                                            speed.isStart = true
                                            speed.timer = fixedRateTimer("timer", false, 0L, 1 * 1000) {
                                                speed.time = speed.time.plusSeconds(1)
                                            }
                                            // 超过一分钟没有输入自动取消计时器
                                            speed.autoPauseTimer =
                                                fixedRateTimer("autoPause", false, 0L, 60 * 1000) {
                                                    val span = System.currentTimeMillis() - lastInputTime
                                                    if (span > 60 * 1000) {
                                                        speed.isStart = false
                                                        // 取消计时器
                                                        speed.timer.cancel()
                                                        speed.autoPauseTimer.cancel()
                                                    }
                                                }
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }


                            LaunchedEffect(state.vocabularyChanged){
                                if(state.vocabularyChanged){
                                    clear()
                                    if(state.memoryStrategy == NormalReviewWrong ||
                                        state.memoryStrategy == DictationReviewWrong
                                    ){
                                        state.wrongWords.clear()
                                    }
                                    if (state.memoryStrategy == Dictation) {
                                        state.showInfo()
                                        resetChapterTime()
                                    }

                                    if(state.memoryStrategy == Review) state.memoryStrategy = Normal


                                    state.vocabularyChanged = false
                                }
                            }

                            var activeMenu by remember { mutableStateOf(false) }
                            Box(
                                Modifier.onPointerEvent(PointerEventType.Exit) { activeMenu = false }
                            ) {
                                Row(Modifier.align(Alignment.Center)){
                                    Word(
                                        word = currentWord,
                                        global = state.global,
                                        wordVisible = typingWord.wordVisible,
                                        pronunciation = typingWord.pronunciation,
                                        isDictation = (state.memoryStrategy == Dictation ||state.memoryStrategy == Review),
                                        fontFamily = monospace,
                                        audioPath = audioPath,
                                        correctTime = wordCorrectTime,
                                        wrongTime = wordWrongTime,
                                        textFieldValue = wordTextFieldValue,
                                        typingResult = wordTypingResult,
                                        checkTyping = { checkWordInput(it) },
                                        focusRequester = wordFocusRequester,
                                        textFieldKeyEvent = {wordKeyEvent(it)},
                                        showMenu = {activeMenu = true}
                                    )
                                }
                                if (activeMenu) {
                                    Row(modifier = Modifier.align(Alignment.TopCenter)) {
                                        val contains = state.hardVocabulary.wordList.contains(currentWord)
                                        DeleteButton(onClick = { showDeleteDialog = true })
                                        EditButton(onClick = { showEditWordDialog = true })
                                        FamiliarButton(onClick = {showFamiliarDialog = true})
                                        HardButton(
                                            onClick = { bookmarkClick() },
                                            contains = contains,
                                            fontFamily = monospace
                                        )
                                        CopyButton(wordValue = currentWord.value)
                                    }
                                }else if(showBookmark){
                                    val contains = state.hardVocabulary.wordList.contains(currentWord)
                                    // 这个按钮只显示 0.3 秒后消失
                                    BookmarkButton(
                                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 96.dp),
                                        contains = contains,
                                        disappear = {showBookmark = false}
                                    )
                                }
                            }


                            Phonetic(
                                word = currentWord,
                                phoneticVisible = state.typingWord.phoneticVisible
                            )
                            Morphology(
                                word = currentWord,
                                isPlaying = isPlaying,
                                searching = false,
                                morphologyVisible = state.typingWord.morphologyVisible
                            )
                            Definition(
                                word = currentWord,
                                definitionVisible = state.typingWord.definitionVisible,
                                isPlaying = isPlaying,
                            )
                            Translation(
                                word = currentWord,
                                translationVisible = state.typingWord.translationVisible,
                                isPlaying = isPlaying
                            )

                            val videoSize = videoBounds.size
                            val startPadding = if (isPlaying) 0.dp else 50.dp
                            val captionsModifier = Modifier
                                .fillMaxWidth()
                                .height(intrinsicSize = IntrinsicSize.Max)
                                .padding(bottom = 0.dp, start = startPadding)
                                .onKeyEvent {
                                    when {
                                        ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown)
                                                && it.type == KeyEventType.KeyUp
                                                ) -> {
                                            toNext()
                                            if (state.memoryStrategy == Dictation || state.memoryStrategy == Review) {
                                                dictationSkipCurrentWord()
                                            }
                                            true
                                        }
                                        (it.key == Key.PageUp && it.type == KeyEventType.KeyUp) -> {
                                            previous()
                                            true
                                        }
                                        else -> globalKeyEvent(it)
                                    }
                                }
                            Captions(
                                captionsVisible = state.typingWord.subtitlesVisible,
                                playTripleMap = getPlayTripleMap(state, currentWord),
                                videoPlayerWindow = state.videoPlayerWindow,
                                videoPlayerComponent = state.videoPlayerComponent,
                                isPlaying = isPlaying,
                                plyingIndex = plyingIndex,
                                setPlayingIndex = {plyingIndex = it},
                                volume = state.global.videoVolume,
                                setIsPlaying = { isPlaying = it },
                                word = currentWord,
                                bounds = videoBounds,
                                textFieldValueList = listOf(captionsTextFieldValue1,captionsTextFieldValue2,captionsTextFieldValue3),
                                typingResultMap = captionsTypingResultMap,
                                putTypingResultMap = { index, list ->
                                    captionsTypingResultMap[index] = list
                                },
                                checkTyping = { index, input, captionContent ->
                                    checkCaptionsInput(index, input, captionContent)
                                },
                                playKeySound = { playKeySound() },
                                modifier = captionsModifier,
                                focusRequesterList = listOf(focusRequester1,focusRequester2,focusRequester3),
                                jumpToWord = {jumpToWord()},
                                externalVisible = typingWord.externalSubtitlesVisible,
                                openSearch = {openSearch()}
                            )
                            if (isPlaying) Spacer(
                                Modifier.height((videoSize.height).dp).width(videoSize.width.dp)
                            )

                            if (showDeleteDialog) {
                                ConfirmDialog(
                                    message = "确定要删除单词 ${currentWord.value} ?",
                                    confirm = {
                                        scope.launch {
                                            delete()
                                            showDeleteDialog = false
                                        }
                                    },
                                    close = { showDeleteDialog = false }
                                )
                            }
                            if(showFamiliarDialog){
                                ConfirmDialog(
                                    message = "确定要把 ${currentWord.value} 加入到熟悉词库？\n" +
                                            "加入到熟悉词库后，${currentWord.value} 会从当前词库删除。",
                                    confirm = { scope.launch { addToFamiliar() } },
                                    close = { showFamiliarDialog = false }
                                )

                            }
                            if (showEditWordDialog) {
                                EditWordDialog(
                                    word = currentWord,
                                    state = state,
                                    save = { newWord ->
                                        scope.launch {
                                            val current = state.getCurrentWord()
                                            val index = state.typingWord.index
                                            newWord.captions = current.captions
                                            newWord.externalCaptions = current.externalCaptions
                                            state.vocabulary.wordList.removeAt(index)
                                            state.vocabulary.wordList.add(index, newWord)
                                            state.saveCurrentVocabulary()
                                            showEditWordDialog = false
                                        }

                                    },
                                    close = { showEditWordDialog = false }
                                )
                            }

                            /** 显示听写复习的选择章节对话框 */
                            var showChapterDialog by remember { mutableStateOf(false) }
                            /** 打开听写复习的选择章节对话框 */
                            val openReviewDialog:() -> Unit = {
                                showChapterFinishedDialog = false
                                showChapterDialog = true
                                resetChapterTime()
                            }

                            if(showChapterDialog){
                                SelectChapterDialog(
                                    close = {showChapterDialog = false},
                                    state = state,
                                    isMultiple = true
                                )
                            }

                            /** 关闭当前章节结束时跳出的对话框 */
                            val close: () -> Unit = {
                                showChapterFinishedDialog = false
                                if(isVocabularyFinished) isVocabularyFinished = false
                            }
                            if (showChapterFinishedDialog) {
                                ChapterFinishedDialog(
                                    close = { close() },
                                    isVocabularyFinished = isVocabularyFinished,
                                    correctRate = correctRate(),
                                    memoryStrategy = state.memoryStrategy,
                                    openReviewDialog = {openReviewDialog()},
                                    isReviewWrong = (state.memoryStrategy == NormalReviewWrong || state.memoryStrategy == DictationReviewWrong),
                                    dictationWrongWords = dictationWrongWords,
                                    enterDictation = { enterDictation() },
                                    learnAgain = { learnAgain() },
                                    reviewWrongWords = { reviewWrongWords() },
                                    nextChapter = { nextChapter() },
                                    resetIndex = { resetIndex(it) }
                                )
                            }
                        }

                    }
                } else {
                    Surface(Modifier.fillMaxSize()) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("请重新选择词库,可以拖放词库到这里", style = MaterialTheme.typography.h6)
                        }
                    }
                }
                val speedAlignment = Alignment.TopEnd
                Speed(
                    speedVisible = state.typingWord.speedVisible,
                    speed = speed,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .align(speedAlignment)
                        .padding(end = endPadding)
                )
            }
        }
        Settings(
            isOpen = state.openSettings,
            setIsOpen = { state.openSettings = it },
            modifier = Modifier.align(Alignment.TopStart)
        )
    }

}


/**
 * 词型组件
 */
@Composable
fun Morphology(
    word: Word,
    isPlaying: Boolean,
    searching: Boolean,
    morphologyVisible: Boolean,
) {
    if (morphologyVisible && !isPlaying) {
        val exchanges = word.exchange.split("/")
        var preterite = ""
        var pastParticiple = ""
        var presentParticiple = ""
        var third = ""
        var er = ""
        var est = ""
        var plural = ""
        var lemma = ""

        exchanges.forEach { exchange ->
            val pair = exchange.split(":")
            when (pair[0]) {
                "p" -> {
                    preterite = pair[1]
                }
                "d" -> {
                    pastParticiple = pair[1]
                }
                "i" -> {
                    presentParticiple = pair[1]
                }
                "3" -> {
                    third = pair[1]
                }
                "r" -> {
                    er = pair[1]
                }
                "t" -> {
                    est = pair[1]
                }
                "s" -> {
                    plural = pair[1]
                }
                "0" -> {
                    lemma = pair[1]
                }

            }
        }

        Column {
            SelectionContainer {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.height(IntrinsicSize.Max)
                        .width(if(searching) 600.dp else 554.dp)
                        .padding(start = if(searching) 0.dp else 50.dp)

                ) {
                    val textColor = MaterialTheme.colors.onBackground
                    val plainStyle = SpanStyle(
                        color = textColor,
                        fontSize = 16.sp
                    )


                    Text(
                        buildAnnotatedString {
                            if (lemma.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("原型 ")
                                }
                                withStyle(style = plainStyle.copy(color = Color.Magenta)) {
                                    append(lemma)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (preterite.isNotEmpty()) {
                                var color = textColor
                                if (!preterite.endsWith("ed")) {
                                    color = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

                                }
                                withStyle(style = plainStyle) {
                                    append("过去式 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(preterite)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (pastParticiple.isNotEmpty()) {
                                var color = textColor
                                if (!pastParticiple.endsWith("ed")) {
                                    color =
                                        if (MaterialTheme.colors.isLight) MaterialTheme.colors.primary else Color.Yellow
                                }
                                withStyle(style = plainStyle) {
                                    append("过去分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(pastParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (presentParticiple.isNotEmpty()) {
                                val color = if (presentParticiple.endsWith("ing")) textColor else Color(0xFF303F9F)
                                withStyle(style = plainStyle) {
                                    append("现在分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(presentParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (third.isNotEmpty()) {
                                val color = if (third.endsWith("s")) textColor else Color.Cyan
                                withStyle(style = plainStyle) {
                                    append("第三人称单数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(third)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }

                            if (er.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("比较级 $er;")
                                }
                            }
                            if (est.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("最高级 $est;")
                                }
                            }
                            if (plural.isNotEmpty()) {
                                val color = if (plural.endsWith("s")) textColor else Color(0xFFD84315)
                                withStyle(style = plainStyle) {
                                    append("复数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(plural)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                        }
                    )

                }
            }
            if(!searching){
                Divider(Modifier.padding(start = 50.dp))
            }
        }


    }

}

/**
 * 英语定义组件
 */
@Composable
fun Definition(
    word: Word,
    definitionVisible: Boolean,
    isPlaying: Boolean,
) {
    if (definitionVisible && !isPlaying) {
        val rows = word.definition.length - word.definition.replace("\n", "").length
        val normalModifier = Modifier
            .width(554.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(554.dp)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 5) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.body1.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.definition,
                        )
                    }
                }
                if (rows > 5) {
                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * 中文释义组件
 */
@Composable
fun Translation(
    translationVisible: Boolean,
    isPlaying: Boolean,
    word: Word
) {
    if (translationVisible && !isPlaying) {
        Column {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .width(554.dp)
                    .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = word.translation,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.onBackground
                    )
                }

            }
            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/** 字幕列表组件
 * @param captionsVisible 字幕的可见性
 * @param playTripleMap 要显示的字幕。Map 的类型参数说明：
 * - Map 的 Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 * @param videoPlayerWindow 视频播放窗口
 * @param isPlaying 是否正在播放视频
 * @param volume 音量
 * @param setIsPlaying 设置是否正在播放视频播放的回调
 * @param word 单词
 * @param bounds 视频播放窗口的位置
 * @param textFieldValueList 用户输入的字幕列表
 * @param typingResultMap 用户输入字幕的结果 Map
 * @param putTypingResultMap 添加当前的字幕到结果Map
 * @param checkTyping 检查用户输入的回调
 * @param playKeySound 当用户输入字幕时播放敲击键盘音效的回调
 * @param modifier 修改器
 */
@ExperimentalComposeUiApi
@Composable
fun Captions(
    captionsVisible: Boolean,
    playTripleMap: Map<Int, Triple<Caption, String, Int>>,
    videoPlayerWindow: JFrame,
    videoPlayerComponent: Component,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    plyingIndex: Int,
    setPlayingIndex: (Int) -> Unit,
    volume: Float,
    word: Word,
    bounds: Rectangle,
    textFieldValueList: List<String>,
    typingResultMap: Map<Int, MutableList<Pair<Char, Boolean>>>,
    putTypingResultMap: (Int, MutableList<Pair<Char, Boolean>>) -> Unit,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    modifier: Modifier,
    focusRequesterList:List<FocusRequester>,
    jumpToWord: () -> Unit,
    externalVisible:Boolean,
    openSearch: () -> Unit,
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
                val scope = rememberCoroutineScope()
                playTripleMap.forEach { (index, playTriple) ->
                    var captionContent = playTriple.first.content
                    if (captionContent.contains("\r\n")) {
                        captionContent = captionContent.replace("\r\n", " ")
                    } else if (captionContent.contains("\n")) {
                        captionContent = captionContent.replace("\n", " ")
                    }
                    val textFieldValue = textFieldValueList[index]
                    var typingResult = typingResultMap[index]
                    if (typingResult == null) {
                        typingResult = mutableListOf()
                        putTypingResultMap(index, typingResult)
                    }
                    var isPathWrong by remember { mutableStateOf(false) }
                    val playCurrentCaption:()-> Unit = {
                        if (!isPlaying) {
                            val file = File(playTriple.second)
                            if (file.exists()) {
                                setIsPlaying(true)
                                scope.launch {
                                    setPlayingIndex(index)
                                    play(
                                        videoPlayerWindow,
                                        setIsPlaying = { setIsPlaying(it) },
                                        volume,
                                        playTriple,
                                        videoPlayerComponent,
                                        bounds,
                                        externalVisible
                                    )
                                }

                            } else {
                                isPathWrong = true
                                Timer("恢复状态", false).schedule(2000) {
                                    isPathWrong = false
                                }
                            }
                        }
                    }
                    var selectable by remember { mutableStateOf(false) }
                    val focusMoveUp:() -> Unit = {
                        if(index == 0){
                            jumpToWord()
                        }else{
                            focusRequesterList[index-1].requestFocus()
                        }
                    }
                    val focusMoveDown:() -> Unit = {
                        if(index<2 && index + 1 < word.captions.size){
                            focusRequesterList[index+1].requestFocus()
                        }
                    }
                    val captionKeyEvent:(KeyEvent) -> Boolean = {
                        when {
                            (it.type == KeyEventType.KeyDown
                                    && it.key != Key.ShiftRight
                                    && it.key != Key.ShiftLeft
                                    && it.key != Key.CtrlRight
                                    && it.key != Key.CtrlLeft
                                    ) -> {
                                scope.launch { playKeySound() }
                                true
                            }
                            (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                scope.launch { selectable = !selectable }
                                true
                            }
                            (it.key == Key.Tab && it.type == KeyEventType.KeyUp) -> {
                                scope.launch {  playCurrentCaption() }
                                true
                            }
                            (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            (it.key == Key.DirectionUp && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            else -> false
                        }
                    }

                    Caption(
                        isPlaying = isPlaying,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        index = index,
                        playingIndex = plyingIndex,
                        focusRequester = focusRequesterList[index],
                        playCurrentCaption = {playCurrentCaption()},
                        captionKeyEvent = {captionKeyEvent(it)},
                        selectable = selectable,
                        setSelectable = {selectable = it},
                        isPathWrong = isPathWrong,
                        openSearch = {openSearch()}
                    )
                }

            }
        }
        if (!isPlaying && (word.captions.isNotEmpty() || word.externalCaptions.isNotEmpty()))
            Divider(Modifier.padding(start = 50.dp))
    }
}

/**
 * 获取字幕
 * @return Map 的类型参数说明：
 * Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 */
@OptIn(ExperimentalSerializationApi::class)
fun getPlayTripleMap(state: AppState, word: Word): MutableMap<Int, Triple<Caption, String, Int>> {

    val playTripleMap = mutableMapOf<Int, Triple<Caption, String, Int>>()
    if (state.vocabulary.type == VocabularyType.DOCUMENT) {
        if (word.externalCaptions.isNotEmpty()) {
            word.externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val playTriple =
                    Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }
        }
    } else {
        if (word.captions.isNotEmpty()) {
            word.captions.forEachIndexed { index, caption ->
                val playTriple =
                    Triple(caption, state.vocabulary.relateVideoPath, state.vocabulary.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }

        }
    }
    return playTripleMap
}

fun secondsToString(seconds: Double): String {
    val duration = Duration.ofMillis((seconds * 1000).toLong())
    return String.format(
        "%02d:%02d:%02d.%03d",
        duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart()
    )
}

/**
 * 字幕组件
 * @param isPlaying 是否正在播放
 * @param captionContent 字幕的内容
 * @param textFieldValue 输入的字幕
 * @param typingResult 输入字幕的结果
 * @param checkTyping 输入字幕后被调用的回调
 * @param index 当前字幕的索引
 * @param playingIndex 正在播放的字幕索引
 * @param focusRequester 焦点请求器
 * @param playCurrentCaption 播放当前字幕的函数
 * @param captionKeyEvent 处理当前字幕的快捷键函数
 * @param selectable 是否可选择复制
 * @param setSelectable 设置是否可选择
 * @param isPathWrong 是否路径错误
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun Caption(
    isPlaying: Boolean,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    index: Int,
    playingIndex: Int,
    focusRequester:FocusRequester,
    playCurrentCaption:()-> Unit,
    captionKeyEvent:(KeyEvent) -> Boolean,
    selectable:Boolean,
    setSelectable:(Boolean) -> Unit,
    isPathWrong:Boolean,
    openSearch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(36.dp).width(IntrinsicSize.Max)
        ) {
            val dropMenuFocusRequester = remember { FocusRequester() }
            Box(Modifier.width(IntrinsicSize.Max).padding(top = 8.dp, bottom = 8.dp)) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { input ->
                        scope.launch {
                            checkTyping(index, input, captionContent)
                        }
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.CenterStart)
                        .focusRequester(focusRequester)
                        .onKeyEvent { captionKeyEvent(it) }
                )
                Text(
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).height(32.dp),
                    overflow = TextOverflow.Ellipsis,
                    text = buildAnnotatedString {
                        typingResult.forEach { (char, correct) ->
                            if (correct) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.primary,
                                        fontSize = LocalTextStyle.current.fontSize,
                                        letterSpacing = LocalTextStyle.current.letterSpacing,
                                        fontFamily = LocalTextStyle.current.fontFamily,
                                    )
                                ) {
                                    append(char)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.Red,
                                        fontSize = LocalTextStyle.current.fontSize,
                                        letterSpacing = LocalTextStyle.current.letterSpacing,
                                        fontFamily = LocalTextStyle.current.fontFamily,
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
                        val remainChars = captionContent.substring(typingResult.size)
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colors.onBackground,
                                fontSize = LocalTextStyle.current.fontSize,
                                letterSpacing = LocalTextStyle.current.letterSpacing,
                                fontFamily = LocalTextStyle.current.fontFamily,
                            )
                        ) {
                            append(remainChars)
                        }
                    },
                )

                DropdownMenu(
                    expanded = selectable,
                    focusable = true,
                    onDismissRequest = { setSelectable(false) },
                    offset = DpOffset(0.dp, (-30).dp)
                ) {
                    BasicTextField(
                        value = captionContent,
                        onValueChange = {},
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle =  LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                        ),
                        modifier = Modifier.focusable()
                            .focusRequester(dropMenuFocusRequester)
                            .onKeyEvent {
                                if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                    scope.launch { setSelectable(!selectable) }
                                    true
                                }else if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                    scope.launch { openSearch() }
                                    true
                                } else false
                            }
                    )
                    LaunchedEffect(Unit) {
                        dropMenuFocusRequester.requestFocus()
                    }

                }
            }

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shift = if (isMacOS()) "⇧" else "Shift"
                        val text: Any = when (index) {
                            0 -> "播放 $ctrl+$shift+Z"
                            1 -> "播放 $ctrl+$shift+X"
                            2 -> "播放 $ctrl+$shift+C"
                            else -> println("字幕数量超出范围")
                        }
                        Text(text = text.toString(), modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
                    playCurrentCaption()
                }) {
                    val tint = if(isPlaying && playingIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = tint
                    )
                }
            }
            if (isPathWrong) {
                Text("视频地址错误", color = Color.Red)
            }
        }
    }


}

/** 删除按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun DeleteButton(onClick:()->Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(modifier = Modifier.padding(10.dp)){
                    Text(text = "删除单词" )
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " Delete ")

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
        IconButton(onClick = { onClick() }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}
/** 编辑按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun EditButton(onClick: () -> Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "编辑", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = {
//            showEditWordDialog = true
            onClick()
        }) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 困难单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HardButton(
    contains:Boolean,
    onClick: () -> Unit,
    fontFamily:FontFamily,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(modifier = Modifier.padding(10.dp)){
                    Text(text = "加入到困难词库" )
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + ")
                        Text(text = "I",fontFamily =fontFamily)
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

        IconButton(onClick = { onClick() }) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 熟悉单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FamiliarButton(
    onClick: () -> Unit,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(modifier = Modifier.padding(10.dp)){
                    Text(text = "加入到熟悉词库" )
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + Y")
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
        IconButton(onClick = { onClick() }) {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 使用快捷键 Ctrl + I,把当前单词加入到困难单词时显示 0.3 秒后消失 */
@Composable
fun BookmarkButton(
    modifier: Modifier,
    contains:Boolean,
    disappear:() ->Unit
){
        IconButton(onClick = {},modifier = modifier) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary,
            )
            SideEffect{
                Timer("不显示 Bookmark 图标", false).schedule(300) {
                    disappear()
                }
            }
        }

}

/** 复制按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CopyButton(wordValue:String){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "复制", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        val clipboardManager = LocalClipboardManager.current
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(wordValue))
        }) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 退出按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ExitButton(onClick: () -> Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "退出听写复习", modifier = Modifier.padding(10.dp))
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
            onClick()
        }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SearchResultInfo(
    word: Word,
    state: AppState,
){
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()){
        AudioButton(
            word = word,
            state = state,
            volume = state.global.audioVolume,
            pronunciation = state.typingWord.pronunciation,
        )
    }
    Divider()
    Morphology(
        word = word,
        isPlaying = false,
        searching = true,
        morphologyVisible = true
    )
    Spacer(Modifier.height(8.dp))
    Divider()
    SelectionContainer {
        Text(text = word.definition,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
    SelectionContainer {
        Text(word.translation,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
}

/**
 * 计算视频播放窗口的位置和大小
 */
fun computeVideoBounds(
    windowState: WindowState,
    openSettings: Boolean,
    density:Float,
): Rectangle {
    var mainX = windowState.position.x.value.toInt()
    var mainY = windowState.position.y.value.toInt()
    mainX = (mainX).div(density).toInt()
    mainY = (mainY).div(density).toInt()

    val mainWidth = windowState.size.width.value.toInt()
    val mainHeight = windowState.size.height.value.toInt()

    val size = if (mainWidth in 801..1079) {
        Dimension(642, 390)
    } else if (mainWidth > 1080) {
        Dimension(1005, 610)
    } else {
        Dimension(540, 304)
    }
    if(density!=1f){
        size.width = size.width.div(density).toInt()
        size.height = size.height.div(density).toInt()
    }
    var x = (mainWidth - size.width).div(2)
    // 232 是单词 + 字幕的高度 ，再加一个文本输入框48 == 280
    // 48 是内容的 bottom padding
    var y = ((mainHeight - 280 - size.height).div(2)) + 280 + 15-48
    x += mainX
    y += mainY
    if (openSettings) x += 109
    val point = Point(x, y)
    return Rectangle(point, size)
}

/**
 * @param currentWord 当前正在记忆的单词
 * @param index links 的 index
 * @return Triple<Caption, String, Int>? ,视频播放器需要的信息
 */
fun getPayTriple(currentWord: Word, index: Int): Triple<Caption, String, Int>? {

    return if (index < currentWord.externalCaptions.size) {
        val externalCaption = currentWord.externalCaptions[index]
        val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
        Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
    } else {
        null
    }
}