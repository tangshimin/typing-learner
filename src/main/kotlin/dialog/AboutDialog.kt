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
import components.LinkText
import player.isWindows
import state.getResourcesFile
import java.awt.Desktop
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

/**
 * 关于 对话框
 */
@Composable
fun AboutDialog(close: () -> Unit) {
    Dialog(
        title = "关于",
        icon = painterResource("logo/logo.png"),
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
                LocalUriHandler.current
                if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
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
                                    Text("Typing Leaner v1.0.0 64-bit")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) {
                                Text("如果你有任何问题或建议可以到 GitHub 提 Issue,如果没有 GitHub 账号，可以发邮件。")
                            }
                            Row{
                                Text("GitHub 地址：")
                                LinkText(
                                    text = "https://github.com/tangshimin/typing-learner",
                                    url =  "https://github.com/tangshimin/typing-learner"
                                )

                            }
                            Row{
                                Text("邮箱：            ")
                                LinkText(
                                    text = "typinglearner@outlook.com",
                                    url = "mailto:typinglearner@outlook.com"
                                )
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

                        Column (Modifier.padding(start = 38.dp,top = 20.dp,end = 38.dp,bottom = 20.dp)){
                            Row{
                                Text("本项目的核心功能，记忆单词来源于  ")
                                LinkText(
                                    text = "qwerty-learner",
                                    url = "https://github.com/Kaiyiwing/qwerty-learner"
                                )
                            }
                            Row{
                                Text("感谢 qwerty-learner 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。")
                            }
                            Row{
                                Text("感谢 ")
                                LinkText(
                                    text = "skywind3000",
                                    url = "https://github.com/skywind3000"
                                )
                                Text("开源")
                                LinkText(
                                    text = "ECDICT",
                                    url = "https://github.com/skywind3000/ECDICT"
                                )
                            }
                            Row{
                                LinkText(
                                    text = "libregd",
                                    url = "https://github.com/libregd"
                                )
                                Text(" 为本项目设计 Logo。")
                            }
                        }



                    }
                    3 -> {
                        Column (Modifier.padding(start = 38.dp,top = 20.dp,end = 38.dp,bottom = 20.dp)){

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text("软件")
                                Text("License")
                            }
                            Divider()
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding( top = 5.dp,bottom = 5.dp),){
                                Row{
                                    LinkText(
                                        text = "VLC Media Player",
                                        url = "https://www.videolan.org/"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("3.17.4")
                                }
                                LinkText(
                                    text = "GPL 2",
                                    url = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "VLCJ",
                                        url = "https://github.com/caprica/vlcj"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("4.7.1")
                                }
                                LinkText(
                                    text = "GPL 3",
                                    url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "FlatLaf",
                                        url = "https://github.com/JFormDesigner/FlatLaf"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("2.3")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "h2database",
                                        url = "https://www.h2database.com/html/main.html"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("2.1.210")
                                }
                                Row{
                                    LinkText(
                                        text = "MPL 2.0",
                                        url = "https://www.mozilla.org/en-US/MPL/2.0/"
                                    )
                                    Text("/")
                                    LinkText(
                                        text = "EPL 1.0",
                                        url = "https://opensource.org/licenses/eclipse-1.0.php"
                                    )
                                }

                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "Apache OpenNLP",
                                        url = "https://opennlp.apache.org/"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.9.4")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "Apache PDFBox",
                                        url = "https://pdfbox.apache.org/"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("2.0.24")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }


                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "Compose Desktop",
                                        url = "https://github.com/JetBrains/compose-jb"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.1.1")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "jetbrains compose material3",
                                        url = "https://mvnrepository.com/artifact/org.jetbrains.compose.material3/material3"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.0.1")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "material-icons-extended",
                                        url = "https://mvnrepository.com/artifact/org.jetbrains.compose.material/material-icons-extended"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.0.1")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "kotlinx",
                                        url = "https://github.com/JetBrains/kotlin"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.6.10")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "kotlinx-coroutines-core",
                                        url = "https://github.com/Kotlin/kotlinx.coroutines"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.6.0")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "kotlinx-serialization-json",
                                        url = "https://github.com/Kotlin/kotlinx.serialization"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.3.2")
                                }
                                LinkText(
                                    text = "Apache-2.0",
                                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "subtitleConvert",
                                        url = "https://github.com/JDaren/subtitleConverter"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.0.2")
                                }
                                LinkText(
                                    text = "MIT",
                                    url = "https://opensource.org/licenses/mit-license.php"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "LyricConverter",
                                        url = "https://github.com/IntelleBitnify/LyricConverter"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("1.0")
                                }
                                LinkText(
                                    text = "MIT",
                                    url = "https://opensource.org/licenses/mit-license.php"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Row{
                                    LinkText(
                                        text = "EBMLReader",
                                        url = "https://github.com/matthewn4444/EBMLReader"
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("0.1.0")
                                }
                            }

                            Divider()
                            Row(horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){

                                Text("本地词典：")
                                LinkText(
                                    text = "ECDICT 本地词典",
                                    url = "https://github.com/skywind3000/ECDICT"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Text("单词发音：单词的语音数据来源于 ")
                                LinkText(
                                    text = "有道词典",
                                    url = "https://www.youdao.com/"
                                )
                                Text(" 在线发音 API")
                            }
                            Row(horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)){
                                Text("本程序使用的音效：")
                                LinkText(
                                    text = "Success!!",
                                    url = "https://freesound.org/people/jobro/sounds/60445/"
                                )
                            }
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