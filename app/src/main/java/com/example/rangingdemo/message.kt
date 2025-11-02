package com.example.rangingdemo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * [Demo from Kotlin](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism)
 */
interface Message {}

@Serializable
@SerialName("cmd_ping")
class CmdPing(val timestamp: Long = System.currentTimeMillis()) : Message

@Serializable
@SerialName("cmd_pong")
class CmdPong(val timestamp: Long = System.currentTimeMillis()) : Message

@Serializable
@SerialName("cmd_stop")
class CmdStop() : Message


@Serializable
@SerialName("cmd_start_play")
@Deprecated("no longer useful")
class CmdStartPlay() : Message

@Serializable
@SerialName("cmd_stop_play")
@Deprecated("use CmdStop()")
class CmdStopPlay() : Message

@Serializable
@SerialName("cmd_start_record")
@Deprecated("no longer useful")
class CmdStartRecord() : Message

@Serializable
@SerialName("cmd_stop_record")
@Deprecated("use CmdStop()")
class CmdStopRecord() : Message

@Serializable
@SerialName("cmd_disconnect")
class CmdDisconnect() : Message

@Serializable
@SerialName("cmd_request_array")
@Deprecated("use CmdRequestArrayV2()")
class CmdRequestArray() : Message

@Serializable
@SerialName("cmd_request_array_v2")
class CmdRequestArrayV2() : Message

@Serializable
@SerialName("cmd_response_array")
@Deprecated("use CmdResponseArrayV2()")
class CmdResponseArray(val f_c: Int, val array_left: IntArray, val array_right: IntArray) : Message

@Serializable
@SerialName("cmd_response_array_v2")
// array_left & array_right: index must same with CmdSetParams.params
class CmdResponseArrayV2(val index: Int, val array_left: IntArray, val array_right: IntArray) : Message

@Serializable
@SerialName("data")
data class Param(val f_c: Int, val u: Int, val q: Int)

@Serializable
@SerialName("cmd_set_params")
@Deprecated("use CmdSetParamsV2()")
// f_c 为该设备所用频率，params 为全体设备使用的信息
class CmdSetParams(val f_c: Int, val N: Int, val params: Array<Param>) : Message

@Serializable
@SerialName("param_v2")
data class ParamV2(val f_c: Int, val u: Int, val q: Int, val N: Int)

@Serializable
@SerialName("cmd_set_params_v2")
// index 为该设备索引&所用参数索引，params 为全体设备使用的信息
class CmdSetParamsV2(val index: Int, val params: Array<ParamV2>) : Message


@Serializable
@SerialName("cmd_square_matrix")
class CmdSquareMatrix(val left_matrix: Array<Array<Int>>, val right_matrix: Array<Array<Int>>) : Message

val module = SerializersModule {
    polymorphic(Message::class) {
        subclass(CmdPing::class)
        subclass(CmdPong::class)
        subclass(CmdStartPlay::class)
        subclass(CmdStopPlay::class)
        subclass(CmdStartRecord::class)
        subclass(CmdStopRecord::class)
        subclass(CmdDisconnect::class)
        subclass(CmdRequestArray::class)
        subclass(CmdRequestArrayV2::class)
        subclass(CmdResponseArray::class)
        subclass(CmdResponseArrayV2::class)
        subclass(CmdSetParams::class)
        subclass(CmdSetParamsV2::class)
        subclass(CmdStop::class)
        subclass(CmdSquareMatrix::class)
    }
}

val jsonFormat = Json { serializersModule = module }