package data

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getResourcesFile
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.charset.StandardCharsets

/**
 * 词库
 */
@Serializable
data class Vocabulary(
    var name: String = "",
    val type: VocabularyType = VocabularyType.DOCUMENT,
    val language: String,
    val size: Int,
    val relateVideoPath: String = "",
    val subtitlesTrackId: Int = 0,
    var wordList: MutableList<Word> = mutableListOf(),
)

/**
 * 可观察的词库
 */
class MutableVocabulary(vocabulary: Vocabulary) {
    var name by mutableStateOf(vocabulary.name)
    var type by mutableStateOf(vocabulary.type)
    var language by mutableStateOf(vocabulary.language)
    var size by mutableStateOf(vocabulary.size)
    var relateVideoPath by mutableStateOf(vocabulary.relateVideoPath)
    var subtitlesTrackId by mutableStateOf(vocabulary.subtitlesTrackId)
    var wordList = getMutableStateList(vocabulary.wordList)

    /**
     * 用于持久化
     */
    val serializeVocabulary
        get() = Vocabulary(name, type, language, size, relateVideoPath, subtitlesTrackId, wordList)

}

/**
 * 获得可观察的单词列表
 */
fun getMutableStateList(wordList: MutableList<Word>): MutableList<Word> {
    val list = mutableStateListOf<Word>()
    list.addAll(wordList)
    return list
}
/**
 links 存储字幕链接,格式：(subtitlePath)[videoPath][subtitleTrackId][index]
 captions 字幕列表
 */
@Serializable
data class Word(
    var value: String,
    var usphone: String = "",
    var ukphone: String = "",
    var definition: String = "",
    var translation: String = "",
    var pos: String = "",
    var collins: Int = 0,
    var oxford: Boolean = false,
    var tag: String = "",
    var bnc: Int? = 0,
    var frq: Int? = 0,
    var exchange: String = "",
    var links: MutableList<String> = mutableListOf(),
    var captions: MutableList<Caption> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        val otherWord = other as Word
        return this.value == otherWord.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@Serializable
data class Caption(var start: String, var end: String, var content: String) {
    override fun toString(): String {
        return content
    }
}
@OptIn(ExperimentalComposeUiApi::class)
fun loadMutableVocabularyFromContext(path: String): MutableVocabulary {
    println("loadVocabulary: $path")
    ResourceLoader.Default.load(path).use { inputStream ->
        InputStreamReader(inputStream,StandardCharsets.UTF_8).use{ reader ->
            val string = reader.readText()
            val vocabulary = Json.decodeFromString<Vocabulary>(string )
        return MutableVocabulary(vocabulary)
        }
    }
}

fun loadMutableVocabularyFromAbsolutePath(absolutePath: String):MutableVocabulary {
    val common = File("app/resources/common")
    // 开发环境
    return if(common.exists()){
        val path =  absolutePath.replaceFirst("app/resources","app/resources/common")
        val vocabulary = Json.decodeFromString<Vocabulary>(File(path).readText())
        MutableVocabulary(vocabulary)
    }else{
        // 打包之后的路径没有 common
        val vocabulary = Json.decodeFromString<Vocabulary>(File(absolutePath).readText())
        MutableVocabulary(vocabulary)
    }
}

fun loadMutableVocabulary(path: String):MutableVocabulary{
    val file = getResourcesFile(path)
    // TODO 处理 FileNotFoundException
    return if (file != null && file.exists()) {
        val vocabulary = Json.decodeFromString<Vocabulary>(file.readText())
        MutableVocabulary(vocabulary)
    }else{
        val vocabulary = Vocabulary(
            name = "",
            type = VocabularyType.DOCUMENT,
            language = "",
            size = 0,
            relateVideoPath = "",
            subtitlesTrackId = 0,
            wordList = mutableListOf()
        )
        MutableVocabulary(vocabulary)
    }

}




@OptIn(ExperimentalComposeUiApi::class)
fun loadVocabulary(path: String): Vocabulary {
    println("loadVocabulary: $path")
    val file = getResourcesFile(path)
    return Json.decodeFromString(file?.readText() ?: "TODO(还没实现)")
}

fun loadCaptionsMap(path: String): HashMap<String, List<Caption>> {
    val map = HashMap<String, List<Caption>>()
//    val file = getFile("vocabulary/字幕/${name}.json") ?: return map
    val file = getResourcesFile(path) ?: return map
    try{
        val vocabulary = Json.decodeFromString<Vocabulary>(file.readText())
        vocabulary.wordList.forEach { word ->
            map[word.value] = word.captions
        }
        return map
    }catch (exception:Exception){
        println(exception.message)
        return map
    }

}

fun getRelativeVideoPath(name: String): String {
    val file = getResourcesFile("vocabulary/字幕/${name}.json") ?: return ""
    return Json.decodeFromString<Vocabulary>(
        file.readText()
    ).relateVideoPath
}

fun getTrackId(name: String): Int {
    val file = getResourcesFile("vocabulary/字幕/${name}.json") ?: return 0
    return Json.decodeFromString<Vocabulary>(
        file.readText()
    ).subtitlesTrackId
}

fun saveVocabularyToTempDirectory(vocabulary: Vocabulary, directory: String) {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    val json = format.encodeToString(vocabulary)
    val file = File("src/main/resources/temp/$directory/${vocabulary.name}.json")
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    File("src/main/resources/temp/$directory/${vocabulary.name}.json").writeText(json)
}

fun saveVocabulary(vocabulary: Vocabulary, path: String) {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    Thread(Runnable {
        val json = format.encodeToString(vocabulary)
        val file = getResourcesFile(path)
        file?.writeText(json)
    }).start()
}

fun main() {
//    convertWord(File("D:\\qwerty-learner-desktop\\resources\\common\\vocabulary"))


}


fun convertWord(dir: File) {

    dir.listFiles().forEach { file ->

        if (file.isDirectory) {
            convertWord(file)
        } else {

            val vocabulary = loadVocabulary(file.absolutePath)
//            var newList = mutableListOf<Word>()

//            for (word in oldVocabulary.wordList) {
//                val word = Word(
//                    value = oldWord.value,
//                    usphone = oldWord.usphone,
//                    ukphone = oldWord.ukphone,
//                    definition = oldWord.definition,
//                    translation = oldWord.translation,
//                    pos = oldWord.pos,
//                    collins = oldWord.collins,
//                    oxford = oldWord.oxford,
//                    tag = oldWord.tag,
//                    bnc = oldWord.bnc,
//                    frq = oldWord.frq,
//                    exchange = oldWord.exchange,
//                    links = oldWord.links,
//                    captions = oldWord.captions
//                )
//                newList.add(word)
//            }


//            val vocabulary = Vocabulary(
//                name = file.nameWithoutExtension,
//                language = oldVocabulary.language,
//                size = oldVocabulary.size,
//                wordList = oldVocabulary.wordList
//            )

            vocabulary.wordList.forEach { word ->
                word.translation = word.translation.replace("\r\n","\n")
//                if(vocabulary.type != VocabularyType.DOCUMENT){
//                    word.captions.forEachIndexed { index, _ ->
//                        word.captions[index].content = word.captions[index].content.replace("\r\n","\n")
//                    }
//                }
            }
            val directory = file.parent.split("\\").last()
            saveVocabularyToTempDirectory(vocabulary, directory)
        }
    }
}

@Serializable
data class OldWord(
    var value: String,
    var usphone: String = "",
    var ukphone: String = "",
    var definition: String = "",
    var translation: String = "",
    var pos: String = "",
    var collins: String = "",
    var oxford: String = "",
    var tag: String = "",
    var bnc: Int? = Int.MAX_VALUE,
    var frq: Int? = Int.MAX_VALUE,
    var exchange: String = "",
    var links: List<String> = listOf(),
    var captions: List<Caption> = listOf()
)

@Serializable
data class OldVocabulary(
    var name: String = "",
    val type: String = "document",
    val language: String,
    val size: Int,
    val relateVideoPath: String = "",
    val subtitlesTrackId: Int = 0,
    var wordList: MutableList<Word> = mutableListOf(),
)

fun loadOldVocabulary(pathname: String): OldVocabulary {
    return Json.decodeFromString(File(pathname).readText())
}
