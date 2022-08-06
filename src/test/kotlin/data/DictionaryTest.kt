package data

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DictionaryTest {

    @Test
    fun query() {
        val apple = Dictionary.query("apple")
        assertNotNull(apple)
        if (apple != null) {
            assertEquals("apple",apple.value)
        }


        val cancel = Dictionary.query("cancel")
        assertNotNull(cancel)
        if(cancel != null){
            assertEquals("cancel",cancel.value)
        }

    }

    @Test
    fun queryList() {
        val list = listOf("apple","cancel","abandon","design","book","dictionary","explosive")
        val result = Dictionary.queryList(list)
        assertEquals(7,result.size)
        assertEquals("apple",result[0].value,"第 1 个单词应该是 apple")
        assertEquals("cancel",result[1].value,"第 2 个单词应该是 cancel")
        assertEquals("abandon",result[2].value,"第 3 个单词应该是 abandon")
        assertEquals("design",result[3].value,"第 4 个单词应该是 design")
        assertEquals("book",result[4].value,"第 5 个单词应该是 book")
        assertEquals("dictionary",result[5].value,"第 6 个单词应该是 dictionary")
        assertEquals("explosive",result[6].value,"第 7 个单词应该是 explosive")
    }

    @Test
    fun queryByBncLessThan() {
        val result = Dictionary.queryByBncLessThan(1000)
        assertNotNull(result)
        assertEquals(true, result.isNotEmpty(),"列表不为空")
        assertEquals(988,result.size,"单词的总数不对")
        assertEquals(true,result.size<1001,"单词的数量超过了1000")
        assertEquals("the",result[0].value,"第一个单词不是 the ")
        assertEquals("be",result[1].value,"第二个单词不是 be ")
        assertEquals("of",result[2].value,"第三个单词不是 of ")

    }

    @Test
    fun queryByFrqLessThan() {
        val result = Dictionary.queryByFrqLessThan(1000)
        assertNotNull(result)
        assertEquals(true, result.isNotEmpty(),"列表不为空")
        assertEquals(902,result.size,"单词的总数不对")
        assertEquals(true,result.size<1001,"单词的数量超过了1000")
        assertEquals("the",result[0].value,"第一个单词不是 the ")
        assertEquals("be",result[1].value,"第二个单词不是 be ")
        assertEquals("and",result[2].value,"第三个单词不是 and ")
    }

    @Test
    fun queryBncMax() {
        val result = Dictionary.queryBncMax()
        assertNotEquals(0,result,"BNC 词频的最大值不应该为 0")
        assertEquals(50000,result,"BNC 词频的最大值应该为 50000")
    }

    @Test
    fun queryFrqMax() {
        val result = Dictionary.queryFrqMax()
        assertNotEquals(0,result,"COCA 词频的最大值不应该为 0")
        assertEquals(47062,result,"BNC 词频的最大值应该为 47062")
    }

    @Test
    fun wordCount() {
        val result = Dictionary.wordCount()
        assertNotEquals(0,result,"词典的单词总数不应该为 0")
        assertEquals(770612,result,"词典的单词总数应该为 770612")
    }

}