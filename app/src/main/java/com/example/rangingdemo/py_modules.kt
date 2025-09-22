package com.example.rangingdemo

import com.chaquo.python.Python

private val py = Python.getInstance()
val hello_py = py.getModule("hello_py")
val angle_algorithm = py.getModule("angle_algorithm")