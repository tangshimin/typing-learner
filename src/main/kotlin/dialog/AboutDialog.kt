package dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import state.getResourcesFile
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * 关于 对话框
 */
@Composable
fun AboutDialog(close:()->Unit) {
    Dialog(
        title = "关于",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = {close()},
        undecorated = !MaterialTheme.colors.isLight,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(620.dp,650.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {
            Column (horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()){
                var state by remember{ mutableStateOf(0) }
                TabRow(selectedTabIndex = state,
                backgroundColor = Color.Transparent
                ){
                    Tab(
                        text = {Text("关于")},
                        selected = state == 0,
                        onClick = {state = 0}
                    )
                    Tab(
                        text = {Text("许可")},
                        selected = state == 1,
                        onClick = {state = 1}
                    )
                    Tab(
                        text = {Text("第三方软件")},
                        selected = state == 2,
                        onClick = {state = 2}
                    )
                }
                when(state){
                    0 -> {
                        Column {
                            Row (horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top =12.dp)){
                                SelectionContainer {
                                    Text("qwerty leaner desktop 1.0.0 64-bit")
                                }
                            }

                            Row (horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top =12.dp)){
                                SelectionContainer {
                                    Text("源代码地址：https://github.com/tangshimin/qwerty-learner-desktop")
                                }
                            }
                        }
                    }
                     1 -> {
                         val file = getResourcesFile("LICENSE")
                         if (file != null) {
                             val license = file.readText()
                             Box(Modifier.fillMaxWidth().height(550.dp)){
                                 val stateVertical = rememberScrollState(0)
                                 Box(Modifier.verticalScroll(stateVertical)){
                                     SelectionContainer {
                                         Text(license,modifier =Modifier.padding(start = 38.dp,top = 20.dp,end = 38.dp))
                                     }
                                 }
                                 VerticalScrollbar(
                                     style = LocalScrollbarStyle.current.copy(shape = RectangleShape),
                                     modifier = Modifier.align(Alignment.CenterEnd)
                                         .fillMaxHeight(),
                                     adapter = rememberScrollbarAdapter(stateVertical)
                                 )
                             }
                         }
                     }
                     2 -> {
                         val file = getResourcesFile("3rd.txt")
                         if (file != null) {
                             val thirdParty = file.readText()
                             SelectionContainer {
                                 Text(thirdParty,modifier =Modifier.padding(10.dp))
                             }
                         }
                     }
                }

                Row(horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()){
                    OutlinedButton(onClick = {close()}){
                        Text("确定")
                    }
                }
            }

        }
    }
}