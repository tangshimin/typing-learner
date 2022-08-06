package data

import player.isMacOS
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

/**
 * stardict 350万条数据
 *
 * 从 stardict.csv 文件 导入数据库
 */
const val CREATE_STARDICT = "CREATE TABLE IF NOT EXISTS stardict" +
        "(word VARCHAR(255) NOT NULL UNIQUE , " +
        " phonetic VARCHAR(255), " +
        " definition TEXT, " +
        " translation TEXT, " +
        " pos VARCHAR(64), " +
        " collins INTEGER DEFAULT (0), " +
        " oxford BOOLEAN DEFAULT (0), " +
        " tag VARCHAR(64), " +
        " bnc INTEGER DEFAULT (0), " +
        " frq INTEGER DEFAULT (0), " +
        " exchange TEXT, " +
        " detail TEXT, " +
        " audio TEXT, " +
        " PRIMARY KEY ( word ))" +
        " AS SELECT * FROM CSVREAD('file:C:/Users/tangs/Downloads/stardict/stardict.csv')"

/**
 * ecdict 70万条数据
 *
 * 从 stardict.csv 文件 导入数据库
 */
const val CREATE_ECDICT = "CREATE TABLE IF NOT EXISTS ecdict" +
        "(word VARCHAR(255) NOT NULL UNIQUE , " +
        " phonetic VARCHAR(255), " +
        " definition TEXT, " +
        " translation TEXT, " +
        " pos VARCHAR(64), " +
        " collins INTEGER DEFAULT (0), " +
        " oxford BOOLEAN DEFAULT (0), " +
        " tag VARCHAR(64), " +
        " bnc INTEGER DEFAULT (0), " +
        " frq INTEGER DEFAULT (0), " +
        " exchange TEXT, " +
        " detail TEXT, " +
        " audio TEXT, " +
        " PRIMARY KEY ( word ))" +
        " AS SELECT * FROM CSVREAD('file:C:/Users/tangs/OneDrive/文档/ECDICT/ecdict.csv')"

/**
 * 创建索引
 */
const val WORD_INDEX_STARDICT = "CREATE INDEX IF NOT EXISTS word_index ON stardict(word)"
const val WORD_INDEX_ECDICT = "CREATE INDEX IF NOT EXISTS word_index ON ecdict(word)"

/**
 * 删除索引
 */
const val DROP_INDEX = "DROP INDEX word_index"

/**
 * JDBC driver name
 */
const val JDBC_DRIVER = "org.h2.Driver"

//  Database credentials
const val USER = "sa"
const val PASS = ""

object Dictionary {


    // 70万条数据
    private fun getURL(): String {
        val property = "compose.application.resources.dir"
        val dir = System.getProperty(property)
        return if (dir != null) {
            // 打包之后的环境
            if(isMacOS()){
                "jdbc:h2:file:/Applications/Typing Learner.app/Contents/app/resources/dictionary/ecdict;ACCESS_MODE_DATA=r"
            }else{
                "jdbc:h2:./app/resources/dictionary/ecdict;ACCESS_MODE_DATA=r"
            }
        } else {
            // 开发环境
            "jdbc:h2:./resources/common/dictionary/ecdict;ACCESS_MODE_DATA=r"
        }
    }


    /** 查询一个单词 */
    fun query(word: String): Word? {
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                val sql = "SELECT * from ecdict WHERE word = ?"
                conn.prepareStatement(sql).use { statement ->

                    try {
                        statement.setString(1, word)
                        val result = statement.executeQuery()
                        while (result.next()) {
                            return mapToWord(result)
                        }
                    } catch (se: SQLException) {
                        //Handle errors for JDBC
                        se.printStackTrace()
                    }
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
        return null
    }

    /** 查询一个列表 */
    fun queryList(words: List<String>): MutableList<Word> {
        val results = mutableListOf<Word>()
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                val sql = "SELECT * from ecdict WHERE word = ?"
                conn.prepareStatement(sql).use { statement ->

                    words.forEach { word ->
                        try {
                            statement.setString(1, word)
                            val result = statement.executeQuery()
                            while (result.next()) {
                                val resultWord = mapToWord(result)
                                results.add(resultWord)
                            }
                        } catch (se: SQLException) {
                            //Handle errors for JDBC
                            se.printStackTrace()
                        }
                    }


                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
        return results
    }

    /** 查询所有 BNC 词频小于 num 的单词 */
    fun queryByBncLessThan(num:Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE bnc < $num AND bnc != 0 " +
                  "ORDER BY bnc"
        val results = mutableListOf<Word>()
        try{
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url,USER,PASS).use{conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 查询所有 FRQ 词频小于 num 的单词 */
    fun queryByFrqLessThan(num:Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE frq < $num AND frq != 0 " +
                  "ORDER BY frq"
        val results = mutableListOf<Word>()
        try{
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url,USER,PASS).use{conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 执行更新 */
    fun executeUpdate(sql: String) {
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                conn.createStatement().use { statement ->
                    statement.executeUpdate(sql)
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }

    /** 查询 BNC 词频的最大值 */
    fun queryBncMax():Int{
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                val sql = "SELECT MAX(bnc) as max_bnc from ecdict"
                val statement = conn.createStatement()
                val result = statement.executeQuery(sql)
                if(result.next()){
                    return result.getInt(1)
                }else return 0
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

    /** 查询 COCA 词频的最大值 */
    fun queryFrqMax():Int{
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                val sql = "SELECT MAX(frq) as max_frq from ecdict"
                val statement = conn.createStatement()
                val result = statement.executeQuery(sql)
                if(result.next()){
                    return result.getInt(1)
                }else return 0
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

    /** 内置词典单词总数 */
    fun wordCount():Int{
        try {
            Class.forName(JDBC_DRIVER)
            val url = getURL()
            DriverManager.getConnection(url, USER, PASS).use { conn ->
                val sql = "SELECT COUNT(*) as count from ecdict"
                val statement = conn.createStatement()
                val result = statement.executeQuery(sql)
                if(result.next()){
                    return result.getInt(1)
                }else return 0
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

}

/**
 * 把结果集映射成单词
 */
fun mapToWord(result: ResultSet): Word {
    var value = result.getString("word")
    var phonetic = result.getString("phonetic")
    var definition = result.getString("definition")
    var translation = result.getString("translation")
    var pos = result.getString("pos")
    val collins = result.getInt("collins")
    val oxford = result.getBoolean("oxford")
    var tag = result.getString("tag")
    val bnc = result.getInt("bnc")
    val frq = result.getInt("frq")
    var exchange = result.getString("exchange")

    if (value == null) value = ""
    if (phonetic == null) phonetic = ""
    if (definition == null) definition = ""
    if (translation == null) translation = ""
    if (pos == null) pos = ""
    if (tag == null) tag = ""
    if (exchange == null) exchange = ""

    definition = definition.replace("\\n", "\n")
    translation = translation.replace("\\n", "\n")
    return Word(
        value,
        "",
        phonetic,
        definition,
        translation,
        pos,
        collins,
        oxford,
        tag,
        bnc,
        frq,
        exchange,
        mutableListOf(),
        mutableListOf()
    )
}




