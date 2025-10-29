package com.example.rangingdemo

import com.chaquo.python.Python

private val py = Python.getInstance()
private val hello_py = py.getModule("hello_py")
private val angle_algorithm = py.getModule("angle_algorithm")


fun helloPython(): String = hello_py.callAttr("hello_python").toString()

fun npVersion(): String = hello_py.callAttr("np_version").toString()

fun pandasVersion(): String = hello_py.callAttr("pandas_version").toString()

fun calculateAngle(angleList: DoubleArray, diffList: DoubleArray): Double {
    assert(angleList.size == diffList.size)
    return angle_algorithm.callAttr("calculate_angle", angleList, diffList).toDouble()
}
