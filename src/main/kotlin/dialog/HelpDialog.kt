package dialog

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isMacOS
import player.isWindows
import java.util.*
import kotlin.concurrent.schedule

@Composable
fun HelpDialog(close: () -> Unit) {
    Dialog(
        title = "帮助文档",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1200.dp, 700.dp)
        ),
    ) {
        Surface {
            Column (Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
                Divider()
                Row{
                    var currentPage by remember{ mutableStateOf("") }
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.width(200.dp).fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "document" }) {
                            Text("从文档生成词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "document"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {  currentPage = "subtitles" }) {
                            Text("从字幕生成词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "subtitles"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {  currentPage = "matroska"}) {
                            Text("从 MKV 视频生成词库", modifier = Modifier.padding(start = 16.dp))
                            if( currentPage == "matroska"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "youtube" }) {
                            Text("YouTube 视频下载", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "youtube"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "linkVocabulary" }) {
                            Text("链接字幕词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkVocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "shortcutKey" }) {
                            Text("快捷键", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "shortcutKey"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())

                    when(currentPage){
                        "document" -> {DocumentPage()}
                        "subtitles" -> {SubtitlesPage()}
                        "matroska" -> {MatroskaPage()}
                        "youtube" -> {YouTubeDownloadPage()}
                        "linkVocabulary" -> {LinkVocabularyPage()}
                        "shortcutKey" -> {ShortcutKeyPage()}
                    }
                }
            }

        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("1. 打开从文档生成词库窗口")
            Image(
                painter = painterResource("screenshot/document-$theme/document-1.png"),
                contentDescription = "document-1",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n2. 选择文档，可以拖放文档到窗口快速打开，我这里选择的是一个 android 开发英文文档，有 1300 页。点击分析按钮。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-2.png"),
                contentDescription = "document-2",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n3. 在预览区可以看到程序生成的单词。你可以点击左边的过滤词频顺序为0的词，词频为 0 的词包括简单的字母和数字还有不常见的单词。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-3.png"),
                contentDescription = "document-3",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n4. 还可以把所有的派生词替换为原型词。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-4.png"),
                contentDescription = "document-4",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n5.经过前面的过滤之后，还是有你很熟悉的词，比如你已经过了很熟悉牛津核心5000词了，点击左边的内置词库，然后选择：牛津核心词 -> The_Oxford_5000，选择之后的单词是不是少了很多。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-5.png"),
                contentDescription = "document-5",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n6. 如果还有你熟悉的词，可以用鼠标单击单词的右上角的删除按钮删除了。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-6.png"),
                contentDescription = "document-6",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n7. 也可以在记忆单词的时候删除熟悉的词，把鼠标移动到正在记忆的单词，会弹出一个菜单，可以从这里删除单词。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-7.png"),
                contentDescription = "document-7",
                modifier = Modifier.width(950.dp).height(316.dp).padding(start = 182.dp,end = 162.dp)
            )
            Text("\n8. 词库不要保存到应用程序的安装目录，升级的时候要先卸载软件，卸载的时候会把安装目录删除。如果你想把内置词库和生成的词库放到一起，可以把内置的词库复制出来。")
            val uriHandler = LocalUriHandler.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Row (verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 30.dp)){
                Text("演示文档 AndroidNotesForProfessionals 来源于：")
                val annotatedString1 = buildAnnotatedString {
                    pushStringAnnotation(tag = "android", annotation = "https://goalkicker.com/AndroidBook/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("https://goalkicker.com/AndroidBook/")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString1,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            Row(verticalAlignment = Alignment.CenterVertically){
                Text("在 ")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "goalkicker", annotation = "https://goalkicker.com/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("goalkicker")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "goalkicker", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 有很多 Stack Overflow Documentation")
            }

        }
        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubtitlesPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("从字幕生成的词库，每个单词最多匹配三条字幕。\n")
            Text("1. 打开从字幕生成词库窗口")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-1.png"),
                contentDescription = "Subtitles-1",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n2. 选择 SRT 字幕，也可以拖放文件到窗口快速打开，如果有对应的视频，就选择对应的视频，然后点击分析按钮。")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-2.png"),
                contentDescription = "Subtitles-2",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n3. 在预览区可以看到程序生成的单词。你可以点击左边的过滤词频顺序为0的词，词频为 0 的词包括简单的字母和数字还有不常见的单词。")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-3.png"),
                contentDescription = "Subtitles-3",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n4. 还可以把所有的派生词替换为原型词。")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-4.png"),
                contentDescription = "Subtitles-4",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n5. 经过前面的过滤之后，还是有你很熟悉的词，比如你已经过了很熟悉牛津核心5000词了，点击左边的内置词库，然后选择：牛津核心词 -> The_Oxford_5000，选择之后的单词是不是少了很多。")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-5.png"),
                contentDescription = "Subtitles-5",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n6. 如果还有你熟悉的词，可以用鼠标单击单词的右上角的删除按钮删除了。")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-6.png"),
                contentDescription = "Subtitles-6",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n7. 也可以在记忆单词的时候删除熟悉的词，把鼠标移动到正在记忆的单词，会弹出一个菜单，可以从这里删除单词。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-7.png"),
                contentDescription = "document-7",
                modifier = Modifier.width(950.dp).height(316.dp).padding(start = 182.dp,end = 162.dp)
            )
            Text("\n8. 词库不要保存到应用程序的安装目录，升级的时候要先卸载软件，卸载的时候会把安装目录删除。如果你想把内置词库和生成的词库放到一起，可以把内置的词库复制出来。")
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("演示字幕来源于")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "blender", annotation = "https://durian.blender.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "blender", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MatroskaPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("从 MKV 生成的词库，每个单词最多匹配三条字幕。\n")
            Text("1. 打开从 MKV 生成词库窗口")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-1.png"),
                contentDescription = "mkv-1",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n2. 选择 MKV 视频，也可以拖放文件到窗口快速打开，然后点击分析按钮")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-2.png"),
                contentDescription = "mkv-2",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n3. 在预览区可以看到程序生成的单词。你可以点击左边的过滤词频顺序为0的词，词频为 0 的词包括简单的字母和数字还有不常见的单词。")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-3.png"),
                contentDescription = "mkv-3",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n4. 还可以把所有的派生词替换为原型词。")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-4.png"),
                contentDescription = "mkv-4",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n5. 经过前面的过滤之后，还是有你很熟悉的词，比如你已经过了很熟悉牛津核心5000词了，点击左边的内置词库，然后选择：牛津核心词 -> The_Oxford_5000，选择之后的单词是不是少了很多。")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-5.png"),
                contentDescription = "mkv-5",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n6. 如果还有你熟悉的词，可以用鼠标单击单词的右上角的删除按钮删除了。")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-6.png"),
                contentDescription = "mkv-6",
                modifier = Modifier.width(950.dp).height(580.dp).padding(start = 20.dp)
            )
            Text("\n7. 也可以在记忆单词的时候删除熟悉的词，把鼠标移动到正在记忆的单词，会弹出一个菜单，可以从这里删除单词。")
            Image(
                painter = painterResource("screenshot/document-$theme/document-7.png"),
                contentDescription = "document-7",
                modifier = Modifier.width(950.dp).height(316.dp).padding(start = 182.dp,end = 162.dp)
            )
            Text("\n8. 词库不要保存到应用程序的安装目录，升级的时候要先卸载软件，卸载的时候会把安装目录删除。如果你想把内置词库和生成的词库放到一起，可以把内置的词库复制出来。")
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("演示视频来源于")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "Sintel", annotation = "https://www.youtube.com/watch?v=eRsGyueVLvQ")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "Sintel", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YouTubeDownloadPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val uriHandler = LocalUriHandler.current
            val clipboard = LocalClipboardManager.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Row(verticalAlignment = Alignment.CenterVertically){

                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "youtube-dl", annotation = "https://github.com/ytdl-org/youtube-dl")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("youtube-dl")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "youtube-dl", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 非常强大的视频下载程序，可以下载 1000+ 视频网站的视频，")
                Text("下载英语字幕和视频的命令：")
            }
            val command = "youtube-dl.exe  --proxy \"URL\" --sub-lang en --convert-subs srt --write-sub URL"
            Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(if(MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35))){
                SelectionContainer {
                    Text("    $command")
                }

                Box{
                    var copyed by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                    clipboard.setText(AnnotatedString(command))
                    copyed = true
                    Timer("恢复状态", false).schedule(2000) {
                        copyed = false
                    }
                }){
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                    DropdownMenu(
                        expanded = copyed,
                        onDismissRequest = {copyed = false}
                    ){
                        Text("已复制")
                    }
                }


            }

            Row{
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "downloader", annotation = "https://jely2002.github.io/youtube-dl-gui/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Open Video Downloader")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "downloader", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 基于 youtube-dl 的视频下载工具, UI 非常简洁。")
            }
            val text = if(isWindows()) "最好选择 Microsoft Store 版本，因为其他版本在某些 Windows 电脑上可能无法使用。\n" else ""
           Text("$text")

            val annotatedString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDYouTube%E8%A7%86%E9%A2%91")
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow：如何下载YouTube视频")
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                })
        }
        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkVocabularyPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("提示：不要把链接后的词库保存到应用程序的安装目录\n")
            Text("1. 打开链接字幕对话框")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-1.png"),
                contentDescription = "mkv-1",
                modifier = Modifier.width(590.dp).height(436.dp).padding(start = 20.dp)
            )
            Text("\n2. 选择一个要链接的词库，如果是四级词库就从选择内置词库打开，也可以直接拖拽一个词库到窗口。")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-2.png"),
                contentDescription = "mkv-2",
                modifier = Modifier.width(590.dp).height(435.dp).padding(start = 20.dp)
            )
            Text("\n3. 再选择一个有字幕的词库。选择后可以预览视频片段，然后点击链接，有字幕的词库就链接到了没有字幕的词库。")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-3.png"),
                contentDescription = "mkv-3",
                modifier = Modifier.width(590.dp).height(650.dp).padding(start = 20.dp)
            )
            Text("\n4. 点击链接后返回到链接字幕的主界面，还可以链接多个有字幕的词库。也可以删除已经链接的字幕。不想链接了就点击保存，最后注意不要把词库保存到应用程序的安装目录")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-4.png"),
                contentDescription = "mkv-4",
                modifier = Modifier.width(590.dp).height(440.dp).padding(start = 20.dp)
            )

        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}
@Composable
fun ShortcutKeyPage() {
    Column(Modifier.fillMaxSize()) {
        SelectionContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(start = 16.dp,top = 16.dp,bottom = 10.dp)
            ) {
                Text("激活复制", modifier = Modifier.padding(end = 20.dp))
                val ctrl = LocalCtrl.current
                val annotatedString = buildAnnotatedString {

                    val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                    val shift = if (isMacOS()) "⇧" else "Shift"
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("如果想复制正在抄写的字幕或文本可以先抄写到要复制的词，然后使用")
                    }

                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  $shift + ← ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("  选择要复制的单词\n或者使用快捷键")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  $ctrl + B ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("  激活复制功能，激活后，不要先抄写就可以自由的复制。")
                    }

                }
                Text(annotatedString)
            }
        }

        SelectionContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
            ) {
                Text("切换单词", modifier = Modifier.padding(end = 20.dp))
                val annotatedString = buildAnnotatedString {

                    val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("切换到下一个单词用")
                    }

                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  Enter 或 PgDn ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("  切换到上一个单词用")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  PgUp ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("  在听写模式下，不能切换到上一个单词。")
                    }

                }
                Text(annotatedString)
            }
        }
        SelectionContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
            ) {
                Text("切换光标", modifier = Modifier.padding(end = 20.dp))
                val annotatedString = buildAnnotatedString {

                    val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("把光标从字幕切换到单词")
                    }

                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  Ctrl + Shift + A ")
                    }

                }
                Text(annotatedString)
            }
        }
        SelectionContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
            ) {
                Text("搜索      ", modifier = Modifier.padding(end = 20.dp))
                val annotatedString = buildAnnotatedString {

                    val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("打开搜索")
                    }

                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary,
                            background = background
                        )
                    ) {
                        append("  Ctrl + F ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                        append("  如果当前词库没有查到，会搜索内置词典。")
                    }
                }
                Text(annotatedString)
            }
        }

    }
}
