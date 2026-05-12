# GMA Next-Gen SDK Integration Guide

This guide provides instructions and examples for implementing the new **GMA Next-Gen SDK** using the `LBSAdsSDK` wrappers.

## 1. Initialization

Initialize the SDK in your `SplashActivity` or `Application` class. It must be done before loading any ads.

```kotlin
NextGenAdsManager.initialize(
    context = this,
    appId = "YOUR_APP_ID",
    billing = billingManager
) {
    // SDK Initialized. Start preloading Interstitials.
    NextGenAdsManager.startPreloadingInterstitial("YOUR_INTERSTITIAL_ID")
}
```

## 2. Banner Ads

Banners support adaptive sizes and collapsible parameters.

```kotlin
val bannerManager = NextGenAdsManager.getBannerManager("YOUR_BANNER_ID")
bannerManager.loadAndShowBanner(
    activity = this,
    container = binding.bannerContainer,
    isCollapsible = true, // Optional: Enable collapsible
    collapsibleType = "bottom" // "top" or "bottom"
)
```

## 3. Fullscreen Ads (Interstitial)

We support three specialized managers for Interstitials:

### A. Simple Interstitial
Standard load and show logic.
```kotlin
val manager = NextGenAdsManager.getInterstitialManager("YOUR_ID")
manager.showAd(activity) { /* onDismiss callback */ }
```

### B. Counter Interstitial (Recommended)
Shows the ad only after a specific number of attempts (threshold).
```kotlin
val counter = NextGenAdsManager.getInterstitialCounterManager("YOUR_ID")
// Shows ad every 3rd time
counter.showAd(activity, threshold = 3) { /* callback */ }
```

### C. Interval Interstitial
Ensures a time gap (e.g., 60 seconds) between ads.
```kotlin
val interval = NextGenAdsManager.getInterstitialIntervalManager("YOUR_ID")
interval.showAd(activity, intervalSeconds = 60) { /* callback */ }
```

## 4. Native Ads

Native ads come with built-in shimmers and 3 predefined sizes.

### Sizes:
- `SMALL`: Fixed 120x120 MediaView. Ideal for lists.
- `MEDIUM`: Balanced layout for news/feed items.
- `LARGE`: Full-width MediaView with comprehensive app details.

```kotlin
val nativeManager = NextGenAdsManager.getNativeAdManager("YOUR_NATIVE_ID")
nativeManager.loadAndShowNativeAd(
    activity = this,
    container = binding.nativeContainer,
    size = NextGenNativeAdManager.NativeSize.MEDIUM
)
```

## 5. App Open Ads

Handles app entry/exit transitions automatically.

```kotlin
val appOpenManager = NextGenAdsManager.getAppOpenAdManager(application, "YOUR_ID")
appOpenManager.loadAd() // Preload
appOpenManager.showAdIfAvailable(activity) // Show on splash or foreground
```

## 6. Remote Config Integration

You can control ad behavior remotely by passing keys to the managers:
- `remoteConfigKey`: Controls if the ad is enabled globally.
- `thresholdKey`: Changes the counter threshold remotely.
- `intervalKey`: Changes the time gap remotely.

Example:
```kotlin
NextGenAdsManager.getInterstitialCounterManager(
    adUnitId = "ID",
    thresholdKey = "remote_click_threshold"
)
```

## 7. Advanced Preloading & Prefetching

For the best user experience, you should preload ads as early as possible (e.g., in `SplashActivity` or during the screen transition before the ad is needed).

### Interstitial & Rewarded (Global)
Use the built-in `InterstitialAdPreloader` via `NextGenAdsManager`.
```kotlin
// In Splash
NextGenAdsManager.startPreloadingInterstitial("YOUR_ID")

// To show later (will poll from cache)
val counter = NextGenAdsManager.getInterstitialCounterManager("YOUR_ID")
counter.showAd(activity) { /* ... */ }
```

### Banner Prefetching
You can load a banner in the background and attach it when the view is ready.
```kotlin
// 1. Prefetch early
val bannerManager = NextGenAdsManager.getBannerManager("YOUR_ID")
bannerManager.preloadAdaptiveBanner(activity, width = 320)

// 2. Attach later in Fragment/Activity
if (bannerManager.isAdLoaded()) {
    bannerManager.showPreloadedBanner(activity, binding.container)
}
```

### Native Prefetching
Similar to banners, Native ads can be cached to avoid shimmers on slow networks.
```kotlin
// 1. Prefetch early
val nativeManager = NextGenAdsManager.getNativeAdManager("YOUR_ID")
nativeManager.preloadAd()

// 2. Show later
if (nativeManager.isAdLoaded()) {
    nativeManager.showPreloadedNativeAd(activity, binding.container, NativeSize.MEDIUM)
}
```

---
Refer to the `com.lowbyte.studio.lbsadssdk.examples` package for full working Activity and Fragment implementations.
