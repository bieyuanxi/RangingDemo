package com.example.rangingdemo.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SquareMatrixViewModel: ViewModel() {
    // client
    public val index = mutableIntStateOf(0)
    public lateinit var leftSquareMatrix: Array<Array<Int>>
    public lateinit var rightSquareMatrix: Array<Array<Int>>

    public val distances = mutableStateOf(Array(0){ 0f })
}