package com.example.rangingdemo

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.ejml.simple.SimpleMatrix
import java.io.File
import java.io.FileReader
import kotlin.math.*

/**
 * 读取CSV文件，提取"angle"和"diff"列数据
 * @param csvPath CSV文件路径
 * @return Pair(angle列表, diff列表)
 */
fun readCsv(file: File): Pair<List<Double>, List<Double>> {
    val angleList = mutableListOf<Double>()
    val diffList = mutableListOf<Double>()

    FileReader(file).use { reader ->
        // 解析CSV（首行为表头，匹配"angle"和"diff"列）
        CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader()).use { parser ->
            for (record in parser) {
                val angle = record.get("angle").toDoubleOrNull() ?: continue
                val diff = record.get("diff").toDoubleOrNull() ?: continue
                angleList.add(angle)
                diffList.add(diff)
            }
        }
    }

    require(angleList.isNotEmpty() && diffList.isNotEmpty()) {
        "CSV文件中未找到有效数据或缺少\"angle\"/\"diff\"列"
    }
    return angleList to diffList
}

/**
 * 角度解缠绕（处理360度跳变，类似numpy.unwrap）
 * @param angles 原始角度（度）
 * @return 解缠绕后的角度（度）
 */
fun unwrapAngles(angles: List<Double>): List<Double> {
    if (angles.size < 2) return angles

    val unwrapped = mutableListOf<Double>()
    unwrapped.add(angles[0])

    for (i in 1 until angles.size) {
        val prev = unwrapped[i - 1]
        val curr = angles[i]
        // 计算角度差，若超过180度则补偿360度（处理跳变）
        val delta = curr - prev
        val compensatedDelta =
            if (delta > 180) delta - 360 else if (delta < -180) delta + 360 else delta
        unwrapped.add(prev + compensatedDelta)
    }
    return unwrapped
}

/**
 * 找到角度累计达到360度的截断索引（类似Python中cut_off_index）
 * @param unwrappedAngles 解缠绕后的角度
 * @return 截断索引（首次累计360度的位置）
 */
fun findCutOffIndex(unwrappedAngles: List<Double>): Int {
    if (unwrappedAngles.isEmpty()) return -1
    val initialAngle = unwrappedAngles[0]

    for (i in unwrappedAngles.indices) {
        if (abs(unwrappedAngles[i] - initialAngle) >= 360.0) {
            return i
        }
    }
    return unwrappedAngles.size // 若未达到360度，取全部数据
}

/**
 * 非均匀数据重采样到均匀时间轴（类似Python resample_to_uniform）
 * @param tRaw 原始角度轴（替代时间轴）
 * @param xRaw 原始信号（diff）
 * @param fsTarget 目标采样率（此处固定为1 Hz，即每度1个点）
 * @param duration 重采样总长度（360或-360，控制方向）
 * @return Pair(均匀角度轴x, 重采样后信号y)
 */
fun resampleToUniform(
    tRaw: List<Double>,
    xRaw: List<Double>,
    fsTarget: Double = 1.0,
    duration: Double
): Pair<List<Double>, List<Double>> {
    require(tRaw.size == xRaw.size) { "原始角度和信号长度不匹配" }

    val t0 = tRaw.first()
    val t1 = t0 + duration
    val dt = (1.0 / fsTarget) * if (duration > 0) 1 else -1

    val tUniform = np_arrange(t0, t1, dt).subList(0, 360)   // 均匀时间轴 FIXME: np_arrange和np.arrange表现不一样

    val xUniform = mutableListOf<Double>()
    val func = linearInterp1d(tRaw, xRaw)
    tUniform.forEach { x ->
        xUniform.add(func(x))
    }

    return tUniform to xUniform
}

//FIXME: np_arrange和np.arrange表现不一样
fun np_arrange(
    start: Double,
    stop: Double,
    step: Double,
    precision: Double = 1e-9
): List<Double> {
    // 校验步长非零
    require(step != 0.0) { "步长step不能为0" }

    val sequence = mutableListOf<Double>()
    var current = start

    // 判断序列是递增还是递减
    val isIncreasing = step > 0

    // 循环条件：根据步长方向判断是否继续添加元素
    // 加入精度补偿，避免因浮点数误差导致提前终止或多添加元素
    while (if (isIncreasing) {
            current < stop + precision  // 递增：当前值小于终止值（含精度补偿）
        } else {
            current > stop - precision  // 递减：当前值大于终止值（含精度补偿）
        }
    ) {
        // 检查当前值是否超出范围（考虑精度），避免因累加误差导致包含stop
        if (isIncreasing) {
            if (current > stop + precision) break
        } else {
            if (current < stop - precision) break
        }

        sequence.add(current)

        // 累加步长（处理浮点数）
        current += step
    }

    return sequence
}


/**
 * 二分查找x在xRaw中的插入位置
 * @return 插入索引：x应插入到xRaw[index]的位置
 */
private fun findInsertionIndex(xRaw: List<Double>, x: Double, isIncreasing: Boolean): Int {
    var low = 0
    var high = xRaw.size
    while (low < high) {
        val mid = (low + high) / 2
        // 根据单调性调整比较逻辑
        if (if (isIncreasing) xRaw[mid] < x else xRaw[mid] > x) {
            low = mid + 1
        } else {
            high = mid
        }
    }
    return low
}

/**
 * 实现 scipy.interpolate.interp1d(kind='linear', fill_value='extrapolate', bounds_error=False) 的功能
 * 线性插值，支持边界外推，不校验边界错误
 *
 * @param tRaw 已知数据的x轴（必须单调递增或递减）
 * @param xRaw 已知数据的y轴（与xRaw长度相同）
 * @return 插值函数：(x: Double) -> Double，输入x返回线性插值/外推的y值
 */
fun linearInterp1d(tRaw: List<Double>, xRaw: List<Double>): (Double) -> Double {
    // 校验输入合法性
    require(tRaw.size == xRaw.size) { "xRaw和yRaw长度必须相同" }
    require(tRaw.size >= 2) { "xRaw和yRaw至少需要2个数据点" }

    // 确定x轴单调性（递增/递减）
    val isIncreasing = tRaw.last() > tRaw.first()

    return { x ->
        // 查找x在tRaw中的插入位置（二分查找）
        when (val index = findInsertionIndex(tRaw, x, isIncreasing)) {
            // 1. x在tRaw范围内（内部线性插值）
            in 1 until tRaw.size -> {
                val x0 = tRaw[index - 1]
                val x1 = tRaw[index]
                val y0 = xRaw[index - 1]
                val y1 = xRaw[index]

                // 处理x0与x1相等的极端情况（避免除零）
                if (abs(x1 - x0) < 1e-12) y0
                else y0 + (x - x0) * (y1 - y0) / (x1 - x0)
            }

            // 2. x <= tRaw[0]（左边界外推）
            0 -> {
                val x0 = tRaw[0]
                val x1 = tRaw[1]
                val y0 = xRaw[0]
                val y1 = xRaw[1]
                if (abs(x1 - x0) < 1e-12) y0
                else y0 + (x - x0) * (y1 - y0) / (x1 - x0)
            }

            // 3. x >= tRaw.last()（右边界外推）
            else -> { // index == tRaw.size
                val x0 = tRaw[tRaw.size - 2]
                val x1 = tRaw.last()
                val y0 = xRaw[tRaw.size - 2]
                val y1 = xRaw.last()

                if (abs(x1 - x0) < 1e-12) y0
                else y1 + (x - x1) * (y1 - y0) / (x1 - x0)
            }
        }
    }
}

///**
// * 实现 scipy.interpolate.interp1d(kind='linear', fill_value='extrapolate', bounds_error=False) 的功能
// * 线性插值，支持边界外推，不校验边界错误
// *
// * @param tRaw 已知数据的x轴（必须单调递增或递减）
// * @param xRaw 已知数据的y轴（与xRaw长度相同）
// * @return 插值函数：(x: Double) -> Double，输入x返回线性插值/外推的y值
// */
//fun linearInterp1d(tRaw: List<Double>, xRaw: List<Double>, tUniform: List<Double>): List<Double> {
//    // 校验输入合法性
//    require(tRaw.size == xRaw.size) { "xRaw和yRaw长度必须相同" }
//    require(tRaw.size >= 2) { "xRaw和yRaw至少需要2个数据点" }
//
//    val xUniform = mutableListOf<Double>()
//    for (x in tUniform) {
//        // 查找x在xRaw中的插入位置（二分查找）
//        val index = tRaw.binarySearch(x).let {
//            if (it >= 0) it else -it - 1
//        }
//
//        val y = when {
//            // 1. x在xRaw范围内（内部线性插值）
//            index in 1 until tRaw.size -> {
//                val x0 = tRaw[index - 1]
//                val x1 = tRaw[index]
//                val y0 = xRaw[index - 1]
//                val y1 = xRaw[index]
//
//                // 处理x0与x1相等的极端情况（避免除零）
//                if (abs(x1 - x0) < 1e-12) y0
//                else y0 + (x - x0) * (y1 - y0) / (x1 - x0)
//            }
//
//            // 2. x <= xRaw[0]（左边界外推）
//            index == 0 -> {
//                val x0 = tRaw[0]
//                val x1 = tRaw[1]
//                val y0 = xRaw[0]
//                val y1 = xRaw[1]
//
//                y0 + (x - x0) * (y1 - y0) / (x1 - x0)
//            }
//
//            // 3. x >= xRaw.last()（右边界外推）
//            else -> { // index == xRaw.size
//                val x0 = tRaw[tRaw.size - 2]
//                val x1 = tRaw.last()
//                val y0 = xRaw[tRaw.size - 2]
//                val y1 = xRaw.last()
//
//                y0 + (x - x0) * (y1 - y0) / (x1 - x0)
//            }
//        }
//        xUniform.add(y)
//    }
//    return xUniform
//}

/**
 * 正弦曲线拟合：y = A*sin(kx + b) + c，通过最小二乘求解A、b、c
 * @param x 均匀角度轴（度）
 * @param y 重采样后的信号
 * @return Triple(A_est, b_est, c_est)：振幅、相位、偏移
 */
fun fitSineCurve(x: List<Double>, y: List<Double>): Triple<Double, Double, Double> {
    require(x.size == y.size && x.isNotEmpty()) { "角度和信号长度不匹配或为空" }
    val k = 2 * PI / 360.0 // 2π/360（角度转弧度系数）

    // 构建设计矩阵X：[sin(kx), cos(kx), 1]（每行对应一个样本）
    val numSamples = x.size
    val X = SimpleMatrix(numSamples, 3)
    val Y = SimpleMatrix(numSamples, 1)

    for (i in x.indices) {
        val kx = k * x[i]
        X[i, 0] = sin(kx)   // 第一列：sin(kx)
        X[i, 1] = cos(kx)   // 第二列：cos(kx)
        X[i, 2] = 1.0       // 第三列：常数项1
        Y[i, 0] = y[i]      // 目标值y
    }

    // 最小二乘求解：X * coef = Y → coef = (X^T X)^-1 X^T Y
    val Xt = X.transpose()
    val XtX = Xt.mult(X)
    val XtY = Xt.mult(Y)
    val coef = XtX.invert().mult(XtY) // coef = [P, Q, c_est]^T

    // 计算振幅A和相位b：A = sqrt(P²+Q²)，b = arctan2(Q, P)
    val P = coef[0, 0]
    val Q = coef[1, 0]
    val cEst = coef[2, 0]
    val AEst = hypot(P, Q)
    val bEst = atan2(Q, P)

    return Triple(AEst, bEst, cEst)
}

/**
 * 计算正弦曲线的最低点（y最小处的x值）
 * @param AEst 振幅
 * @param bEst 相位（弧度）
 * @param k 系数（2π/360）
 * @param range 角度范围（默认[-180, 180]）
 * @return 最低点x值列表（x_min_candidates）
 */
fun calculateMinCandidates(
    AEst: Double,
    bEst: Double,
    k: Double = 2 * PI / 360.0,
    range: Pair<Double, Double> = -180.0 to 180.0
): List<Double> {
    val (minRange, maxRange) = range
    val candidates = mutableListOf<Double>()

    // 正弦曲线y = A*sin(kx + b) + c的最小值在sin(...) = -1时取得
    // 解方程：kx + b = -π/2 + 2πn → x = (-π/2 - b + 2πn)/k
    for (n in -5..5) { // n取-5到5，覆盖足够多周期
        val x0 = (-PI / 2 - bEst + 2 * PI * n) / k
        if (x0 in minRange..maxRange) {
            candidates.add(x0)
        }
    }

    return candidates.sorted() // 排序后返回
}

/**
 * 主函数：整合所有步骤，输入CSV路径，输出最低点x_min_candidates
 * @param csvPath CSV文件路径
 * @return 最低点角度列表（x_min_candidates）
 */
fun getAngleFromFile(file: File): List<Double> {
    try {
        // 1. 读取CSV数据
        val (angleRaw, diffRaw) = readCsv(file)
        println("成功读取CSV：角度数据${angleRaw.size}个，Diff数据${diffRaw.size}个")

        // 2. 角度解缠绕
        val unwrappedAngles = unwrapAngles(angleRaw)
        println("解缠绕后角度范围：${unwrappedAngles.min()} ~ ${unwrappedAngles.max()}")

        // 3. 找到360度截断索引并截取数据
        val cutOffIndex = findCutOffIndex(unwrappedAngles)
        val truncatedAngles = unwrappedAngles.subList(0, cutOffIndex)
        val truncatedDiff = diffRaw.subList(0, cutOffIndex)
        println("截断后数据量：${truncatedAngles.size}个")

        // 4. 确定重采样方向（duration=360或-360）
        val duration = if (truncatedAngles.last() > truncatedAngles.first()) 360.0 else -360.0

        // 5. 重采样到均匀角度轴
        val (xUniform, yUniform) = resampleToUniform(
            truncatedAngles,
            truncatedDiff,
            duration = duration
        )
//        for(i in yUniform.indices) {
//            println(yUniform[i])
//        }
        println("重采样后数据量：${xUniform.size}个")

        // 6. 正弦曲线拟合
        val (AEst, bEst, cEst) = fitSineCurve(xUniform, yUniform)
        println("拟合结果：A=$AEst, b=$bEst, c=$cEst")

        // 7. 计算最低点候选
        val xMinCandidates = calculateMinCandidates(AEst, bEst)
        println("最低点候选（角度）：$xMinCandidates")

        return xMinCandidates
    } catch (e: Exception) {
        println("处理失败：${e.message}")
        e.printStackTrace()
        return emptyList()
    }
}

fun sortPairedLists(a: List<Double>, b: List<Double>): Pair<List<Double>, List<Double>> {
    // 校验两个列表长度是否一致
    require(a.size == b.size) { "列表a和b的长度必须相同" }

    // 将两个列表的元素组合成 Pair(a元素, b元素) 的列表
    val paired = a.zip(b)

    // 根据 Pair 中的第一个元素（即a的值）排序
    val sortedPaired = paired.sortedBy { it.first }

    // 拆分排序后的 Pair 列表，分别得到排序后的a和b
    val sortedA = sortedPaired.map { it.first }
    val sortedB = sortedPaired.map { it.second }

    return sortedA to sortedB
}

///**
// * 主函数：整合所有步骤，输入CSV路径，输出最低点x_min_candidates
// * @param csvPath CSV文件路径
// * @return 最低点角度列表（x_min_candidates）
// */
//fun getAngleFromFile(csvPath: String): List<Double> {
//    try {
//        // 1. 读取CSV数据
//        val (angleRaw, diffRaw) = readCsv(csvPath)
//        Log.d("getAngleFromFile", "成功读取CSV：角度数据${angleRaw.size}个，Diff数据${diffRaw.size}个")
//
//        // 2. 角度解缠绕
//        val unwrappedAngles = unwrapAngles(angleRaw)
//        Log.d("getAngleFromFile", "解缠绕后角度范围：${unwrappedAngles.min()} ~ ${unwrappedAngles.max()}")
//
//        // 3. 找到360度截断索引并截取数据
//        val cutOffIndex = findCutOffIndex(unwrappedAngles)
//        val truncatedAngles = unwrappedAngles.subList(0, cutOffIndex)
//        val truncatedDiff = diffRaw.subList(0, cutOffIndex)
//        Log.d("getAngleFromFile", "截断后数据量：${truncatedAngles.size}个")
//
//        // 4. 确定重采样方向（duration=360或-360）
//        val duration = if (truncatedAngles.last() > truncatedAngles.first()) 360.0 else -360.0
//
//        // 5. 重采样到均匀角度轴
//        val (xUniform, yUniform) = resampleToUniform(truncatedAngles, truncatedDiff, duration = duration)
//        Log.d("getAngleFromFile", "重采样后数据量：${xUniform.size}个")
//
//        // 6. 正弦曲线拟合
//        val (AEst, bEst, cEst) = fitSineCurve(xUniform, yUniform)
//        Log.d("getAngleFromFile", "拟合结果：A=$AEst, b=$bEst, c=$cEst")
//
//        // 7. 计算最低点候选
//        val xMinCandidates = calculateMinCandidates(AEst, bEst)
//        Log.d("getAngleFromFile", "最低点候选（角度）：$xMinCandidates")
//
//        return xMinCandidates
//    } catch (e: Exception) {
//        Log.d("getAngleFromFile", "处理失败：${e.message}")
//        e.printStackTrace()
//        return emptyList()
//    }
//}