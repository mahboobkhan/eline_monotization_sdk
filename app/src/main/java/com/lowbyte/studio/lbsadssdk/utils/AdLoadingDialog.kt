package com.lowbyte.studio.lbsadssdk.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.lowbyte.studio.lbsadssdk.databinding.DialogAdLoadingBinding

/**
 * Custom dialog shown while ads are loading.
 * Supports Fullscreen and Small styles.
 */
class AdLoadingDialog(
    context: Context,
    private val style: Style = Style.SMALL
) : Dialog(context) {

    enum class Style {
        FULLSCREEN, SMALL
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogAdLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setCancelable(false)

        window?.let { win ->
            if (style == Style.FULLSCREEN) {
                // Fullscreen style: Covers the whole screen with white background
                win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                win.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            } else {
                // Small style: Centered card with transparent dim background
                win.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                
                // Adjust the CardView to wrap content if it's currently match_parent
                val rootParams = binding.root.layoutParams
                rootParams.width = (context.resources.displayMetrics.widthPixels * 0.7).toInt() // 70% width
                rootParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.root.layoutParams = rootParams
            }
        }
    }
}
