package components

import LocalCtrl
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import data.Caption
import data.VocabularyType
import data.Word
import data.loadCaptionsMap
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import player.mediaPlayer
import state.AppState
import theme.DarkColorScheme
import theme.LightColorScheme
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.*
import java.io.File
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import javax.swing.JFrame
import kotlin.concurrent.schedule

/**
 * 应用程序的核心组件
 * @param state 应用程序的状态
 * @param videoBounds 视频播放窗口的位置和大小
 * @param modifier Typing 的修改器
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class, ExperimentalSerializationApi::class
)
@Composable
fun Typing(
    state: AppState,
    videoBounds: Rectangle,
    modifier: Modifier
) {
    if (state.vocabulary.wordList.isNotEmpty()) {
        Box(modifier = modifier) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(intrinsicSize = IntrinsicSize.Max)
                    .background(MaterialTheme.colors.background)
                    .focusable(true)
            ) {
                /**
                 * 当前正在学习的单词
                 */
                val word = state.getCurrentWord()

                /**
                 * 单词输入框里的字符串
                 */
                var wordTextFieldValue by remember { mutableStateOf("") }

                /**
                 * 英语定义输入框里的字符串
                 */
                var definitionTextFieldValue by remember { mutableStateOf("") }

                /**
                 * 字幕输入框里的字符串列表
                 */
                var captionsTextFieldValueList = remember { mutableStateListOf("", "", "") }

                /**
                 * 单词输入框输入的结果
                 */
                val wordTypingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }

                /**
                 * 英语定义输入框的结果
                 */
                val definitionTypingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }

                /**
                 * 字幕输入框的结果
                 */
                val captionsTypingResultMap =
                    remember { mutableStateMapOf<Int, MutableList<Pair<Char, Boolean>>>() }

                /**
                 * 显示本章节已经完成对话框
                 */
                var showChapterFinishedDialog by remember { mutableStateOf(false) }

                /**
                 * 显示整个词库已经学习完成对话框
                 */
                var isVocabularyFinished by remember { mutableStateOf(false) }

                /**
                 * 显示编辑单词对话框
                 */
                var showEditWordDialog by remember { mutableStateOf(false) }

                /**
                 * 播放错误音效
                 */
                val playBeepSound = {
                    if (state.typing.isPlaySoundTips) {
                        playSound("audio/beep.wav", state.typing.soundTipsVolume)
                    }
                }

                /**
                 * 播放成功音效
                 */
                val playSuccessSound = {
                    if (state.typing.isPlaySoundTips) {
                        playSound("audio/hint.wav", state.typing.soundTipsVolume)
                    }
                }

                /**
                 * 播放整个章节完成时音效
                 */
                val playChapterFinished = {
                    if (state.typing.isPlaySoundTips) {
                        playSound("audio/Success!!.wav", state.typing.soundTipsVolume)
                    }
                }

                /**
                 * 播放按键音效
                 */
                val playKeySound = {
                    if (state.typing.isPlayKeystrokeSound) {
                        playSound("audio/keystroke.wav", state.typing.keystrokeVolume)
                    }
                }

                /**
                 * 当用户在默写模式按 enter 调用的回调，
                 * 在默写模式跳过单词也算一次错误
                 */
                val dictationSkipCurrentWord: () -> Unit = {
                    if (state.wordCorrectTime == 0) {
                        state.chapterWrongTime++
                        val dictationWrongTime = state.dictationWrongWords[word]
                        if (dictationWrongTime == null) {
                            state.dictationWrongWords[word] = 1
                        }
                    }
                }

                /**
                 * 切换下一个单词
                 */
                val toNext: () -> Unit = {
                    wordTypingResult.clear()
                    wordTextFieldValue = ""
                    definitionTextFieldValue = ""
                    definitionTypingResult.clear()
                    captionsTypingResultMap.clear()
                    captionsTextFieldValueList = mutableStateListOf("", "", "")
                    state.wordCorrectTime = 0
                    state.wordWrongTime = 0
                    if (state.isDictation) {
                        if ((state.dictationIndex + 1) % state.dictationWords.size == 0) {
                            /**
                             * 在默写模式，闭着眼睛听写单词时，刚拼写完单词，就播放这个声音感觉不好，
                             * 在非默写模式下按Enter键就不会有这种感觉，因为按Enter键，
                             * 自己已经输入完成了，有一种期待，预测到了将会播放提示音。
                             */
                            Timer("playChapterFinishedSound", false).schedule(1000) {
                                playChapterFinished()
                            }
                            showChapterFinishedDialog = true

                        } else state.dictationIndex++
                    } else {
                        when {
                            (state.typing.index == state.vocabulary.size - 1) -> {
                                isVocabularyFinished = true
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            ((state.typing.index + 1) % 20 == 0) -> {
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            else -> state.typing.index += 1
                        }
                        state.saveTypingState()
                    }
                }


                /**
                 * 检查输入的单词
                 */
                val checkWordInput: (String) -> Unit = { input ->
                    wordTextFieldValue = input
                    wordTypingResult.clear()
                    var done = true
                    /**
                     *  防止用户粘贴内容过长，如果粘贴的内容超过 word.value 的长度，
                     * 会改变 BasicTextField 宽度，和 Text 的宽度不匹配
                     */
                    if (wordTextFieldValue.length > word.value.length) {
                        wordTypingResult.clear()
                        wordTextFieldValue = ""
                    } else if (input.length <= word.value.length) {
                        val chars = input.toList()
                        for (i in chars.indices) {
                            val inputChar = chars[i]
                            val wordChar = word.value[i]
                            if (inputChar == wordChar) {
                                wordTypingResult.add(Pair(inputChar, true))
                            } else {
                                // 字母输入错误
                                done = false
                                wordTypingResult.add(Pair(wordChar, false))
                                state.speed.wrongCount = state.speed.wrongCount + 1
                                playBeepSound()
                                state.wordWrongTime++
                                if (state.isDictation) {
                                    state.chapterWrongTime++
                                    val dictationWrongTime = state.dictationWrongWords[word]
                                    if (dictationWrongTime != null) {
                                        state.dictationWrongWords[word] = dictationWrongTime + 1
                                    } else {
                                        state.dictationWrongWords[word] = 1
                                    }
                                }
                                Timer("cleanInputChar", false).schedule(50) {
                                    wordTextFieldValue = ""
                                    wordTypingResult.clear()
                                }
                            }
                        }
                        // 用户输入的单词完全正确
                        if (wordTypingResult.size == word.value.length && done) {
                            // 输入完全正确
                            state.speed.correctCount = state.speed.correctCount + 1
                            playSuccessSound()
                            if (state.isDictation) state.chapterCorrectTime++
                            if (state.typing.isAuto) {
                                Timer("cleanInputChar", false).schedule(50) {
                                    toNext()
                                    wordTextFieldValue = ""
                                    wordTypingResult.clear()
                                }
                            } else {
                                state.wordCorrectTime++
                                Timer("cleanInputChar", false).schedule(50) {
                                    wordTypingResult.clear()
                                    wordTextFieldValue = ""
                                }
                            }
                        }
                    }
                }

                /**
                 * 检查输入的英语定义
                 */
                val checkDefinitionInput: (String) -> Unit = { input ->
                    definitionTextFieldValue = input
                    definitionTypingResult.clear()
                    if (input.length <= word.definition.length) {
                        val chars = input.toList()
                        for (i in chars.indices) {
                            val char = chars[i]
                            if (char == word.definition[i]) {
                                definitionTypingResult.add(Pair(char, true))
                            } else {
                                definitionTypingResult.add(Pair(char, false))
                            }
                        }

                        if (input.length == word.definition.length) {
                            Timer("cleanInputChar", false).schedule(50) {
                                definitionTextFieldValue = ""
                                definitionTypingResult.clear()
                            }
                        }

                    }
                }

                /**
                 * 检查输入的字幕
                 */
                val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                    captionsTextFieldValueList[index] = input
                    val typingResult = captionsTypingResultMap[index]
                    typingResult!!.clear()
                    if (input.length <= captionContent.length) {
                        val chars = input.toList()
                        for (i in chars.indices) {
                            val char = chars[i]
                            if (char == captionContent[i]) {
                                typingResult.add(Pair(char, true))
                            } else {
                                typingResult.add(Pair(char, false))
                            }
                        }
                        if (input.length == captionContent.length) {
                            Timer("cleanInputChar", false).schedule(50) {
                                captionsTextFieldValueList[index] = ""
                                typingResult.clear()
                            }

                        }

                    }

                }
                WordComponents(
                    state = state,
                    word = word,
                    correctTime = state.wordCorrectTime,
                    wrongTime = state.wordWrongTime,
                    toNext = { toNext() },
                    dictationSkip = { dictationSkipCurrentWord() },
                    textFieldValue = wordTextFieldValue,
                    typingResult = wordTypingResult,
                    checkTyping = { checkWordInput(it) },
                    showChapterFinishedDialog = showChapterFinishedDialog,
                    changeShowChapterFinishedDialog = { showChapterFinishedDialog = it },
                    showEditWordDialog = showEditWordDialog,
                    setShowEditWordDialog = { showEditWordDialog = it },
                    isVocabularyFinished = isVocabularyFinished,
                    setIsVocabularyFinished = { isVocabularyFinished = it },
                    chapterCorrectTime = state.chapterCorrectTime,
                    chapterWrongTime = state.chapterWrongTime,
                    dictationWrongWords = state.dictationWrongWords,
                    resetChapterTime = { state.resetChapterTime() },
                    playKeySound = { playKeySound() },
                )
                Phonetic(
                    word = word,
                    phoneticVisible = state.typing.phoneticVisible
                )
                Morphology(
                    word = word,
                    isPlaying = state.isPlaying,
                    morphologyVisible = state.typing.morphologyVisible
                )
                Definition(
                    word = word,
                    definitionVisible = state.typing.definitionVisible,
                    isPlaying = state.isPlaying,
                    textFieldValue = definitionTextFieldValue,
                    typingResult = definitionTypingResult,
                    checkTyping = { checkDefinitionInput(it) },
                    playKeySound = { playKeySound() },
                )
                Translation(
                    word = word,
                    translationVisible = state.typing.translationVisible,
                    isPlaying = state.isPlaying
                )

                val videoSize = videoBounds.size
                val startPadding = if (state.isPlaying) 0.dp else 50.dp
                val captionsModifier = Modifier
                    .fillMaxWidth()
                    .height(intrinsicSize = IntrinsicSize.Max)
                    .padding(bottom = 0.dp, start = startPadding)
                    .onPreviewKeyEvent {
                        if ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                            && it.type == KeyEventType.KeyUp
                        ) {
                            toNext()
                            true
                        } else false
                    }
                Captions(
                    captionsVisible = state.typing.subtitlesVisible,
                    playTripleMap = getPlayTripleMap(state, word),
                    videoPlayerWindow = state.videoPlayerWindow,
                    videoPlayerComponent = state.videoPlayerComponent,
                    isPlaying = state.isPlaying,
                    volume = state.typing.videoVolume,
                    setIsPlaying = { state.isPlaying = it },
                    word = word,
                    bounds = videoBounds,
                    textFieldValueList = captionsTextFieldValueList,
                    typingResultMap = captionsTypingResultMap,
                    putTypingResultMap = { index, list ->
                        captionsTypingResultMap[index] = list
                    },
                    checkTyping = { index, input, captionContent ->
                        checkCaptionsInput(index, input, captionContent)
                    },
                    playKeySound = { playKeySound() },
                    modifier = captionsModifier,
                )
                if (state.isPlaying) Spacer(Modifier.height((videoSize.height).dp).width(videoSize.width.dp))
            }

        }
    } else {
        Surface(Modifier.fillMaxSize()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("请选择词库,快捷键：", style = MaterialTheme.typography.h6)
                Text(
                    "Ctrl + O",
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }

}

/**
 * 词型组件
 */
@Composable
fun Morphology(
    word: Word,
    isPlaying: Boolean,
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
                "1" -> {
                }
            }
        }

        Column {
            SelectionContainer {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.height(IntrinsicSize.Max)
                        .width(554.dp)
                        .padding(start = 50.dp)

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
                                    append("$lemma")
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
                                    append("$preterite")
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
                                    append("$pastParticiple")
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
                                    append("$presentParticiple")
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
                                    append("$third")
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
                                    append("$plural")
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                        }
                    )

                }
            }
            Divider(Modifier.padding(start = 50.dp))
        }


    }

}

/**
 * 英语定义组件
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Definition(
    word: Word,
    definitionVisible: Boolean,
    isPlaying: Boolean,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    playKeySound: () -> Unit,
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
        val scope = rememberCoroutineScope()
        Column {
            Box(modifier = if (rows > 5) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            scope.launch {
                                checkTyping(input)
                            }
                        },
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = MaterialTheme.typography.body1.copy(color = Color.Transparent, lineHeight = 26.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .align(Alignment.TopStart).onKeyEvent {
                                if (it.type == KeyEventType.KeyDown
                                    && it.key != Key.ShiftRight
                                    && it.key != Key.ShiftLeft
                                    && it.key != Key.CtrlRight
                                    && it.key != Key.CtrlLeft
                                ) {
                                    playKeySound()
                                }
                                true
                            }
                    )
                    Text(
                        textAlign = TextAlign.Start,
//                    maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body1.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart),
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
                            val remainChars = word.definition.substring(typingResult.size)
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

                }
                if (rows > 5) {
                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = RectangleShape),
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
    volume: Float,
    setIsPlaying: (Boolean) -> Unit,
    word: Word,
    bounds: Rectangle,
    textFieldValueList: List<String>,
    typingResultMap: Map<Int, MutableList<Pair<Char, Boolean>>>,
    putTypingResultMap: (Int, MutableList<Pair<Char, Boolean>>) -> Unit,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    modifier: Modifier,
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
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
                    Caption(
                        videoPlayerWindow = videoPlayerWindow,
                        videoPlayerComponent = videoPlayerComponent,
                        setIsPlaying = {
                            setIsPlaying(it)
                        },
                        volume = volume,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult!!,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        playKeySound = { playKeySound() },
                        index = index,
                        playTriple = playTriple,
                        bounds = bounds
                    )
                }

            }
        }
        if (!isPlaying && (word.captions.isNotEmpty() || word.links.isNotEmpty()))
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
        if (word.links.isNotEmpty()) {
            word.links.forEachIndexed { index, _ ->
                val playTriple = getPayTriple(state, index)
                if (playTriple != null) {
                    playTripleMap[index] = playTriple
                }
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
 * @param videoPlayerWindow 视频播放窗口
 * @param setIsPlaying 设置是否正在播放视频的回调
 * @param volume 音量
 * @param captionContent 字幕的内容
 * @param textFieldValue 输入的字幕
 * @param typingResult 输入字幕的结果
 * @param checkTyping 输入字幕后被调用的回调
 * @param playKeySound 当用户输入字幕时播放敲击键盘音效的回调
 * @param index 当前字幕的索引
 * @param playTriple 用于播放当前字幕的相关信息：
 * - Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - String   -> 字幕对应的视频地址
 * - Int      -> 字幕的轨道
 * @param bounds 视频播放器的位置和大小
 */
@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun Caption(
    videoPlayerWindow: JFrame,
    videoPlayerComponent: Component,
    setIsPlaying: (Boolean) -> Unit,
    volume: Float,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    index: Int,
    playTriple: Triple<Caption, String, Int>,
    bounds: Rectangle
) {
    val scope = rememberCoroutineScope()
    val relativeVideoPath = playTriple.second
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(36.dp).width(IntrinsicSize.Max)
        ) {

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
                        .align(Alignment.CenterStart).onKeyEvent {
                            if (it.type == KeyEventType.KeyDown
                                && it.key != Key.ShiftRight
                                && it.key != Key.ShiftLeft
                                && it.key != Key.CtrlRight
                                && it.key != Key.CtrlLeft
                            ) {
                                playKeySound()
                            }
                            true
                        }
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
                        var remainChars = captionContent.substring(typingResult.size)
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
            }
            var isPathWrong by remember { mutableStateOf(false) }
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val text: Any = when (index) {
                            0 -> "播放 $ctrl+Z"
                            1 -> "播放 $ctrl+X"
                            2 -> "播放 $ctrl+C"
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
                    val file = File(relativeVideoPath)
                    if (file.exists()) {
                        setIsPlaying(true)
                        scope.launch {
                            play(
                                videoPlayerWindow,
                                setIsPlaying = { setIsPlaying(it) },
                                volume,
                                playTriple,
                                videoPlayerComponent,
                                bounds
                            )
                        }

                    } else {
                        isPathWrong = true
                        Timer("恢复状态", false).schedule(2000) {
                            isPathWrong = false
                        }
                    }

                }) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            if(isPathWrong){
                Text("视频地址错误",color = Color.Red)
            }

        }
    }


}


/**
 * @param window 视频播放窗口
 * @param setIsPlaying 设置是否正在播放视频
 * @param volume 音量
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 * @param videoPlayerComponent 视频播放组件
 * @param bounds 视频播放窗口的位置和大小
 * 使用 JFrame 的一个原因是 swingPanel 重组的时候会产生闪光,等Jetbrains 把 bug 修复了再重构
 */
fun play(
    window: JFrame,
    setIsPlaying: (Boolean) -> Unit,
    volume: Float,
    playTriple: Triple<Caption, String, Int>,
    videoPlayerComponent: Component,
    bounds: Rectangle
) {
    val caption = playTriple.first
    val relativeVideoPath = playTriple.second
    val trackId = playTriple.third
    window.size = bounds.size
    window.location = bounds.location
    var start = LocalTime.parse(caption.start, DateTimeFormatter.ofPattern("HH:mm:ss.SSS")).toNanoOfDay().toDouble()
    start = start.div(1000_000_000)
    var end = LocalTime.parse(caption.end, DateTimeFormatter.ofPattern("HH:mm:ss.SSS")).toNanoOfDay().toDouble()
    end = end.div(1000_000_000)
    videoPlayerComponent.bounds = Rectangle(0, 0, bounds.size.width, bounds.size.height)

    val closeButton = ComposePanel()
    closeButton.bounds = Rectangle(bounds.size.width - 48, 0, 48, 48)
    closeButton.setContent {
        MaterialTheme(colors = if (MaterialTheme.colors.isLight) LightColorScheme else DarkColorScheme) {
            // TODO 等 ComposePanel 支持背景透明之后重构
            Box(Modifier
                .clickable { window.isVisible = false }
                .fillMaxSize()
                .background(Color.Black)
            ) {
                IconButton(onClick = { window.isVisible = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }

    videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            window.isVisible = false
            EventQueue.invokeLater {
                window.remove(videoPlayerComponent)
            }
            videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    window.layout = null
    window.contentPane.add(videoPlayerComponent)
//        window.contentPane.add(closeButton)
    window.isVisible = true
    videoPlayerComponent.mediaPlayer().media()
        .play(relativeVideoPath, ":sub-track=$trackId", ":start-time=$start", ":stop-time=$end")

}


fun computeVideoBounds(mainWindow: ComposeWindow): Rectangle {
    val windowWidth = mainWindow.size.width
    val size = if (windowWidth in 801..1079) {
        Dimension(642, 390)
    } else if (windowWidth > 1080) {
        Dimension(1005, 610)
    } else {
        Dimension(540, 304)
    }
    var x = (mainWindow.size.width - size.width).div(2)
    // 232 是单词 + 字幕的高度
    var y = ((mainWindow.size.height - 232 - size.height).div(2)) + 232 + 15
    println("size:$size")
    println("x:$x,y:$y")

    x += mainWindow.location.x
    y += mainWindow.location.y
    val point = Point(x, y)
    return Rectangle(point, size)
}

/**
 * 计算视频播放窗口的位置和大小
 */
fun computeVideoBounds(windowState: WindowState, openSettings: Boolean): Rectangle {
    val mainX = windowState.position.x.value.toInt()
    val mainY = windowState.position.y.value.toInt()
    val mainWidth = windowState.size.width.value.toInt()
    val mainHeight = windowState.size.height.value.toInt()
    val size = if (mainWidth in 801..1079) {
        Dimension(642, 390)
    } else if (mainWidth > 1080) {
        Dimension(1005, 610)
    } else {
        Dimension(540, 304)
    }
    var x = (mainWidth - size.width).div(2)
    // 232 是单词 + 字幕的高度 ，再加一个文本输入框48 == 280
    var y = ((mainHeight - 280 - size.height).div(2)) + 280 + 15
    x += mainX
    y += mainY
    if (openSettings) x += 109
    val point = Point(x, y)
    return Rectangle(point, size)
}

/**
 * @param state 应用程序的状态
 * @param index links 的 index
 * @return Triple<Caption, String, Int>? ,视频播放器需要的信息
 */
@OptIn(ExperimentalSerializationApi::class)
fun getPayTriple(state: AppState, index: Int): Triple<Caption, String, Int>? {
    /**
     * 字幕链接的模式
     * (vocabularyPath)[videoPath][subtitleTrackId][index]
     */
    val captionPattern: Pattern = Pattern.compile("\\((.*?)\\)\\[(.*?)\\]\\[([0-9]*?)\\]\\[([0-9]*?)\\]")
    val word = state.getCurrentWord().value
    val item = state.getCurrentWord().links[index]
    val matcher = captionPattern.matcher(item)
    if (matcher.find()) {
        val vocabularyPath = matcher.group(1)
        val relativeVideoPath = matcher.group(2)
        val subtitleTrackId = matcher.group(3).toInt()
        val subtitleIndex = matcher.group(4).toInt()

        // vocabularyPath to captionsMap (word.value to word.captions)
        if (!state.captionsMap.containsKey(vocabularyPath)) {
            state.captionsMap[vocabularyPath] = loadCaptionsMap(vocabularyPath)
        }
        val caption = state.captionsMap[vocabularyPath]?.get(word)?.get(subtitleIndex) ?: return null

        return Triple(caption, relativeVideoPath, subtitleTrackId)
    } else {
        return null
    }
}