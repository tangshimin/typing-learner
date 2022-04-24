package dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.loadVocabulary
import java.io.File
import java.util.*
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MergeVocabularyDialog(
    futureFileChooser: FutureTask<JFileChooser>,
    close: () -> Unit){
    Dialog(
        title = "合并词库",
        icon = painterResource("logo/logo.svg"),
        onCloseRequest = {close()},
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp,600.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {

            Box{
                var merging by remember { mutableStateOf(false) }

                Column (verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()){
                    var selectedFileList = remember { mutableStateListOf<File>() }
                    var done by remember{ mutableStateOf(false)}
                    if(!merging){
                        selectedFileList.forEach { file ->
                            Row(horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()){
                                Text(text = file.nameWithoutExtension,
                                    modifier = Modifier.width(420.dp))
                                IconButton(onClick = {
                                    selectedFileList.remove(file)
                                }){
                                    Icon( Icons.Filled.Close, contentDescription = "",tint = MaterialTheme.colors.primary)
                                }
                            }
                            Divider(Modifier.width(468.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()){
                        OutlinedButton(onClick = {
                            Thread(Runnable {
                                val fileChooser = futureFileChooser.get()
                                fileChooser.dialogTitle = "选择词库"
                                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                fileChooser.isAcceptAllFileFilterUsed = false
                                fileChooser.isMultiSelectionEnabled = true
                                val fileFilter = FileNameExtensionFilter("词库", "json")
                                fileChooser.addChoosableFileFilter(fileFilter)
                                fileChooser.selectedFile = null
                                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    fileChooser.selectedFiles.forEach { file ->
                                        if (!selectedFileList.contains(file)) {
                                            selectedFileList.add(file)
                                        }
                                    }
                                }
                                fileChooser.selectedFile = null
                                fileChooser.removeChoosableFileFilter(fileFilter)
                            }).start()
                        },modifier = Modifier.padding(end = 10.dp)){
                            Text("添加词库")
                        }

                        OutlinedButton(
                            enabled = selectedFileList.size>1,
                            onClick = {
                                Thread(Runnable {
                                    merging = true

                                    selectedFileList.forEach { file ->
                                        val vocabulary = loadVocabulary(file.absolutePath)

                                    }

                                    merging = false
                                }).start()
                            },modifier = Modifier.padding(end = 10.dp)){
                            Text("合并词库")
                        }
                        OutlinedButton(
                            enabled = done,
                            onClick = {
                                Thread(Runnable {

                                }).start()
                            }){
                            Text("保存词库")
                        }
                    }

                }

                if(merging){
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = 100.dp))
                }
            }

        }
    }
}