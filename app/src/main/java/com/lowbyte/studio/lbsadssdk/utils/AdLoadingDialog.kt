package com.lowbyte.studio.lbsadssdk.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.lowbyte.studio.lbsadssdk.databinding.DialogAdLoadingBinding
import androidx.core.graphics.drawable.toDrawable

class AdLoadingDialog(context: Context) : Dialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogAdLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
    }
}
