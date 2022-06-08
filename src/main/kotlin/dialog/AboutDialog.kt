package dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isWindows
import state.getResourcesFile
import java.awt.Desktop
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

/**
 * 关于 对话框
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AboutDialog(close: () -> Unit) {
    Dialog(
        title = "关于",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(645.dp, 650.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Divider()
                var state by remember { mutableStateOf(0) }
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                TabRow(
                    selectedTabIndex = state,
                    backgroundColor = Color.Transparent
                ) {
                    Tab(
                        text = { Text("关于") },
                        selected = state == 0,
                        onClick = { state = 0 }
                    )
                    Tab(
                        text = { Text("许可") },
                        selected = state == 1,
                        onClick = { state = 1 }
                    )
                    Tab(
                        text = { Text("致谢") },
                        selected = state == 2,
                        onClick = { state = 2 }
                    )
                    Tab(
                        text = { Text("第三方软件") },
                        selected = state == 3,
                        onClick = { state = 3 }
                    )
                }
                when (state) {
                    0 -> {
                        Column (modifier = Modifier.width(IntrinsicSize.Max).padding(start = 38.dp,top = 20.dp,end = 38.dp)){

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                Image(
                                    painter = painterResource("logo/logo.png"),
                                    contentDescription = "logo",
                                    modifier = Modifier.width(70.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                SelectionContainer {
                                    Text("typing leaner v1.0.0 64-bit")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                    val annotatedString = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                            append("如果你有任何问题或建议可以到 GitHub 提 Issue,如果没有 GitHub 账号，可以发邮件。\n")
                                        }
                                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                            append("GitHub 地址：")
                                        }
                                        pushStringAnnotation(tag = "github", annotation = "https://github.com/tangshimin/typing-learner")
                                        withStyle(style = SpanStyle(color = blueColor)) {
                                            append("https://github.com/tangshimin/typing-learner")
                                        }
                                        pop()
                                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                            append("\n邮箱：")
                                        }
                                        pushStringAnnotation(tag = "email", annotation = "mailto:tang_shimin@qq.com")
                                        withStyle(style = SpanStyle(color = blueColor)) {
                                            append("tang_shimin@qq.com")
                                        }
                                        pop()
                                    }
                                    ClickableText(text = annotatedString,
                                        style = MaterialTheme.typography.body1,
                                        onClick = { offset ->
                                            annotatedString.getStringAnnotations(tag = "github", start = offset, end = offset).firstOrNull()?.let {
                                                uriHandler.openUri(it.item)
                                            }
                                            annotatedString.getStringAnnotations(tag = "email", start = offset, end = offset).firstOrNull()?.let {
                                                uriHandler.openUri(it.item)
                                            }
                                        })
                            }
                        }
                    }
                    1 -> {
                        val file = getResourcesFile("LICENSE")
                        if (file.exists()) {
                            val license = file.readText()
                            Box(Modifier.fillMaxWidth().height(550.dp)) {
                                val stateVertical = rememberScrollState(0)
                                Box(Modifier.verticalScroll(stateVertical)) {
                                    SelectionContainer {
                                        Text(
                                            license,
                                            modifier = Modifier.padding(start = 38.dp, top = 20.dp, end = 38.dp)
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(stateVertical)
                                )
                            }
                        }
                    }
                    2 -> {

                        val annotatedString = buildAnnotatedString {
                            // 本项目的核心功能，记忆单词来源于  [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner)，
                            // 感谢 qwerty-learner 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("本项目的核心功能，记忆单词来源于  ")
                            }
                            pushStringAnnotation(tag = "qwerty", annotation = "https://github.com/Kaiyiwing/qwerty-learner")
                            withStyle(style = SpanStyle(color = blueColor)) {
                                append("qwerty-learner")
                            }
                            pop()
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("\n感谢 qwerty-learner 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。")
                            }

                            // 感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)。
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("\n感谢 ")
                            }
                            pushStringAnnotation(tag = "skywind3000", annotation = "https://github.com/skywind3000")
                            withStyle(style = SpanStyle(color = blueColor)) {
                                append("skywind3000")
                            }
                            pop()
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("开源")
                            }
                            pushStringAnnotation(tag = "ECDICT", annotation = "https://github.com/skywind3000/ECDICT")
                            withStyle(style = SpanStyle(color = blueColor)) {
                                append("ECDICT")
                            }
                            pop()

                            // 感谢 [libregd](https://github.com/libregd) 为本项目设计 Logo。
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("\n感谢 ")
                            }
                            pushStringAnnotation(tag = "libregd", annotation = "https://github.com/libregd")
                            withStyle(style = SpanStyle(color = blueColor)) {
                                append("libregd")
                            }
                            pop()
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append(" 为本项目设计 Logo。")
                            }

                        }

                        ClickableText(text = annotatedString,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(start = 38.dp,top = 20.dp,end = 38.dp,bottom = 20.dp),
                            onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "qwerty", start = offset, end = offset).firstOrNull()?.let {
                                uriHandler.openUri(it.item)
                            }

                            annotatedString.getStringAnnotations(tag = "skywind3000", start = offset, end = offset).firstOrNull()?.let {
                                uriHandler.openUri(it.item)
                            }
                            annotatedString.getStringAnnotations(tag = "ECDICT", start = offset, end = offset).firstOrNull()?.let {
                                uriHandler.openUri(it.item)
                            }
                            annotatedString.getStringAnnotations(tag = "libregd", start = offset, end = offset).firstOrNull()?.let {
                                uriHandler.openUri(it.item)
                            }
                        })
                    }
                    3 -> {
                        val file = getResourcesFile("3rd.html")
                        if (file.exists()) {
                            val thirdParty = file.readText()
                            val editorPane = JEditorPane()
                            editorPane.isEditable = false
                            editorPane.contentType = "text/html"
                            editorPane.text = thirdParty
                            editorPane.addHyperlinkListener {
                                if(it.eventType == HyperlinkEvent.EventType.ACTIVATED){
                                    Desktop.getDesktop().browse(it.url.toURI())
                                }
                            }
                            SwingPanel(
                                modifier = Modifier.width(600.dp).height(460.dp),
                                factory = {
                                    editorPane
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { close() }) {
                        Text("确定")
                    }
                }
            }

        }
    }
}