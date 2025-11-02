package com.example.rangingdemo.viewmodel

import androidx.lifecycle.ViewModel

class SquareMatrixViewModel: ViewModel() {
    // client
    public var index: Int = 0
    public lateinit var leftSquareMatrix: Array<Array<Int>>
    public lateinit var rightSquareMatrix: Array<Array<Int>>
}