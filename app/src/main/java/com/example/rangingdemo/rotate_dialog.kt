package com.example.rangingdemo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 旋转手机提示弹窗：包含圆形进度条、提示文本、完成提示
 * @param isShowing 是否显示弹窗
 * @param rotationProgress 当前旋转进度（0f~360f）
 * @param onDismiss 弹窗关闭回调
 */
@Composable
fun RotatePhoneDialog(
    isShowing: Boolean,
    rotationProgress: Float,
    onDismiss: suspend () -> Unit
) {
    AnimatedVisibility(
        visible = isShowing,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var isCompleted by remember { mutableStateOf(false) } // 是否旋转完成
        var progress by remember { mutableFloatStateOf(0f) }

        if (isCompleted) {
            LaunchedEffect(Unit) {
                onDismiss()
            }
        } else {
            if (rotationProgress - progress < 30) { // 防止跳变时更新&&允许最大单步30度跨越
                progress = rotationProgress
            }

            // 设置两个检查点180~190度和0~10度，只有在180~190度先激活后，再激活0~10度的情况下才认为旋转完整一圈
            var check by remember { mutableStateOf(false) }
            if(progress in 180f..190f) {
                check = true
            }

            // 旋转进度达到第二个检查点，标记完成并关闭
            if (check && progress in 0f..10f) {
                progress = 360f
                isCompleted = true
            }
        }

        Dialog(
            onDismissRequest = {}, // 禁止点击外部关闭
            properties = DialogProperties(
                dismissOnBackPress = false, // 禁止返回键关闭
                dismissOnClickOutside = false
            )
        ) {
            // 弹窗内容容器（圆角+白色背景）
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    // 圆形进度条（显示旋转进度，0f~1f）
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 360f }, // 进度：0~1
                            color = Color(0xFF2196F3), // 进度条颜色
                            strokeWidth = 8.dp,
                            modifier = Modifier.size(140.dp)
                        )

                        // 进度文本（百分比）
                        Text(
                            text = "${(progress / 360f * 100).toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }

                    // 提示文本（旋转中/完成）
                    Text(
                        text = if (isCompleted) "旋转完成！" else "请顺时针360度旋转手机",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}