package com.example.rangingdemo

import kotlinx.serialization.SerialName
import org.junit.Test

import org.junit.Assert.*

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.*
import java.sql.Ref

interface Project {
    val name: String
}

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testJson() {
        @Serializable
        data class Data(val a: Int, val b: String)

        val data = Data(42, "str")
        val json = Json.encodeToString(data)

        val obj = Json.decodeFromString<Data>(json)
        assertEquals(data, obj)
    }

    @Test
    fun test1() {
        println(get_distance(40, 145, 1337, 747, 480 * 40, N = 480 * 40))
    }

}