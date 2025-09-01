package com.example.rangingdemo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass



interface Message {}

@Serializable
@SerialName("cmd_ping")
class CmdPing(val timestamp: Long = System.currentTimeMillis()) : Message

@Serializable
@SerialName("cmd_pong")
class CmdPong(val timestamp: Long = System.currentTimeMillis()) : Message


@Serializable
@SerialName("cmd_start_play")
class CmdStartPlay() : Message

@Serializable
@SerialName("cmd_stop_play")
class CmdStopPlay() : Message

@Serializable
@SerialName("cmd_start_record")
class CmdStartRecord() : Message

@Serializable
@SerialName("cmd_stop_record")
class CmdStopRecord() : Message

@Serializable
@SerialName("cmd_disconnect")
class CmdDisconnect() : Message

@Serializable
@SerialName("cmd_request_array")
class CmdRequestArray() : Message

@Serializable
@SerialName("cmd_response_array")
class CmdResponseArray(val array: IntArray) : Message


@Serializable
@SerialName("data")
data class Param(val N: Int, val f_c: Int, val u: Int, val q: Int)

@Serializable
@SerialName("cmd_set_params")
class CmdSetParams(val index: Int, val params: Array<Param>) : Message

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
        subclass(CmdResponseArray::class)
        subclass(CmdSetParams::class)
    }
}

val jsonFormat = Json { serializersModule = module }