package com.lowbyte.studio.lbsadssdk.examples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.lowbyte.studio.lbsadssdk.R
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenAdsManager
import com.lowbyte.studio.lbsadssdk.ads.nextgen.NextGenNativeAdManager

/**
 * Example Fragment demonstrating Small Native ads in a layout.
 */
class AdFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ad_example, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adContainer = view.findViewById<FrameLayout>(R.id.small_native_container)

        // Show Small Native Ad (120x120 Media) with Shimmer
        val nativeManager = NextGenAdsManager.getNativeAdManager("ca-app-pub-3940256099942544/2247696110")
        nativeManager.loadAndShowNativeAd(
            activity = requireActivity(),
            container = adContainer,
            size = NextGenNativeAdManager.NativeSize.SMALL
        )
    }
}
