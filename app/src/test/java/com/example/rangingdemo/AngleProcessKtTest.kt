package com.example.rangingdemo

import org.junit.Test

import org.junit.Assert.*
import java.io.File


class AngleProcessKtTest {


    @Test
    fun testGetAngleFromFile() {
        val path = "D:/ultra_sound/RangingDemoData/BLK-AL80/"
        val fileName = "BLK-AL80_angle_audio_1758195063292.csv"
        val result = getAngleFromFile(File(path + fileName))
        println(result)
        assertEquals("%.1f".format(-13.37276748), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile1() {
        val path = "D:/ultra_sound/RangingDemoData/BLK-AL80/"
        val fileName = "BLK-AL80_angle_audio_1758195030973.csv"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py: [-6.46392303]
        assertEquals("%.1f".format(-6.46392303), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile2() {
        val path = "D:/ultra_sound/RangingDemoData/BLK-AL80/"
        val fileName = "BLK-AL80_angle_audio_1758194978868.csv"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py: [-11.81574328]
        assertEquals("%.1f".format(-11.81574328), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile3() {
        val fileName = "BRA-AL00_angle_audio_1758195225378.csv"
        val path = "D:/ultra_sound/RangingDemoData/BRA-AL00/"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py:  [7.73752567]
        assertEquals("%.1f".format(7.73752567), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile4() {
        val fileName = "BRA-AL00_angle_audio_1758195201059.csv"
        val path = "D:/ultra_sound/RangingDemoData/BRA-AL00/"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py:  [7.3040552]
        assertEquals("%.1f".format(7.3040552), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile5() {
        val fileName = "BRA-AL00_angle_audio_1758195171282.csv"
        val path = "D:/ultra_sound/RangingDemoData/BRA-AL00/"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py:  [4.63696457]
        assertEquals("%.1f".format(4.63696457), "%.1f".format(result[0]))
    }

    @Test
    fun testGetAngleFromFile6() {
        val fileName = "BRA-AL00_angle_audio_1758195135136.csv"
        val path = "D:/ultra_sound/RangingDemoData/BRA-AL00/"
        val result = getAngleFromFile(File(path + fileName))
        println(result) // py:  [14.91160081]
        assertEquals("%.1f".format(14.91160081), "%.1f".format(result[0]))
    }
}