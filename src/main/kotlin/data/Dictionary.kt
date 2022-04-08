package data

import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

/**
 * stardict 350万条数据
 *
 * 从 stardict.csv 文件 导入数据库
 */
//
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

// 创建索引
const val WORD_INDEX_STARDICT = "CREATE INDEX IF NOT EXISTS word_index ON stardict(word)"
const val WORD_INDEX_ECDICT = "CREATE INDEX IF NOT EXISTS word_index ON ecdict(word)"
const val DROP_INDEX = "DROP INDEX word_index"

// JDBC driver name and database URL
const val JDBC_DRIVER = "org.h2.Driver"

//  Database credentials
const val USER = "sa"
const val PASS = ""

object Dictionary {
    // 350万条数据
    private fun getURL(): String {
        val property = "compose.application.resources.dir"
        val dir = System.getProperty(property)
        return if (dir != null) {
            // 打包之后的环境
            "jdbc:h2:./app/resources/dictionary/stardict"
        } else {
            // 开发环境
            "jdbc:h2:./resources/common/dictionary/stardict"
        }
    }

    // 70万条数据
    private fun getEcdictURL(): String {
        val property = "compose.application.resources.dir"
        val dir = System.getProperty(property)
        return if (dir != null) {
            // 打包之后的环境
            "jdbc:h2:./app/resources/dictionary/ecdict"
        } else {
            // 开发环境
            "jdbc:h2:./resources/common/dictionary/ecdict"
        }
    }


    fun query(word: String): Word? {
        try {
            Class.forName(JDBC_DRIVER)
//            val DB_URL = getURL()
            val DB_URL = getEcdictURL()
            DriverManager.getConnection(DB_URL, USER, PASS).use { conn ->
//                val sql = "SELECT * from stardict WHERE word = ?"
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

    fun querySet(words: Set<String>): MutableSet<Word> {
        val results = mutableSetOf<Word>()
        try {
            Class.forName(JDBC_DRIVER)
//            val DB_URL = getURL()
            val DB_URL = getEcdictURL()
            DriverManager.getConnection(DB_URL, USER, PASS).use { conn ->
//                val sql = "SELECT * from stardict WHERE word = ?"
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

    fun executeUpdate(sql: String) {
        try {
            Class.forName(JDBC_DRIVER)
//            val DB_URL = getURL()
            val DB_URL = getEcdictURL()
            DriverManager.getConnection(DB_URL, USER, PASS).use { conn ->
                conn.createStatement().use { statement ->
                    statement.executeUpdate(sql)
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }
}

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


fun main() {
//    val start = System.currentTimeMillis()
//    val weAre = Dictionary.query("we're")
//    println("查询 we're 耗时 ${System.currentTimeMillis() - start}")
//
//    val start2 = System.currentTimeMillis()
//    val apple = Dictionary.query("apple")
//    println("查询 apple 耗时 ${System.currentTimeMillis() - start2}")
//
//    val start3 = System.currentTimeMillis()
//    val zoo = Dictionary.query("zoo")
//    println("查询 zoo 耗时 ${System.currentTimeMillis() - start3}")
//
//    val does = Dictionary.query("does")
//    println(does)
//    val did = Dictionary.query("did")
//    println(did)
//    val done = Dictionary.query("done")
//    println(done)
//
//    val updateDoes = "UPDATE ecdict SET exchange = '0:do' WHERE word = 'does'"


//    Dictionary.executeUpdate()
}




