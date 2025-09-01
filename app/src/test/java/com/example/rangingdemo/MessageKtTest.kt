package com.example.rangingdemo

import org.junit.Test

class MessageKtTest {
    @Test
    fun test2() {

//        val data: Message = SetParamMsg(81)
//        val data: Message = StartAudioTrackMsg()
//        val msg: Message = CmdPing()

        println(jsonFormat.encodeToString(
            CmdStartRecord() as Message
        ))

        run {
            val msg: Message = CmdPing()
            val json = jsonFormat.encodeToString(msg)
            println(json)
            val obj = jsonFormat.decodeFromString<Message>(json)
        }

        run {
            val msg: Message = CmdPong()
            println(jsonFormat.encodeToString(msg))
        }

        run {
            val msg: Message = CmdStartPlay()
            println(jsonFormat.encodeToString(msg))
        }


        run {
            val msg: Message = CmdSetParams(
                f_c = 19000,
                arrayOf(Param(960 * 40, 19000, 0, 81), Param(960 * 20, 17000, 0, 41))
            )
            val string = jsonFormat.encodeToString(msg)
            println(string)
            val obj = jsonFormat.decodeFromString<Message>(string)
            print_obj(obj)
        }

        run {
            val msg: Message = CmdResponseArray(18000, intArrayOf(1, 2, 3, 4), intArrayOf(5, 6, 7, 8))

            val string = jsonFormat.encodeToString(msg)
            println(string)
            val obj = jsonFormat.decodeFromString<Message>(string)
            when(obj) {
                is CmdResponseArray -> {
                    println(obj.f_c)
                    println(obj.array_left.contentToString())
                }
            }
        }

    }
}

private fun print_obj(obj: Message) {
    when (obj) {
        is CmdPing -> {
            println(obj)
        }

        is CmdSetParams -> {
            for (param in obj.params) {
                println(param)
            }
            println(obj.f_c)
        }

        is CmdResponseArray -> {
            println(obj.array_left.contentToString())
            println(obj.array_right.contentToString())
        }
    }
}