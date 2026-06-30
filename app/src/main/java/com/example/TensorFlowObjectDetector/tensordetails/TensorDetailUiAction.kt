package com.example.TensorFlowObjectDetector.tensordetails

import com.example.TensorFlowObjectDetector.tensorchat.ChatContext

sealed class TensorDetailUiAction {
    data class NavigateToAdvancedAnalysis(val chatContext: ChatContext) : TensorDetailUiAction()
}
